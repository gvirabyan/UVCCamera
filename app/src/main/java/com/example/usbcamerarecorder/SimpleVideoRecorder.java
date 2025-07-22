package com.example.usbcamerarecorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SimpleVideoRecorder {
    private static final String TAG = "SimpleVideoRecorder";
    private static final int TIMEOUT_USEC = 10000;

    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private Surface inputSurface;
    private boolean isRecording = false;
    private int trackIndex = -1;
    private boolean muxerStarted = false;
    private Thread encoderThread;
    private Callback callback;
    private final Object lock = new Object();


    public interface FrameProcessedCallback {
        void onFrameProcessed();
    }
    private FrameProcessedCallback frameProcessedCallback;



    public interface Callback {
        void onSurfaceReady(Surface surface);
        void onError(String message);
    }


    public void startRecording(File outputFile, int width, int height, Callback callback) {
        synchronized (lock) {
            if (isRecording) {
                callback.onError("Already recording");
                return;
            }
            this.callback = callback;

            try {
                MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000); // 4 Mbps
                format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 second
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
                format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);

                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                inputSurface = mediaCodec.createInputSurface();
                mediaCodec.start();

                mediaMuxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                isRecording = true;
                encoderThread = new Thread(this::drainEncoder);
                encoderThread.start();

                callback.onSurfaceReady(inputSurface);
                Log.i(TAG, "Recording setup complete. Surface ready.");

            } catch (IOException e) {
                Log.e(TAG, "startRecording failed", e);
                cleanup();
                callback.onError("Failed to start recording: " + e.getMessage());
            }
        }
    }

    private void drainEncoder() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) {
            synchronized (lock) {
                if (!isRecording) {
                    Log.i(TAG, "drainEncoder: Not recording, exiting loop.");
                    break;
                }
            }

            try {
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    setupMuxer();
                } else if (outputBufferId >= 0) {
                    processOutputBuffer(outputBufferId, bufferInfo);
                    // 👉 Вызываем колбэк после успешной обработки и записи кадра
                    if (frameProcessedCallback != null) {
                        frameProcessedCallback.onFrameProcessed();
                    }
                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // No output available yet
                } else {
                    Log.w(TAG, "Unexpected outputBufferId: " + outputBufferId);
                }
            } catch (Exception e) {
                Log.e(TAG, "drainEncoder error", e);
                if (callback != null) {
                    callback.onError("Encoder error during drain: " + e.getMessage());
                }
                break;
            }
        }

        Log.i(TAG, "drainEncoder loop finished. Initiating final cleanup.");
        cleanup();
    }

    private void setupMuxer() {
        synchronized (lock) {
            if (muxerStarted) {
                Log.e(TAG, "Format changed twice, this should not happen.");
                if (callback != null) {
                    callback.onError("Muxer error: Format changed twice");
                }
                return;
            }

            try {
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                trackIndex = mediaMuxer.addTrack(newFormat);
                mediaMuxer.start();
                muxerStarted = true;
                Log.i(TAG, "Muxer started with track index: " + trackIndex);
            } catch (Exception e) {
                Log.e(TAG, "setupMuxer failed", e);
                if (callback != null) {
                    callback.onError("Muxer setup failed: " + e.getMessage());
                }
            }
        }
    }

    private void processOutputBuffer(int outputBufferId, MediaCodec.BufferInfo bufferInfo) {
        synchronized (lock) {
            try {
                ByteBuffer encodedData = mediaCodec.getOutputBuffer(outputBufferId);
                if (encodedData == null) {
                    Log.e(TAG, "Encoder output buffer was null for id: " + outputBufferId);
                    return;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size > 0 && muxerStarted) {
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                }

                mediaCodec.releaseOutputBuffer(outputBufferId, false);
            } catch (Exception e) {
                Log.e(TAG, "processOutputBuffer error", e);
                if (callback != null) {
                    callback.onError("Output processing error: " + e.getMessage());
                }
            }
        }
    }

    public void stopRecording() {
        synchronized (lock) {
            if (!isRecording) {
                Log.d(TAG, "stopRecording: Not recording, returning.");
                return;
            }
            isRecording = false;
            Log.i(TAG, "stopRecording: Setting isRecording to false.");
        }

        try {
            if (encoderThread != null) {
                Log.i(TAG, "stopRecording: Joining encoder thread.");
                encoderThread.join(2000);
                if (encoderThread.isAlive()) {
                    Log.w(TAG, "Encoder thread did not terminate gracefully.");
                }
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "stopRecording: Interrupted while waiting for encoder thread", e);
            Thread.currentThread().interrupt();
        } finally {
            synchronized (lock) {
                if (mediaCodec != null || mediaMuxer != null || inputSurface != null) {
                    Log.w(TAG, "Cleanup was not fully performed by encoder thread, performing now.");
                    cleanup();
                }
            }
        }
    }

    private void cleanup() {
        synchronized (lock) {
            Log.i(TAG, "Initiating cleanup process.");
            try {
                if (mediaCodec != null) {
                    Log.d(TAG, "Releasing mediaCodec.");
                    try {
                        mediaCodec.stop();
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to stop mediaCodec during cleanup", e);
                    }
                    mediaCodec.release();
                    mediaCodec = null;
                }

                if (mediaMuxer != null) {
                    Log.d(TAG, "Releasing mediaMuxer.");
                    try {
                        if (muxerStarted) {
                            mediaMuxer.stop();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to stop mediaMuxer during cleanup", e);
                    }
                    mediaMuxer.release();
                    mediaMuxer = null;
                }

                if (inputSurface != null) {
                    Log.d(TAG, "Releasing inputSurface.");
                    inputSurface.release();
                    inputSurface = null;
                }

                muxerStarted = false;
                trackIndex = -1;
                Log.i(TAG, "Cleanup complete.");
            } catch (Exception e) {
                Log.e(TAG, "Cleanup failed unexpectedly", e);
                if (callback != null) {
                    callback.onError("Cleanup error: " + e.getMessage());
                }
            }
        }
    }
}