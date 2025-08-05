package com.example.usbcamerarecorder;

import java.util.concurrent.atomic.AtomicLong;

public class FpsCalculator {

    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong startTimeNano = new AtomicLong(0);
    private static final long SAMPLING_INTERVAL_NANO = 1_000_000_000L; // 1 секунда
    private double currentFps = 0.0;
    private FpsListener listener;

    public interface FpsListener {
        void onFpsUpdate(double fps);
    }

    public void setListener(FpsListener listener) {
        this.listener = listener;
    }

    public void frameProcessed() {
        frameCount.incrementAndGet();
        long currentTimeNano = System.nanoTime();
        long startTime = startTimeNano.get();

        if (startTime == 0) {
            startTimeNano.set(currentTimeNano);
            return;
        }

        long elapsedTime = currentTimeNano - startTime;

        if (elapsedTime >= SAMPLING_INTERVAL_NANO) {
            currentFps = frameCount.get() * (double) SAMPLING_INTERVAL_NANO / elapsedTime;
            if (listener != null) {
                listener.onFpsUpdate(currentFps);
            }
            startTimeNano.set(currentTimeNano);
            frameCount.set(0);
        }
    }

    public void reset() {
        frameCount.set(0);
        startTimeNano.set(0);
        currentFps = 0.0;
    }
}