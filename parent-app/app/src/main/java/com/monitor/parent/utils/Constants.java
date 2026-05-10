package com.monitor.parent.utils;

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

    public static final long POLL_INTERVAL_MS = 3000;
}
