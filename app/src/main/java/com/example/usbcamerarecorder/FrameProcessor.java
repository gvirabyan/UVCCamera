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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE;

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
                        processFrameForOCR(bmp);
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
}