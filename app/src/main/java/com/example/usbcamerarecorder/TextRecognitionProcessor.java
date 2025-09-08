package com.example.usbcamerarecorder;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class TextRecognitionProcessor {

    public interface Listener {
        void onTextRecognized(String text);
    }

    private final TextRecognizer recognizer;
    private final Listener listener;
    private long lastRun = 0;

    public TextRecognitionProcessor(Listener listener) {
        this.listener = listener;
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void process(Bitmap bmp) {
        // ограничим частоту вызова OCR (раз в 500мс)
        if (System.currentTimeMillis() - lastRun < 500) return;
        lastRun = System.currentTimeMillis();

        InputImage image = InputImage.fromBitmap(bmp, 0);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    StringBuilder sb = new StringBuilder();
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        sb.append(block.getText()).append("\n");
                    }
                    String recognizedText = sb.toString().trim();
                    if (listener != null) {
                        // Если текст найден, передаем его. Иначе - "NONE"
                        if (!recognizedText.isEmpty()) {
                            listener.onTextRecognized(recognizedText);
                            Log.d("OCR", "Text recognized: " + recognizedText);
                        } else {
                            listener.onTextRecognized("NONE");
                            Log.d("OCR", "No text found in this frame.");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Error: " + e.getMessage());
                    // При ошибке также выводим "NONE"
                    if (listener != null) {
                        listener.onTextRecognized("NONE");
                    }
                });
    }

    public void release() {
        recognizer.close();
    }
}