package com.example.usbcamerarecorder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class OpenCVProcessor {
    private static final String TAG = "OpenCVProcessor";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private final TextureView mTextureView;
    private final TextView mTvFps;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private final Paint mPaint = new Paint();
    private Mat mRgba;
    private Mat mGray;
    private Mat mCircles;

    private boolean isProcessing = false;

    public OpenCVProcessor(TextureView textureView, TextView tvFps) {
        this.mTextureView = textureView;
        this.mTvFps = tvFps;

        mRgba = new Mat();
        mGray = new Mat();
        mCircles = new Mat();

        mPaint.setColor(0xFFFF0000);
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);

        mHandlerThread = new HandlerThread("OpenCVThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void startProcessing() {
        if (!isProcessing) {
            isProcessing = true;
            mHandler.post(mProcessFramesRunnable);
        }
    }

    public void stopProcessing() {
        if (isProcessing) {
            mHandler.removeCallbacks(mProcessFramesRunnable);
            isProcessing = false;
        }
    }

    private final Runnable mProcessFramesRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isProcessing) {
                return;
            }

            long startTime = System.currentTimeMillis();
            Bitmap currentBitmap = null;
            try {
                if (mTextureView == null || !mTextureView.isAvailable()) {
                    mHandler.postDelayed(this, 100);
                    return;
                }

                currentBitmap = mTextureView.getBitmap(WIDTH, HEIGHT);
                // **ОБНОВЛЕНИЕ: Убедимся, что Bitmap не только не null, но и имеет правильные размеры**
                if (currentBitmap == null || currentBitmap.getWidth() != WIDTH || currentBitmap.getHeight() != HEIGHT) {
                    if (currentBitmap != null) {
                        currentBitmap.recycle();
                    }
                    MyLogger.log("Skipping frame: Bitmap is not ready yet.");
                    mHandler.postDelayed(this, 100);
                    return;
                }

                // **ДОБАВЛЕНЫ ЛОГИ, ЧТОБЫ НАЙТИ ТОЧНОЕ МЕСТО СБОЯ**
                MyLogger.log("Processing frame...");
                Utils.bitmapToMat(currentBitmap, mRgba);
                MyLogger.log("Utils.bitmapToMat() succeeded.");
                Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
                MyLogger.log("cvtColor() succeeded.");
                Imgproc.GaussianBlur(mGray, mGray, new org.opencv.core.Size(9, 9), 2, 2);
                MyLogger.log("GaussianBlur() succeeded.");
                Imgproc.HoughCircles(mGray, mCircles, Imgproc.HOUGH_GRADIENT, 1.0, (double) mGray.rows() / 8, 100.0, 30.0, 0, 0);
                MyLogger.log("HoughCircles() succeeded.");

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                final double fps = 1000.0 / (duration > 0 ? duration : 1);

                Bitmap finalCurrentBitmap = currentBitmap;
                mTextureView.post(() -> {
                    if (!isProcessing || mTextureView == null) return;
                    Canvas canvas = mTextureView.lockCanvas();
                    if (canvas != null) {
                        canvas.drawBitmap(finalCurrentBitmap, 0, 0, null);
                        if (mCircles.cols() > 0) {
                            for (int x = 0; x < mCircles.cols(); x++) {
                                double[] circle = mCircles.get(0, x);
                                if (circle != null) {
                                    Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                                    int radius = (int) Math.round(circle[2]);
                                    canvas.drawCircle((float) center.x, (float) center.y, radius, mPaint);
                                }
                            }
                        }
                        mTextureView.unlockCanvasAndPost(canvas);
                        mTvFps.setText(String.format("FPS: %.1f", fps));
                    }
                });

            } catch (Exception e) {
                MyLogger.log("Error during OpenCV processing: " + e.getMessage());
            } finally {
                if (currentBitmap != null) {
                    currentBitmap.recycle();
                }
            }
            mHandler.postDelayed(this, 100);
        }
    };

    public void destroy() {
        stopProcessing();
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
        if (mRgba != null) mRgba.release();
        if (mGray != null) mGray.release();
        if (mCircles != null) mCircles.release();
    }
}