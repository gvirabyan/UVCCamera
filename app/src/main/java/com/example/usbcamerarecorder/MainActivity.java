package com.example.usbcamerarecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private TextView tvDeviceStatus;
    private TextView tvFps;
    private TextView tvOcrResult;
    private Button btnRecord;
    private TextureView cameraPreviewTextureView;
    private TextureView overlayView;

    private CameraManager mCameraManager;
    private FrameProcessor mFrameProcessor; // ✅ Заменяем OpenCVProcessor на FrameProcessor
    private SimpleVideoRecorder mVideoRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLogger.init(this);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initDebug()) {
            MyLogger.log("OpenCV failed to load!");
            Toast.makeText(this, "OpenCV failed to load!", Toast.LENGTH_LONG).show();
        } else {
            MyLogger.log("OpenCV loaded successfully.");
        }

        initViews();
        mVideoRecorder = new SimpleVideoRecorder();

        // === Инициализируем новый FrameProcessor ===
        mFrameProcessor = new FrameProcessor(cameraPreviewTextureView, overlayView, tvOcrResult);

        mCameraManager = new CameraManager(this, tvDeviceStatus, cameraPreviewTextureView, new CameraManager.CameraListener() {
            @Override
            public void onCameraStarted() {
                runOnUiThread(() -> {
                    btnRecord.setEnabled(true);
                    btnRecord.setText("Record");
                    // Запускаем новый FrameProcessor
                    mFrameProcessor.start();
                });
            }

            @Override
            public void onCameraStopped() {
                runOnUiThread(() -> {
                    btnRecord.setEnabled(false);
                    // Останавливаем новый FrameProcessor
                    mFrameProcessor.stop();
                });
            }

            @Override
            public void onRecordingStarted() {
                runOnUiThread(() -> {
                    btnRecord.setText("Stop");
                    // Останавливаем FrameProcessor, чтобы избежать конфликтов при записи
                    mFrameProcessor.stop();
                });
            }

            @Override
            public void onRecordingStopped() {
                runOnUiThread(() -> {
                    btnRecord.setText("Record");
                    // Снова запускаем FrameProcessor после остановки записи
                    mFrameProcessor.start();
                });
            }
        });

        if (checkPermissions()) {
            mCameraManager.initUSBCamera();
        } else {
            requestPermissions();
        }

        setupRecordButton();
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btn_record);
        tvDeviceStatus = findViewById(R.id.tv_device_status);
        tvFps = findViewById(R.id.tv_fps);
        tvOcrResult = findViewById(R.id.tv_recognized_text);
        tvDeviceStatus.setText("Waiting for USB device");

        cameraPreviewTextureView = findViewById(R.id.camera_view);
        overlayView = findViewById(R.id.overlay_view);
        overlayView.setAlpha(0.5f);
    }

    private void setupRecordButton() {
        btnRecord.setOnClickListener(v -> {
            if (mCameraManager.isRecording()) {
                mVideoRecorder.stopRecording();
                mCameraManager.stopRecording();
            } else {
                File outputFile = getOutputFile();
                if (outputFile != null) {
                    Surface recorderSurface = mVideoRecorder.startRecording(outputFile, WIDTH, HEIGHT);
                    if (recorderSurface != null) {
                        mCameraManager.startRecording(recorderSurface);
                    } else {
                        Toast.makeText(this, "Failed to get recording surface.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to create output file.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnRecord.setEnabled(false);
    }

    private File getOutputFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "VIDEO_" + timeStamp + ".mp4";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "USBCameraRecorder");
        if (!storageDir.exists()) storageDir.mkdirs();
        return new File(storageDir, fileName);
    }

    private boolean checkPermissions() {
        String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        } else {
            requiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        } else {
            permissionsToRequest = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
        ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                mCameraManager.initUSBCamera();
            } else {
                Toast.makeText(this, "Permissions denied. Cannot operate the camera.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCameraManager != null) mCameraManager.registerUSBMonitor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCameraManager != null) mCameraManager.unregisterUSBMonitor();
    }

    @Override
    protected void onDestroy() {
        if (mCameraManager != null) mCameraManager.destroy();
        if (mFrameProcessor != null) mFrameProcessor.stop(); // ✅ Останавливаем новый процессор
        super.onDestroy();
    }
}