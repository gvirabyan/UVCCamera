package com.example.usbcamerarecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 1;

    private TextView tvDeviceStatus;
    private Button btnRecord;
    private TextureView cameraPreviewTextureView;

    private CameraManager mCameraManager;
    private OpenCVProcessor mOpenCVProcessor;

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

        mOpenCVProcessor = new OpenCVProcessor(cameraPreviewTextureView, findViewById(R.id.tv_fps));

        mCameraManager = new CameraManager(this, tvDeviceStatus, cameraPreviewTextureView, new CameraManager.CameraListener() {
            @Override
            public void onCameraStarted() {
                MyLogger.log("CameraManager: Camera started. Scheduling OpenCV processing with a delay.");
                runOnUiThread(() -> {
                    btnRecord.setEnabled(true);
                    btnRecord.setText("Record");
                    // Добавляем задержку в 500 миллисекунд
                    new Handler().postDelayed(() -> {
                        MyLogger.log("Delayed start of OpenCV processing.");
                        mOpenCVProcessor.startProcessing();
                    }, 5000);
                });
            }

            @Override
            public void onCameraStopped() {
                MyLogger.log("CameraManager: Camera stopped. Stopping OpenCV processing.");
                runOnUiThread(() -> {
                    btnRecord.setEnabled(false);
                    mOpenCVProcessor.stopProcessing();
                });
            }

            @Override
            public void onRecordingStarted() {
                MyLogger.log("CameraManager: Recording started. Stopping OpenCV processing.");
                runOnUiThread(() -> {
                    btnRecord.setText("Stop");
                    mOpenCVProcessor.stopProcessing();
                });
            }

            @Override
            public void onRecordingStopped() {
                MyLogger.log("CameraManager: Recording stopped. Starting OpenCV processing.");
                runOnUiThread(() -> {
                    btnRecord.setText("Record");
                    mOpenCVProcessor.startProcessing();
                });
            }
        });

        if (checkPermissions()) {
            // Вызов initUSBCamera() после получения всех разрешений.
            mCameraManager.initUSBCamera();
        } else {
            requestPermissions();
        }

        setupRecordButton();
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btn_record);
        tvDeviceStatus = findViewById(R.id.tv_device_status);
        tvDeviceStatus.setText("Waiting for USB device");
        cameraPreviewTextureView = findViewById(R.id.camera_view);
    }

    private void setupRecordButton() {
        btnRecord.setOnClickListener(v -> {
            if (mCameraManager.isRecording()) {
                mCameraManager.stopRecording();
            } else {
                mCameraManager.startRecording();
            }
        });
        btnRecord.setEnabled(false);
    }

    private boolean checkPermissions() {
        String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        } else {
            requiredPermissions = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        }
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                MyLogger.log("Missing permission: " + perm);
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        } else {
            permissionsToRequest = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE };
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
                MyLogger.log("Permissions granted. Initializing USB camera.");
                mCameraManager.initUSBCamera();
            } else {
                MyLogger.log("Permissions denied. Cannot operate the camera.");
                Toast.makeText(this, "Permissions denied. Cannot operate the camera.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCameraManager != null) {
            // Регистрация USBMonitor в onStart()
            mCameraManager.registerUSBMonitor();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCameraManager != null) {
            // Снятие с регистрации USBMonitor в onStop()
            mCameraManager.unregisterUSBMonitor();
        }
    }

    @Override
    protected void onDestroy() {
        if (mCameraManager != null) {
            mCameraManager.destroy();
        }
        if (mOpenCVProcessor != null) {
            mOpenCVProcessor.destroy();
        }
        super.onDestroy();
    }
}