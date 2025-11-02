package com.example.usbcamerarecorder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.Surface;
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

    // For recording
    private Surface mRecordingSurface;
    private volatile boolean isRecording = false;

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

    public void startRecording(Surface surface) {
        mRecordingSurface = surface;
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
        // The surface is owned by MediaCodec, so we don't release it here.
        // Just remove the reference.
        mRecordingSurface = null;
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
                        // If recording is active, draw the current frame to the recording surface.
                        // This must be done *before* the bitmap is passed to processing methods that might recycle it.
                        if (isRecording && mRecordingSurface != null) {
                            try {
                                Canvas canvas = mRecordingSurface.lockCanvas(null);
                                if (canvas != null) {
                                    canvas.drawBitmap(bmp, 0, 0, null);
                                    mRecordingSurface.unlockCanvasAndPost(canvas);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error drawing to recording surface", e);
                            }
                        }

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

        // Preprocessing - FIXED: Removed bitwise_not to detect dark circles on light background
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(mGray, mGray, GAUSS_KSIZE, 0);
        
        // Optional: Enhance contrast for better detection
        Imgproc.equalizeHist(mGray, mGray);

        // Detection with improved parameters
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
        Mat edges = new Mat();
        
        // Apply Canny edge detection for better circle detection
        Imgproc.Canny(grayFrame, edges, 50, 150);
        
        // Apply morphological operations to close gaps in edges
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.dilate(edges, edges, kernel);
        
        // FIXED: Improved Hough parameters for better detection
        Imgproc.HoughCircles(
                edges,
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.0,                    // dp: inverse ratio of accumulator resolution
                grayFrame.rows() / 8,   // minDist: minimum distance between circle centers
                100,                    // param1: higher threshold for Canny
                30,                     // param2: accumulator threshold (lower = more circles detected)
                30,                     // minRadius: minimum circle radius
                300                     // maxRadius: maximum circle radius
        );

        kernel.release();
        
        if (circles.cols() > 0) {
            // Find the largest circle
            double[] bestCircle = null;
            double maxRadius = 0;
            
            for (int i = 0; i < circles.cols(); i++) {
                double[] c = circles.get(0, i);
                if (c[2] > maxRadius) {
                    maxRadius = c[2];
                    bestCircle = c;
                }
            }
            
            edges.release();
            circles.release();
            return bestCircle;
        }
        
        edges.release();
        circles.release();
        return null;
    }

    private RotatedRect findBestEllipse(Mat inputMat) {
        // Apply threshold for contour detection
        Mat binary = new Mat();
        Imgproc.threshold(inputMat, binary, 127, 255, Imgproc.THRESH_BINARY_INV);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

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
            
            // Check if the ellipse is reasonably circular
            double aspectRatio = Math.max(ellipse.size.width, ellipse.size.height) / 
                                Math.min(ellipse.size.width, ellipse.size.height);
            
            if (area > 3000 && area > maxArea && aspectRatio < 2.0) {
                maxArea = area;
                bestEllipse = ellipse;
            }
            contour2f.release();
        }
        
        binary.release();
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
                
                // Optional: Draw center point
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle((float) center.x, (float) center.y, 5, paint);
                paint.setStyle(Paint.Style.STROKE);
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
                
                // Optional: Draw center point
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, y, 5, paint);
            }
        } finally {
            mOverlay.unlockCanvasAndPost(canvas);
        }
    }
}
