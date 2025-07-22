package com.example.usbcamerarecorder;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyLogger {
    private static final String TAG = "AppLogger";
    private static File logFile;

    public static void init(Context context) {
        try {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "External storage not available");
                return;
            }

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            File logDir = new File(dir, "USBCameraRecorderLogs");
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.e(TAG, "Cannot create log directory");
                return;
            }

            String date = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            logFile = new File(logDir, "usb_camera_log_" + date + ".txt");

            writeToFile("=== USB Camera Recorder Log ===");
            writeToFile("Initialized at: " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            Log.i(TAG, "Logger initialized. Path: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Logger init failed", e);
        }
    }

    public static void log(String text) {
        Log.d(TAG, text);
        writeToFile(text);
    }

    private static void writeToFile(String text) {
        if (logFile == null) {
            Log.e(TAG, "Log file not initialized");
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date());
        String logMessage = timeStamp + " - " + text + "\n";

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(logMessage);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log", e);
        }
    }

    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "Not available";
    }
}