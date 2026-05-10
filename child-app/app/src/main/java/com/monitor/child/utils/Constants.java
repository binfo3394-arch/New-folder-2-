package com.monitor.child.utils;

public class Constants {
    public static final String FIREBASE_CHILD_NODE = "child_devices";
    public static final String FIREBASE_COMMANDS_NODE = "commands";
    public static final String FIREBASE_STATUS_NODE = "status";

    public static final String COMMAND_SWITCH_CAMERA = "switch_camera";
    public static final String COMMAND_START_MONITORING = "start_monitoring";
    public static final String COMMAND_STOP_MONITORING = "stop_monitoring";

    public static final String CAMERA_FRONT = "front";
    public static final String CAMERA_BACK = "back";

    public static final String STORAGE_CAMERA_PATH = "camera_frames";
    public static final String STORAGE_AUDIO_PATH = "audio_chunks";
    public static final String STORAGE_LOCATION_PATH = "location";
    public static final String STORAGE_CALL_LOGS_PATH = "call_logs";
    public static final String STORAGE_MESSAGES_PATH = "messages";
    public static final String STORAGE_NOTIFICATIONS_PATH = "notifications";

    public static final long CAMERA_CAPTURE_INTERVAL_MS = 2000;
    public static final long AUDIO_CHUNK_DURATION_MS = 5000;
    public static final long LOCATION_UPDATE_INTERVAL_MS = 10000;

    public static final int NOTIFICATION_ID_CAMERA = 1001;
    public static final int NOTIFICATION_ID_AUDIO = 1002;
    public static final int NOTIFICATION_ID_LOCATION = 1003;
    public static final int NOTIFICATION_ID_CALL_LOGS = 1004;
    public static final int NOTIFICATION_ID_NOTIFICATIONS = 1005;
    public static final String NOTIFICATION_CHANNEL_CALL = "channel_call_logs";
    public static final String NOTIFICATION_CHANNEL_NOTIF = "channel_notifications";
}
