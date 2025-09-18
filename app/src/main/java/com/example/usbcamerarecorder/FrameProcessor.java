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
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

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

    private final ObjectDetector objectDetector;
    private final TextRecognizer textRecognizer;
    private final ExecutorService mExecutor;

    private boolean running = false;
    private long lastOcrTime = 0;

    public FrameProcessor(TextureView preview, TextureView overlay, TextView tvOcrResult) {
        this.mPreview = preview;
        this.mOverlay = overlay;
        this.mTvOcrResult = tvOcrResult;
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()  // Optional
                        .build();
        this.objectDetector = ObjectDetection.getClient(options);
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
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
                        Bitmap bmpForObjectDetection = bmp.copy(bmp.getConfig(), true);
                        Bitmap bmpForTextRecognition = bmp.copy(bmp.getConfig(), true);
                        bmp.recycle();

                        processFrameForObjects(bmpForObjectDetection);
                        processFrameForText(bmpForTextRecognition);
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

    private void processFrameForObjects(Bitmap bmp) {
        InputImage image = InputImage.fromBitmap(bmp, 0);
        objectDetector.process(image)
                .addOnSuccessListener(
                        detectedObjects -> {
                            drawOverlay(detectedObjects);
                            bmp.recycle();
                        })
                .addOnFailureListener(
                        e -> {
                            Log.e(TAG, "Object detection failed", e);
                            bmp.recycle();
                        });
    }

    private void drawOverlay(java.util.List<com.google.mlkit.vision.objects.DetectedObject> detectedObjects) {
        Canvas canvas = mOverlay.lockCanvas();
        if (canvas == null) return;

        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setAntiAlias(true);

            for (com.google.mlkit.vision.objects.DetectedObject detectedObject : detectedObjects) {
                canvas.drawRect(detectedObject.getBoundingBox(), paint);
            }
        } finally {
            mOverlay.unlockCanvasAndPost(canvas);
        }
    }

    private void processFrameForText(Bitmap bmp) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOcrTime < 500) { // Process text every 500ms
            bmp.recycle();
            return;
        }
        lastOcrTime = currentTime;

        InputImage image = InputImage.fromBitmap(bmp, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(
                        text -> {
                            String recognizedText = text.getText().trim();
                            if (!recognizedText.isEmpty()) {
                                updateTextView(recognizedText);
                            } else {
                                updateTextView("NONE");
                            }
                            bmp.recycle();
                        })
                .addOnFailureListener(
                        e -> {
                            Log.e(TAG, "Text recognition failed", e);
                            updateTextView("NONE");
                            bmp.recycle();
                        });
    }

    private void updateTextView(String text) {
        mTvOcrResult.post(() -> mTvOcrResult.setText(text));
    }
}