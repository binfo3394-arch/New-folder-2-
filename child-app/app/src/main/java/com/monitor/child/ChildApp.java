package com.monitor.child;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.monitor.child.utils.Constants;

public class ChildApp extends Application {
    public static final String CHANNEL_CAMERA = "channel_camera";
    public static final String CHANNEL_AUDIO = "channel_audio";
    public static final String CHANNEL_LOCATION = "channel_location";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
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
