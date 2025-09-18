package com.example.usbcamerarecorder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.TextureView;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FrameProcessor {

    private static final String TAG = "FrameProcessor";
    private static final int CAM_WIDTH = 640;
    private static final int CAM_HEIGHT = 480;
    private static final long FRAME_DELAY_MS = 50;

    private final TextureView mPreview;
    private final TextureView mOverlay;
    private final TextView mTvOcrResult;

    private final TextRecognizer mTextRecognizer;
    private final ExecutorService mExecutor;

    private boolean running = false;
    private long lastOcrTime = 0;

    // Mats for OpenCV processing
    private Mat mRgba;
    private Mat mGray;

    public FrameProcessor(TextureView preview, TextureView overlay, TextView tvOcrResult) {
        this.mPreview = preview;
        this.mOverlay = overlay;
        this.mTvOcrResult = tvOcrResult;
        this.mTextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.mExecutor = Executors.newSingleThreadExecutor();
        mOverlay.setOpaque(false);
    }

    public void start() {
        if (!running) {
            running = true;
            mExecutor.execute(processingRunnable);
        }
    }

    public void stop() {
        if (running) {
            running = false;
            mExecutor.shutdown();
            releaseMats();
        }
    }

    private void releaseMats() {
        if (mRgba != null) mRgba.release();
        if (mGray != null) mGray.release();
    }

    private final Runnable processingRunnable = new Runnable() {
        @Override
        public void run() {
            while (running) {
                try {
                    if (!mPreview.isAvailable()) {
                        Thread.sleep(FRAME_DELAY_MS);
                        continue;
                    }
                    Bitmap bmp = mPreview.getBitmap(CAM_WIDTH, CAM_HEIGHT);
                    if (bmp != null) {
                        // Create a copy for OCR, as both methods will recycle their bitmaps
                        Bitmap bmpCopyForOCR = bmp.copy(bmp.getConfig(), true);

                        // Process for shapes with the original bitmap
                        processFrameForShapes(bmp);

                        // Process for OCR with the copy
                        processFrameForOCR(bmpCopyForOCR);
                    }
                    Thread.sleep(FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error in processing loop", e);
                }
            }
        }
    };

    private void processFrameForOCR(Bitmap bmp) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOcrTime < 500) {
            bmp.recycle();
            return;
        }
        lastOcrTime = currentTime;

        Mat rgbaMat = new Mat();
        Utils.bitmapToMat(bmp, rgbaMat);
        bmp.recycle();

        Mat grayMat = new Mat();
        Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // 1. Нормализация освещения
        Imgproc.equalizeHist(grayMat, grayMat);

        // 2. Улучшение контраста с помощью CLAHE
        CLAHE clahe = Imgproc.createCLAHE();
        clahe.setClipLimit(2.0);
        clahe.apply(grayMat, grayMat);

        // 3. Адаптивная бинаризация для лучшей четкости символов
        Imgproc.adaptiveThreshold(grayMat, grayMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        // 4. Удаление шумов
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_OPEN, kernel);

        Bitmap ocrBitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayMat, ocrBitmap);

        rgbaMat.release();
        grayMat.release();
        kernel.release();

        InputImage image = InputImage.fromBitmap(ocrBitmap, 0);
        mTextRecognizer.process(image)
                .addOnSuccessListener(result -> {
                    String recognizedText = result.getText().trim();
                    if (!recognizedText.isEmpty()) {
                        updateTextView(recognizedText);
                    } else {
                        updateTextView("NONE");
                    }
                    ocrBitmap.recycle();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    updateTextView("NONE");
                    ocrBitmap.recycle();
                });
    }

    private void updateTextView(String text) {
        mTvOcrResult.post(() -> mTvOcrResult.setText(text));
    }

    // --- Shape Detection and Drawing ---

    private static final Size GAUSS_KSIZE = new Size(5, 5);

    private void initMats() {
        if (mRgba == null) mRgba = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC4);
        if (mGray == null) mGray = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC1);
    }

    private void processFrameForShapes(Bitmap bmp) {
        initMats();

        // Convert bitmap to Mat and recycle bitmap
        Utils.bitmapToMat(bmp, mRgba);
        bmp.recycle();

        // Preprocessing
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(mGray, mGray, GAUSS_KSIZE, 0);
        Core.bitwise_not(mGray, mGray); // Invert for white circle on black background

        // Detection
        double[] foundCircle = detectCircleHough(mGray);
        RotatedRect foundEllipse = null;
        if (foundCircle == null) {
            foundEllipse = findBestEllipse(mGray);
        }

        // Drawing
        drawOverlay(foundCircle, foundEllipse);
    }

    private double[] detectCircleHough(Mat grayFrame) {
        Mat circles = new Mat();
        Imgproc.HoughCircles(
                grayFrame,
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.2,
                grayFrame.rows() / 4,
                80,
                40,
                50,
                250
        );

        if (circles.cols() > 0) {
            double[] c = circles.get(0, 0);
            circles.release();
            return c;
        }
        circles.release();
        return null;
    }

    private RotatedRect findBestEllipse(Mat inputMat) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(inputMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        RotatedRect bestEllipse = null;
        double maxArea = 0;

        for (MatOfPoint contour : contours) {
            if (contour.rows() < 5) continue;

            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            RotatedRect ellipse;
            try {
                ellipse = Imgproc.fitEllipse(contour2f);
            } catch (Exception e) {
                contour2f.release();
                continue;
            }

            double area = Math.PI * (ellipse.size.width / 2.0) * (ellipse.size.height / 2.0);
            if (area > 5000 && area > maxArea) {
                maxArea = area;
                bestEllipse = ellipse;
            }
            contour2f.release();
        }
        hierarchy.release();
        return bestEllipse;
    }

    private void drawOverlay(double[] circle, RotatedRect ellipse) {
        Canvas canvas = mOverlay.lockCanvas();
        if (canvas == null) return;

        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setAntiAlias(true);

            if (circle != null) {
                Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                int radius = (int) Math.round(circle[2]);
                canvas.drawCircle((float) center.x, (float) center.y, radius, paint);
            }

            if (ellipse != null) {
                Point center = ellipse.center;
                Size size = ellipse.size;
                float angle = (float) ellipse.angle;
                float x = (float) center.x;
                float y = (float) center.y;
                float width = (float) size.width;
                float height = (float) size.height;

                canvas.save();
                canvas.rotate(angle, x, y);
                canvas.drawOval(x - width / 2, y - height / 2, x + width / 2, y + height / 2, paint);
                canvas.restore();
            }
        } finally {
            mOverlay.unlockCanvasAndPost(canvas);
        }
    }
}