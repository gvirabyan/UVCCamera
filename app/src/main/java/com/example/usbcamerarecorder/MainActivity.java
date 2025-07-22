package com.example.usbcamerarecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jiangdg.usb.USBMonitor;
import com.jiangdg.uvc.UVCCamera;

import org.opencv.android.OpenCVLoader; // Добавлен импорт для инициализации OpenCV
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvException; // Добавлен для отлова ошибок конвертации

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USBCamera";
    private static final int REQUEST_PERMISSIONS = 1;
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private TextView tvDeviceStatus;
    private UVCCamera uvcCamera;
    private USBMonitor usbMonitor;
    private Button btnRecord;
    private TextView tvFps;
    private boolean isRecording = false;
    private TextureView cameraPreviewTextureView;
    private Surface previewSurface;
    private Surface recordingSurface;
    private SimpleVideoRecorder videoRecorder; // Убедитесь, что SimpleVideoRecorder доступен
    private Context context;

    private FpsCalculator fpsCalculator; // Убедитесь, что FpsCalculator доступен

    // Переменные OpenCV
    private Mat mRgba;
    private Mat mGray;
    private Bitmap mBitmap;
    private boolean mIsFrameProcessing = false; // Флаг для предотвращения одновременной обработки

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация OpenCV
        if (!OpenCVLoader.initDebug()) { // Используем initDebug для отладки
            Log.e(TAG, "OpenCV не загружен. Убедитесь, что зависимость правильно настроена.");
            Toast.makeText(this, "OpenCV не смог загрузиться! Проверьте зависимости.", Toast.LENGTH_LONG).show();
            // В случае ошибки загрузки OpenCV, вы можете здесь отключить функционал, зависящий от него
        } else {
            Log.d(TAG, "OpenCV загружен успешно.");
        }

        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        MyLogger.init(this); // Убедитесь, что MyLogger доступен
        MyLogger.log("Application started");

        // Инициализация OpenCV Mat объектов
        // mRgba будет хранить конечное изображение в формате RGBA для отображения
        mRgba = new Mat(HEIGHT, WIDTH, CvType.CV_8UC4);
        // mGray будет хранить серое изображение для алгоритма HoughCircles
        mGray = new Mat(HEIGHT, WIDTH, CvType.CV_8UC1);
        // mBitmap используется для конвертации Mat в Bitmap для отрисовки на TextureView
        mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);


        initViews();
        setupSurfaceListener();

        initFpsCalculator();

        if (checkPermissions()) {
            initUSBCamera();
            checkConnectedDevices();
        } else {
            requestPermissions();
        }

        setupRecordButton();
    }

    private void initFpsCalculator() {
        fpsCalculator = new FpsCalculator();
        fpsCalculator.setListener(fps -> {
            runOnUiThread(() -> {
                String status = isRecording ? "Recording" : "Preview";
                tvFps.setText(String.format("FPS: %.1f (%s)", fps, status));
            });
        });
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btn_record);
        tvFps = findViewById(R.id.tv_fps);
        tvDeviceStatus = findViewById(R.id.tv_device_status);
        tvDeviceStatus.setText("Waiting for USB device");
        cameraPreviewTextureView = findViewById(R.id.camera_view);
    }

    private void setupSurfaceListener() {
        cameraPreviewTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                MyLogger.log("Surface available: " + width + "x" + height);
                surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT);
                synchronized (cameraLock) {
                    if (previewSurface != null) {
                        previewSurface.release();
                        previewSurface = null;
                    }
                    previewSurface = new Surface(surfaceTexture);
                    if (uvcCamera != null && !isRecording) {
                        tryStartPreview();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                MyLogger.log("Surface size changed: " + width + "x" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                MyLogger.log("Surface destroyed");
                if (previewSurface != null) {
                    previewSurface.release();
                    previewSurface = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // Не используется для расчета FPS здесь, FpsCalculator использует коллбэки кадров
            }
        });
    }

    private void tryStartPreview() {
        synchronized (cameraLock) {
            if (uvcCamera == null) {
                MyLogger.log("tryStartPreview: Camera is null");
                return;
            }

            if (previewSurface == null || !previewSurface.isValid()) {
                MyLogger.log("tryStartPreview: Surface is " +
                        (previewSurface == null ? "null" : "invalid"));
                return;
            }

            if (isRecording) {
                MyLogger.log("tryStartPreview: Skipping - recording in progress");
                return;
            }

            try {
                uvcCamera.setPreviewDisplay(previewSurface);
                uvcCamera.startPreview();
                MyLogger.log("Preview started successfully");
                runOnUiThread(() -> {
                    btnRecord.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Предпросмотр камеры запущен", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                MyLogger.log("Ошибка запуска предпросмотра: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Ошибка предпросмотра: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    private void checkConnectedDevices() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            MyLogger.log("USB устройства не найдены");
            runOnUiThread(() -> tvDeviceStatus.setText("Устройства не подключены"));
        } else {
            boolean uvcDeviceFound = false;
            for (UsbDevice device : deviceList.values()) {
                MyLogger.log("Найдено устройство: " + device.getDeviceName() + " VID: " + String.format("0x%04X", device.getVendorId()) + " PID: " + String.format("0x%04X", device.getProductId()));
                if (isUvcDevice(device)) {
                    showDeviceAlert(device);
                    usbMonitor.requestPermission(device);
                    uvcDeviceFound = true;
                    break;
                }
            }
            if (!uvcDeviceFound) {
                runOnUiThread(() -> tvDeviceStatus.setText("UVC устройство не найдено"));
                MyLogger.log("UVC устройство не найдено среди подключенных устройств.");
            }
        }
    }

    private void showDeviceAlert(UsbDevice device) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("UVC Камера Подключена")
                    .setMessage(String.format(
                            "Имя: %s\nVID: 0x%04X\nPID: 0x%04X",
                            device.getDeviceName(),
                            device.getVendorId(),
                            device.getProductId()))
                    .setPositiveButton("ОК", null)
                    .show();

            tvDeviceStatus.setText("Устройство подключено");
            btnRecord.setEnabled(true);
        });
    }

    private boolean isUvcDevice(UsbDevice device) {
        // Проверка по классам USB для UVC-совместимых устройств
        boolean isMiscUVC = device.getDeviceClass() == UsbConstants.USB_CLASS_MISC &&
                device.getDeviceSubclass() == 0x02 && // CDC Control
                device.getDeviceProtocol() == 0x01; // Video Interface Protocol

        boolean isVideoClass = device.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO;

        return isMiscUVC || isVideoClass;
    }

    private boolean checkPermissions() {
        String[] requiredPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
            };
        } else {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                MyLogger.log("Отсутствует разрешение: " + perm);
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        String[] permissionsToRequest;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
        } else {
            permissionsToRequest = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        ActivityCompat.requestPermissions(this,
                permissionsToRequest,
                REQUEST_PERMISSIONS);
    }

    private void initUSBCamera() {
        usbMonitor = new USBMonitor(this, new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(UsbDevice device) {
                MyLogger.log("Устройство подключено: " + device.getDeviceName());
                if (isUvcDevice(device)) {
                    showDeviceAlert(device);
                    usbMonitor.requestPermission(device);
                }
            }

            @Override
            public void onDetach(UsbDevice device) {
                MyLogger.log("Устройство отключено: " + device.getDeviceName());
                if (uvcCamera != null && uvcCamera.getDevice().equals(device)) {
                    closeCamera();
                    runOnUiThread(() -> {
                        tvDeviceStatus.setText("Отключено");
                        btnRecord.setEnabled(false);
                    });
                }
            }

            @Override
            public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                MyLogger.log("Устройство подключено: " + device.getDeviceName());
                openCamera(ctrlBlock);
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                MyLogger.log("Устройство отключено: " + device.getDeviceName());
                closeCamera();
                runOnUiThread(() -> {
                    tvDeviceStatus.setText("Отключено");
                    btnRecord.setEnabled(false);
                });
            }

            @Override
            public void onCancel(UsbDevice device) {
                MyLogger.log("Разрешение отклонено для устройства: " + device.getDeviceName());
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "USB разрешение отклонено", Toast.LENGTH_SHORT).show());
                tvDeviceStatus.setText("Разрешение отклонено");
            }
        });

        try {
            int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                    PendingIntent.FLAG_UPDATE_CURRENT;

            Intent intent = new Intent("com.serenegiant.USB_PERMISSION." + usbMonitor.hashCode());
            intent.setPackage(getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, flags);

            // Использование рефлексии для установки mPermissionIntent (старый способ для некоторых библиотек)
            Field field = USBMonitor.class.getDeclaredField("mPermissionIntent");
            field.setAccessible(true);
            field.set(usbMonitor, pi);

            usbMonitor.register();
            MyLogger.log("USBMonitor инициализирован и зарегистрирован");
        } catch (Exception e) {
            MyLogger.log("Ошибка инициализации USBMonitor: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Ошибка инициализации USBMonitor: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private final Object cameraLock = new Object();

    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        synchronized (cameraLock) {
            try {
                closeCamera();
                uvcCamera = new UVCCamera();
                uvcCamera.open(ctrlBlock);
                // Устанавливаем размер предпросмотра. Убедитесь, что камера поддерживает 640x480.
                uvcCamera.setPreviewSize(WIDTH, HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                MyLogger.log("Камера открыта успешно. Размер предпросмотра установлен на " + WIDTH + "x" + HEIGHT);

                // Установка FrameCallback для обработки кадров OpenCV
                uvcCamera.setFrameCallback(frame -> {
                    if (fpsCalculator != null) {
                        fpsCalculator.frameProcessed();
                    }
                    // Обрабатываем кадры только в режиме предпросмотра (не записи)
                    // и если предыдущий кадр уже обработан (флаг mIsFrameProcessing)
                    if (!isRecording && !mIsFrameProcessing) {
                        mIsFrameProcessing = true;
                        // Запускаем обработку в отдельном потоке, чтобы не блокировать UI и поток камеры
                        new Thread(() -> {
                            try {
                                processCameraFrame(frame);
                            } finally {
                                mIsFrameProcessing = false; // Сбрасываем флаг после завершения обработки
                            }
                        }).start();
                    }
                }, UVCCamera.PIXEL_FORMAT_YUV); // Используем UVCCamera.PIXEL_FORMAT_YUV (обычно это YUYV)

                tryStartPreview();
            } catch (Exception e) {
                MyLogger.log("Ошибка открытия камеры: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Ошибка камеры: " + e.getMessage(), Toast.LENGTH_LONG).show());
                closeCamera();
            }
        }
    }

    /**
     * Конвертирует YUYV ByteBuffer в RGBA Mat вручную.
     * Используется, когда стандартные Imgproc.cvtColor флаги для YUYV недоступны или работают некорректно.
     * @param yuyvFrameData Входной ByteBuffer с данными YUYV.
     * @param width Ширина кадра.
     * @param height Высота кадра.
     * @return Mat в формате RGBA (CV_8UC4).
     */
    private Mat yuyv422ToRgba(ByteBuffer yuyvFrameData, int width, int height) {
        Mat rgbaMat = new Mat(height, width, CvType.CV_8UC4);
        byte[] yuyvBytes = yuyvFrameData.array(); // Получаем массив байтов из ByteBuffer

        // Предполагаем формат YUYV: Y0 U0 Y1 V0 (4 байта на 2 пикселя)
        // YUYV data size = width * height * 2 bytes
        // RGBA data size = width * height * 4 bytes
        byte[] rgbaBytes = new byte[width * height * 4];

        // Итерируем по YUYV байтам, обрабатывая по 4 байта за раз (два пикселя)
        for (int i = 0, j = 0; i < yuyvBytes.length; i += 4, j += 8) {
            byte y0 = yuyvBytes[i];     // Y компонента для первого пикселя
            byte u0 = yuyvBytes[i + 1]; // U компонента для обоих пикселей
            byte y1 = yuyvBytes[i + 2]; // Y компонента для второго пикселя
            byte v0 = yuyvBytes[i + 3]; // V компонента для обоих пикселей

            // Конвертируем YUV в RGB для первого пикселя (Y0, U0, V0)
            int c = y0 & 0xFF; // Y
            int d = u0 & 0xFF; // U
            int e = v0 & 0xFF; // V

            // Стандартные формулы конвертации YUV в RGB (диапазон 0-255)
            int R0 = (int) (c + 1.402 * (e - 128));
            int G0 = (int) (c - 0.34414 * (d - 128) - 0.71414 * (e - 128));
            int B0 = (int) (c + 1.772 * (d - 128));

            // Записываем RGB и Alpha (255 для полной непрозрачности) в массив rgbaBytes
            // Порядок: R, G, B, A
            rgbaBytes[j] = (byte) Math.max(0, Math.min(255, R0));
            rgbaBytes[j + 1] = (byte) Math.max(0, Math.min(255, G0));
            rgbaBytes[j + 2] = (byte) Math.max(0, Math.min(255, B0));
            rgbaBytes[j + 3] = (byte) 255; // Альфа-канал

            // Конвертируем YUV в RGB для второго пикселя (Y1, U0, V0)
            c = y1 & 0xFF; // Y для второго пикселя
            // U0 и V0 остаются теми же, так как они используются для двух соседних пикселей в YUYV 4:2:2
            R0 = (int) (c + 1.402 * (e - 128));
            G0 = (int) (c - 0.34414 * (d - 128) - 0.71414 * (e - 128));
            B0 = (int) (c + 1.772 * (d - 128));

            rgbaBytes[j + 4] = (byte) Math.max(0, Math.min(255, R0));
            rgbaBytes[j + 5] = (byte) Math.max(0, Math.min(255, G0));
            rgbaBytes[j + 6] = (byte) Math.max(0, Math.min(255, B0));
            rgbaBytes[j + 7] = (byte) 255; // Альфа-канал
        }
        // Копируем сгенерированные RGBA байты в Mat
        rgbaMat.put(0, 0, rgbaBytes);
        return rgbaMat;
    }

    private void processCameraFrame(ByteBuffer frameData) {
        // Получаем RGBA Mat напрямую из YUYV данных, используя ручную конвертацию.
        // Этот метод создаёт новый Mat, который нужно освободить.
        Mat tempRgba = yuyv422ToRgba(frameData, WIDTH, HEIGHT);

        // Копируем содержимое из tempRgba в mRgba.
        // Это необходимо, чтобы mRgba всегда был актуальным для отображения и дальнейшей обработки.
        // Убедитесь, что mRgba инициализирован как CV_8UC4 в onCreate().
        if (!tempRgba.empty()) {
            tempRgba.copyTo(mRgba); // Копируем данные из временного Mat в постоянный mRgba
        } else {
            Log.e(TAG, "Ошибка: yuyv422ToRgba вернул пустой Mat. Не могу обработать кадр.");
            tempRgba.release(); // Освобождаем временный Mat
            return; // Прекращаем обработку текущего кадра
        }
        tempRgba.release(); // Освобождаем временный Mat сразу после копирования

        // Конвертируем mRgba (теперь правильный RGBA) в серый для распознавания кругов
        // Убедимся, что mRgba не пустой перед конвертацией в серый.
        if (!mRgba.empty()) {
            Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        } else {
            Log.e(TAG, "mRgba пустой после конвертации YUYV. Не могу преобразовать в серый.");
            return; // Прекращаем обработку
        }

        // Применяем Гауссово размытие для уменьшения шума (важно для HoughCircles)
        Imgproc.GaussianBlur(mGray, mGray, new org.opencv.core.Size(9, 9), 2, 2);

        // Матрица для хранения найденных кругов
        Mat circles = new Mat();

        // Параметры для HoughCircles
        // Эти параметры могут потребовать тонкой настройки для вашей конкретной среды
        // и характеристик кругов, которые вы хотите обнаружить.
        Imgproc.HoughCircles(mGray, circles, Imgproc.HOUGH_GRADIENT,
                1.0,    // dp: Inverse ratio of the accumulator resolution to the image resolution (1 = то же разрешение)
                (double)mGray.rows() / 8, // minDist: Минимальное расстояние между центрами обнаруженных кругов.
                // Чем меньше, тем больше кругов (возможно, ложных), и наоборот.
                100.0,  // param1: Верхний порог для внутреннего детектора Canny.
                // Используется для обнаружения краев.
                30.0,   // param2: Порог для центров кругов. Чем меньше, тем больше ложных кругов будет найдено.
                // Это самый важный параметр для настройки.
                0,      // minRadius: Минимальный радиус кругов, которые нужно обнаружить (0 = без ограничений)
                0       // maxRadius: Максимальный радиус кругов, которые нужно обнаружить (0 = без ограничений)
        );

        // Рисуем найденные круги на mRgba
        if (circles.cols() > 0) { // Проверяем, были ли найдены круги
            for (int x = 0; x < circles.cols(); x++) {
                double[] circle = circles.get(0, x); // Получаем данные о круге: [centerX, centerY, radius]
                if (circle != null) {
                    Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                    int radius = (int) Math.round(circle[2]);

                    // Рисуем круг (цвет: красный, толщина: 2) - в формате RGBA (R, G, B, A)
                    Imgproc.circle(mRgba, center, radius, new Scalar(255, 0, 0, 255), 2);
                    // Рисуем центр круга (цвет: зеленый, толщина: 3)
                    Imgproc.circle(mRgba, center, 3, new Scalar(0, 255, 0, 255), 3);
                }
            }
        }

        // Освобождаем circles Mat, чтобы избежать утечек памяти
        circles.release();

        // Конвертируем обработанный Mat (mRgba в формате RGBA) обратно в Bitmap для отображения
        Utils.matToBitmap(mRgba, mBitmap);

        // Обновляем TextureView на UI-потоке
        runOnUiThread(() -> {
            if (cameraPreviewTextureView != null && cameraPreviewTextureView.isAvailable()) {
                Canvas canvas = cameraPreviewTextureView.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(mBitmap, 0, 0, null);
                    cameraPreviewTextureView.unlockCanvasAndPost(canvas);
                }
            }
        });
    }

    private void closeCamera() {
        synchronized (cameraLock) {
            if (uvcCamera != null) {
                try {
                    uvcCamera.stopPreview();
                    uvcCamera.setFrameCallback(null, 0); // Важно: отключаем коллбэк кадров при закрытии
                    uvcCamera.destroy();
                    MyLogger.log("Камера закрыта.");
                } catch (Exception e) {
                    MyLogger.log("Ошибка закрытия камеры: " + e.getMessage());
                } finally {
                    uvcCamera = null;
                }
            }
        }
    }

    private void setupRecordButton() {
        btnRecord.setOnClickListener(v -> {
            if (uvcCamera == null) {
                Toast.makeText(this, "Камера не готова!", Toast.LENGTH_SHORT).show();
                MyLogger.log("Попытка записи, но uvcCamera null.");
                return;
            }
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });
    }

    private void startRecording() {
        synchronized (cameraLock) {
            if (isRecording || uvcCamera == null) {
                MyLogger.log("startRecording: Уже идет запись или камера null.");
                return;
            }

            if (fpsCalculator != null) {
                fpsCalculator.reset();
            }

            try {
                // Создаем директорию для сохранения видео
                File outputDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES), "USBCameraRecorder");
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw new IOException("Не удалось создать директорию: " + outputDir.getAbsolutePath());
                }

                File outputFile = new File(outputDir, "video_" + System.currentTimeMillis() + ".mp4");
                // Инициализируем и запускаем SimpleVideoRecorder
                videoRecorder = new SimpleVideoRecorder(); // Убедитесь, что этот класс корректно реализован
                videoRecorder.startRecording(outputFile, WIDTH, HEIGHT, new SimpleVideoRecorder.Callback() {
                    @Override
                    public void onSurfaceReady(Surface surface) {
                        runOnUiThread(() -> { // Переключаемся в UI поток для взаимодействия с UVCCamera
                            synchronized (cameraLock) {
                                if (uvcCamera == null) {
                                    MyLogger.log("onSurfaceReady: uvcCamera null, не могу начать запись.");
                                    return;
                                }
                                recordingSurface = surface;

                                // Останавливаем предпросмотр на TextureView и переключаем на поверхность MediaCodec
                                uvcCamera.stopPreview();
                                uvcCamera.setPreviewDisplay(recordingSurface);
                                uvcCamera.startPreview();

                                isRecording = true;
                                btnRecord.setText("Стоп");
                                Toast.makeText(context, "Запись начата: " + outputFile.getName(), Toast.LENGTH_LONG).show();
                                tvDeviceStatus.setText("Запись...");
                                MyLogger.log("Запись предпросмотра начата на поверхности MediaCodec.");
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Ошибка записи: " + message, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "SimpleVideoRecorder onError: " + message);
                            stopRecording();
                        });
                    }
                });
                MyLogger.log("SimpleVideoRecorder инициирован.");

            } catch (Exception e) {
                Log.e(TAG, "Ошибка настройки записи", e);
                handleRecordingError(e);
            }
        }
    }

    private void stopRecording() {
        synchronized (cameraLock) {
            if (!isRecording) {
                MyLogger.log("stopRecording: Запись не идет.");
                return;
            }

            if (videoRecorder != null) {
                videoRecorder.stopRecording();
                videoRecorder = null;
                MyLogger.log("VideoRecorder остановлен.");
            }

            isRecording = false;
            if (recordingSurface != null) {
                recordingSurface.release();
                recordingSurface = null;
                MyLogger.log("RecordingSurface освобожден.");
            }

            runOnUiThread(() -> {
                btnRecord.setText("Запись");
                tvDeviceStatus.setText("Устройство подключено");
                Toast.makeText(MainActivity.this, "Запись остановлена", Toast.LENGTH_SHORT).show();
            });

            if (fpsCalculator != null) {
                fpsCalculator.reset();
            }

            // Перезапускаем предварительный просмотр на TextureView
            if (uvcCamera != null) {
                try {
                    uvcCamera.stopPreview(); // Останавливаем текущий предпросмотр
                    if (cameraPreviewTextureView != null && cameraPreviewTextureView.isAvailable()) {
                        previewSurface = new Surface(cameraPreviewTextureView.getSurfaceTexture());
                        uvcCamera.setPreviewDisplay(previewSurface); // Устанавливаем обратно TextureView
                        uvcCamera.startPreview(); // Запускаем предпросмотр снова
                        MyLogger.log("Предпросмотр перезапущен на TextureView после записи.");
                    } else {
                        MyLogger.log("TextureView недоступен, не могу перезапустить предпросмотр после записи.");
                    }
                } catch (Exception e) {
                    MyLogger.log("Ошибка перезапуска предпросмотра после записи: " + e.getMessage());
                }
            }
        }
    }

    private void handleRecordingError(Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(context, "Ошибка записи: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "handleRecordingError: " + e.getMessage(), e);
        });
        stopRecording();
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
                MyLogger.log("Разрешения предоставлены");
                initUSBCamera();
                checkConnectedDevices();
            } else {
                MyLogger.log("Разрешения отклонены");
                runOnUiThread(() ->
                        Toast.makeText(this, "Разрешения отклонены. Невозможно управлять камерой.", Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (usbMonitor != null) {
            usbMonitor.register();
            MyLogger.log("USBMonitor зарегистрирован в onStart.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (usbMonitor != null) {
            usbMonitor.unregister();
            MyLogger.log("USBMonitor отменен в onStop.");
        }
        if (isRecording) {
            stopRecording();
        }
        closeCamera();
    }

    @Override
    protected void onDestroy() {
        if (isRecording) {
            stopRecording();
        }

        if (usbMonitor != null) {
            usbMonitor.destroy();
            MyLogger.log("USBMonitor уничтожен в onDestroy.");
        }

        closeCamera();

        if (previewSurface != null) {
            previewSurface.release();
            previewSurface = null;
            MyLogger.log("previewSurface освобожден в onDestroy.");
        }

        // Освобождение ресурсов OpenCV
        if (mRgba != null) mRgba.release();
        if (mGray != null) mGray.release();
        if (mBitmap != null) mBitmap.recycle(); // Важно для Bitmap

        super.onDestroy();
        MyLogger.log("Приложение уничтожено");
    }
}