package com.example.usbcamerarecorder;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.TextureView;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.Utils;
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
    private static final Size GAUSS_KSIZE = new Size(5, 5);

    private final TextureView mPreview;
    private final TextureView mOverlay;
    private final TextView mTvOcrResult;

    private final TextRecognizer mTextRecognizer;
    private final ExecutorService mExecutor;

    private boolean running = false;
    private long lastOcrTime = 0;

    private Mat mRgba, mGray, mEdges;

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
        }
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
                        processFrame(bmp);
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

    private void processFrame(Bitmap bmp) {
        // OCR part
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOcrTime >= 500) {
            lastOcrTime = currentTime;

            Mat ocrGrayMat = new Mat();
            Utils.bitmapToMat(bmp.copy(bmp.getConfig(), true), ocrGrayMat);
            Imgproc.cvtColor(ocrGrayMat, ocrGrayMat, Imgproc.COLOR_RGBA2GRAY);

            // 1. Нормализация освещения
            Imgproc.equalizeHist(ocrGrayMat, ocrGrayMat);

            // 2. Улучшение контраста с помощью CLAHE
            CLAHE clahe = Imgproc.createCLAHE();
            clahe.setClipLimit(2.0);
            clahe.apply(ocrGrayMat, ocrGrayMat);

            // 3. Адаптивная бинаризация для лучшей четкости символов
            Imgproc.adaptiveThreshold(ocrGrayMat, ocrGrayMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

            // 4. Удаление шумов
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
            Imgproc.morphologyEx(ocrGrayMat, ocrGrayMat, Imgproc.MORPH_OPEN, kernel);

            Bitmap ocrBitmap = Bitmap.createBitmap(ocrGrayMat.cols(), ocrGrayMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(ocrGrayMat, ocrBitmap);

            ocrGrayMat.release();
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


        // Shape detection part
        initMats();
        Utils.bitmapToMat(bmp, mRgba);
        bmp.recycle();

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        Core.bitwise_not(mGray, mGray);
        Imgproc.GaussianBlur(mGray, mGray, GAUSS_KSIZE, 2, 2);

        double[] foundCircle = detectCircleHough(mGray);
        RotatedRect foundEllipse = null;
        if (foundCircle == null) {
            foundEllipse = findBestEllipse(mGray);
        }

        drawShapes(foundCircle, foundEllipse);
    }

    private void updateTextView(String text) {
        mTvOcrResult.post(() -> mTvOcrResult.setText(text));
    }

    private void drawShapes(double[] circle, RotatedRect ellipse) {
        Canvas canvas = mOverlay.lockCanvas();
        if (canvas == null) return;
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            if (circle != null) {
                paint.setColor(Color.RED);
                Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                int radius = (int) Math.round(circle[2]);
                canvas.drawCircle((float) center.x, (float) center.y, radius, paint);
            }

            if (ellipse != null) {
                paint.setColor(Color.BLUE);
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

    private void initMats() {
        if (mRgba == null) mRgba = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC4);
        if (mGray == null) mGray = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC1);
        if (mEdges == null) mEdges = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC1);
    }

    // --- Поиск круга через HoughCircles ---
    private double[] detectCircleHough(Mat grayFrame) {
        Mat circles = new Mat();
        Imgproc.HoughCircles(
                grayFrame,
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.0,
                grayFrame.rows() / 8,
                100,
                30,
                80,
                400
        );

        double[] foundCircle = null;
        if (circles.cols() > 0) {
            foundCircle = circles.get(0, 0);
        }
        circles.release();
        return foundCircle;
    }

    // --- Поиск эллипса через fitEllipse ---
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
            if (area > 10000 && area > maxArea) {
                maxArea = area;
                bestEllipse = ellipse;
            }

            contour2f.release();
        }

        hierarchy.release();
        return bestEllipse;
    }
}