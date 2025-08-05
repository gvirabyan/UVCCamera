package com.example.usbcamerarecorder;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.jiangdg.usb.USBMonitor;
import com.jiangdg.uvc.UVCCamera;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private final Context mContext;
    private final TextView mTvDeviceStatus;
    private final TextureView mCameraPreviewTextureView;
    private final CameraListener mListener;

    private USBMonitor mUsbMonitor;
    private UVCCamera mUvcCamera;
    private Surface mPreviewSurface;
    private SimpleVideoRecorder mVideoRecorder;

    private boolean isRecording = false;
    private boolean isSurfaceAvailable = false;
    private final Object mCameraLock = new Object();

    public interface CameraListener {
        void onCameraStarted();
        void onCameraStopped();
        void onRecordingStarted();
        void onRecordingStopped();
    }

    public CameraManager(Context context, TextView tvDeviceStatus, TextureView textureView, CameraListener listener) {
        this.mContext = context;
        this.mTvDeviceStatus = tvDeviceStatus;
        this.mCameraPreviewTextureView = textureView;
        this.mListener = listener;
        setupSurfaceListener();
    }

    private void setupSurfaceListener() {
        mCameraPreviewTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                MyLogger.log("onSurfaceTextureAvailable: Surface is ready.");
                isSurfaceAvailable = true;
                surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT);
                synchronized (mCameraLock) {
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                    }
                    mPreviewSurface = new Surface(surfaceTexture);
                    if (mUvcCamera != null && !isRecording) {
                        tryStartPreview();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                MyLogger.log("onSurfaceTextureDestroyed: Releasing preview surface.");
                isSurfaceAvailable = false;
                synchronized (mCameraLock) {
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    if (mUvcCamera != null) {
                        mUvcCamera.stopPreview();
                    }
                }
                if (mListener != null) {
                    mListener.onCameraStopped();
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {}
        });
    }

    public void initUSBCamera() {
        mUsbMonitor = new USBMonitor(mContext, new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(UsbDevice device) {
                MyLogger.log("onAttach: USB device attached. Name: " + device.getDeviceName());
                if (isUvcDevice(device)) {
                    showDeviceAlert(device);
                    mUsbMonitor.requestPermission(device);
                }
            }
            @Override
            public void onDetach(UsbDevice device) {
                MyLogger.log("onDetach: USB device detached. Name: " + device.getDeviceName());
                if (mUvcCamera != null && mUvcCamera.getDevice().equals(device)) {
                    closeCamera();
                }
            }
            @Override
            public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                MyLogger.log("onConnect: USB device connected. Scheduling camera open with delay.");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Проверяем, что устройство все еще подключено, используя contains() для List
                    if (mUsbMonitor.getDeviceList().contains(device)) {
                        MyLogger.log("onConnect: Delay finished. Opening camera.");
                        openCamera(ctrlBlock);
                    } else {
                        MyLogger.log("onConnect: Device disconnected during delay. Aborting.");
                    }
                }, 500); // Задержка 500 миллисекунд
            }
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                MyLogger.log("onDisconnect: USB device disconnected. Closing camera.");
                closeCamera();
            }
            @Override
            public void onCancel(UsbDevice device) {
                MyLogger.log("onCancel: USB permission denied by user.");
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "USB permission denied", Toast.LENGTH_SHORT).show());
            }
        });
    }

    public void registerUSBMonitor() {
        if (mUsbMonitor != null) {
            try {
                int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                        PendingIntent.FLAG_UPDATE_CURRENT;
                Intent intent = new Intent("com.serenegiant.USB_PERMISSION." + mUsbMonitor.hashCode());
                intent.setPackage(mContext.getPackageName());
                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, intent, flags);
                Field field = USBMonitor.class.getDeclaredField("mPermissionIntent");
                field.setAccessible(true);
                field.set(mUsbMonitor, pi);
                mUsbMonitor.register();
                MyLogger.log("USBMonitor registered successfully.");
                checkConnectedDevices();
            } catch (Exception e) {
                MyLogger.log("Error registering USBMonitor: " + e.getMessage());
                Toast.makeText(mContext, "Error registering USBMonitor", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void unregisterUSBMonitor() {
        if (mUsbMonitor != null) {
            mUsbMonitor.unregister();
            MyLogger.log("USBMonitor unregistered.");
        }
    }

    private void checkConnectedDevices() {
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() -> mTvDeviceStatus.setText("No devices connected"));
        } else {
            boolean uvcDeviceFound = false;
            for (UsbDevice device : deviceList.values()) {
                if (isUvcDevice(device)) {
                    mUsbMonitor.requestPermission(device);
                    uvcDeviceFound = true;
                    break;
                }
            }
            if (!uvcDeviceFound) {
                new Handler(Looper.getMainLooper()).post(() -> mTvDeviceStatus.setText("UVC device not found"));
            }
        }
    }

    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        synchronized (mCameraLock) {
            try {
                closeCamera();
                MyLogger.log("openCamera: closeCamera() finished, creating new UVCCamera.");
                mUvcCamera = new UVCCamera();
                MyLogger.log("openCamera: Calling mUvcCamera.open(ctrlBlock).");
                mUvcCamera.open(ctrlBlock);
                MyLogger.log("openCamera: mUvcCamera.open() succeeded. Setting preview size.");
                mUvcCamera.setPreviewSize(WIDTH, HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                new Handler(Looper.getMainLooper()).post(() -> mTvDeviceStatus.setText("Device connected"));
                if (isSurfaceAvailable) {
                    tryStartPreview();
                } else {
                    MyLogger.log("Camera opened, but surface not ready. Will wait for surface availability.");
                }
            } catch (Exception e) {
                MyLogger.log("Error opening camera: " + e.getMessage());
                closeCamera();
            }
        }
    }

    private void tryStartPreview() {
        synchronized (mCameraLock) {
            if (mUvcCamera == null || !isSurfaceAvailable || mPreviewSurface == null) {
                MyLogger.log("tryStartPreview: Skipping, conditions not met. UVC: " + (mUvcCamera != null) + ", Surface: " + isSurfaceAvailable);
                return;
            }
            try {
                MyLogger.log("tryStartPreview: Setting preview display on surface.");
                mUvcCamera.setPreviewDisplay(mPreviewSurface);
                mUvcCamera.startPreview();
                if (mListener != null) {
                    mListener.onCameraStarted();
                }
                MyLogger.log("Preview started successfully.");
            } catch (Exception e) {
                MyLogger.log("Error starting preview: " + e.getMessage());
            }
        }
    }

    private void closeCamera() {
        synchronized (mCameraLock) {
            if (mUvcCamera != null) {
                try {
                    mUvcCamera.stopPreview();
                    mUvcCamera.destroy();
                } catch (Exception e) {
                    MyLogger.log("Error closing camera: " + e.getMessage());
                } finally {
                    mUvcCamera = null;
                }
            }
        }
        new Handler(Looper.getMainLooper()).post(() -> mTvDeviceStatus.setText("Disconnected"));
        if (mListener != null) {
            mListener.onCameraStopped();
        }
    }

    // **ИСПРАВЛЕННЫЙ МЕТОД**
    public void startRecording(Surface recorderSurface) {
        MyLogger.log("startRecording called with recorder surface.");
        synchronized (mCameraLock) {
            if (mUvcCamera != null) {
                mUvcCamera.stopPreview();
                isRecording = true;
                // Перенаправляем камеру на Surface кодировщика
                mUvcCamera.setPreviewDisplay(recorderSurface);
                mUvcCamera.startPreview();
            }
        }
        if (mListener != null) {
            mListener.onRecordingStarted();
        }
    }

    // **ИСПРАВЛЕННЫЙ МЕТОД**
    public void stopRecording() {
        MyLogger.log("stopRecording called");
        synchronized (mCameraLock) {
            if (mUvcCamera != null && isRecording) {
                mUvcCamera.stopPreview();
                // Возвращаем предпросмотр на исходную поверхность (TextureView)
                mUvcCamera.setPreviewDisplay(mPreviewSurface);
                // Перезапускаем предпросмотр
                mUvcCamera.startPreview();
                isRecording = false;
            }
        }
        if (mListener != null) {
            mListener.onRecordingStopped();
        }
    }
    private boolean isUvcDevice(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) {
                return true;
            }
        }
        boolean isMiscUVC = device.getDeviceClass() == UsbConstants.USB_CLASS_MISC && device.getDeviceSubclass() == 0x02 && device.getDeviceProtocol() == 0x01;
        boolean isVideoClass = device.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO;
        return isMiscUVC || isVideoClass;
    }

    private void showDeviceAlert(UsbDevice device) {
        new Handler(Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(mContext)
                    .setTitle("UVC Camera Connected")
                    .setMessage(String.format("Name: %s\nVID: 0x%04X\nPID: 0x%04X", device.getDeviceName(), device.getVendorId(), device.getProductId()))
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void destroy() {
        closeCamera();
        if (mUsbMonitor != null) mUsbMonitor.destroy();
        if (mPreviewSurface != null) mPreviewSurface.release();
    }
}