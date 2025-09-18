package com.example.usbcamerarecorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.widget.TextView;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

public class OpenCVProcessor {

    private static final int CAM_WIDTH = 640;
    private static final int CAM_HEIGHT = 480;
    private static final long FRAME_DELAY_MS = 50;
    private static final Size GAUSS_KSIZE = new Size(5, 5);

    private final TextureView mPreview;
    private final TextureView mOverlay;
    private final TextView mTvFps;
    private final TextView mTvText;

    private HandlerThread mThread;
    private Handler mHandler;
    private boolean running = false;

    private Mat mRgba, mGray, mEdges;
    private final long[] frameTimes = new long[12];
    private int frameIdx = 0;

    // ✅ OCR
    private TextRecognitionProcessor mTextProcessor;
    private int frameCounter = 0; // счётчик кадров для OCR

    public OpenCVProcessor(Context context,
                           TextureView preview,
                           TextureView overlay,
                           TextView tvFps,
                           TextView tvText) {
        this.mPreview = preview;
        this.mOverlay = overlay;
        this.mTvFps = tvFps;
        this.mTvText = tvText;
        mOverlay.setOpaque(false);
        startThread();

        // OCR init
        mTextProcessor = new TextRecognitionProcessor(text ->
                mTvText.post(() -> mTvText.setText(text))
        );
    }

    private void startThread() {
        mThread = new HandlerThread("OpenCV-Processor");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    public void start() {
        if (!running) {
            running = true;
            mHandler.post(processFrame);
        }
    }

    public void stop() {
        if (running) {
            running = false;
            mHandler.removeCallbacks(processFrame);
        }
    }

    public void release() {
        stop();
        if (mThread != null) {
            mThread.quitSafely();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (mTextProcessor != null) mTextProcessor.release();
        releaseMat(mRgba);
        releaseMat(mGray);
        releaseMat(mEdges);
    }

    private void releaseMat(Mat m) {
        if (m != null) m.release();
    }

    // --- Главный цикл обработки кадров ---
    private final Runnable processFrame = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long t0 = System.currentTimeMillis();

            try {
                if (!mPreview.isAvailable()) {
                    mHandler.postDelayed(this, FRAME_DELAY_MS);
                    return;
                }

                initMats();

                Bitmap bmp = mPreview.getBitmap(CAM_WIDTH, CAM_HEIGHT);
                if (bmp == null) {
                    mHandler.postDelayed(this, FRAME_DELAY_MS);
                    return;
                }

                // OpenCV: переводим bitmap → Mat
                Utils.bitmapToMat(bmp, mRgba);
                bmp.recycle();

                // ✅ Улучшаем изображение для OCR
                Mat ocrMat = new Mat();
                // 1. Преобразуем в оттенки серого
                Imgproc.cvtColor(mRgba, ocrMat, Imgproc.COLOR_RGBA2GRAY);

                // 2. Улучшаем контраст (коэффициент 2.0 для более агрессивного контраста)
                Core.convertScaleAbs(ocrMat, ocrMat, 2.0, 0);

                // 3. Бинаризация: превращаем изображение в черно-белое
                Imgproc.threshold(ocrMat, ocrMat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

                // Преобразуем Mat для OCR обратно в Bitmap
                Bitmap ocrBmp = Bitmap.createBitmap(ocrMat.cols(), ocrMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(ocrMat, ocrBmp);

                // Запускаем OCR с улучшенным изображением
                frameCounter++;
                if (frameCounter % 10 == 0) { // уменьшил частоту до 10 кадров для более быстрой реакции
                    if (mTextProcessor != null) {
                        mTextProcessor.process(ocrBmp);
                    }
                }

                ocrBmp.recycle();
                ocrMat.release();

                // Преобразование в оттенки серого
                Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

                // Сглаживание для удаления шума
                Imgproc.GaussianBlur(mGray, mGray, GAUSS_KSIZE, 0);

                // Инвертирование, чтобы круг стал белым
                Core.bitwise_not(mGray, mGray);

                // --- Поиск и отрисовка фигур ---
                double[] foundCircle = detectCircleHough(mGray);
                RotatedRect foundEllipse = null;

                if (foundCircle == null) {
                    // если не нашли круг, пробуем эллипс
                    foundEllipse = findBestEllipse(mGray);
                }

                // Рисуем найденные фигуры
                drawOverlay(foundCircle, foundEllipse);

                updateFps(t0);

            } catch (Throwable e) {
                e.printStackTrace();
            }

            mHandler.postDelayed(this, FRAME_DELAY_MS);
        }

    };

    private void initMats() {
        if (mRgba == null) mRgba = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC4);
        if (mGray == null) mGray = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC1);
        if (mEdges == null) mEdges = new Mat(CAM_HEIGHT, CAM_WIDTH, CvType.CV_8UC1);
    }

    private void updateFps(long t0) {
        long dt = Math.max(1, System.currentTimeMillis() - t0);
        frameTimes[frameIdx] = dt;
        frameIdx = (frameIdx + 1) % frameTimes.length;
        long sum = 0;
        int count = 0;
        for (long v : frameTimes) {
            if (v > 0) {
                sum += v;
                count++;
            }
        }
        double avg = count > 0 ? (sum / (double) count) : dt;
        final double fps = 1000.0 / avg;
        mTvFps.post(() -> mTvFps.setText(String.format("FPS: %.1f", fps)));
    }

    // --- Поиск круга через HoughCircles ---
    private double[] detectCircleHough(Mat grayFrame) {
        Mat circles = new Mat();
        Imgproc.HoughCircles(
                grayFrame,
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.5, // dp
                grayFrame.rows() / 8, // minDist
                150, // param1
                50, // param2
                100, // minRadius
                300  // maxRadius
        );

        if (circles.cols() > 0) {
            double[] c = circles.get(0, 0);
            circles.release();
            return c;
        }
        circles.release();
        return null;
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
            if (area > 5000 && area > maxArea) {
                maxArea = area;
                bestEllipse = ellipse;
            }

            contour2f.release();
        }

        hierarchy.release();
        return bestEllipse;
    }

    // --- Рисуем оверлей ---
    private void drawOverlay(double[] circle, RotatedRect ellipse) {
        Canvas canvas = mOverlay.lockCanvas();
        if (canvas == null) return;

        try {
            // Очищаем канвас
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Настройки кисти
            Paint paint = new Paint();
            paint.setColor(Color.GREEN); // <--- Зеленый цвет
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setAntiAlias(true);

            // Рисуем круг
            if (circle != null) {
                Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                int radius = (int) Math.round(circle[2]);
                canvas.drawCircle((float) center.x, (float) center.y, radius, paint);
            }

            // Рисуем эллипс
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