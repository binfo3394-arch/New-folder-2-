package com.monitor.child.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.monitor.child.ChildApp;
import com.monitor.child.MainActivity;
import com.monitor.child.R;
import com.monitor.child.manager.FirebaseManager;
import com.monitor.child.utils.Constants;

public class CallLogMonitor extends Service {
    private static final String TAG = "CallLogMonitor";
    private ContentObserver callLogObserver;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.NOTIFICATION_ID_CALL_LOGS, createNotification());

        callLogObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                readLatestCallLog();
            }
        };

        getContentResolver().registerContentObserver(
                CallLog.Calls.CONTENT_URI, true, callLogObserver);

        readLatestCallLog();
        return START_STICKY;
    }

    private void readLatestCallLog() {
        try {
            String[] projection = {
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE
            };

            Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null, null,
                    CallLog.Calls.DATE + " DESC LIMIT 1");

            if (cursor != null && cursor.moveToFirst()) {
                String number = cursor.getString(0);
                int type = cursor.getInt(1);
                long duration = cursor.getLong(2);
                long date = cursor.getLong(3);

                String typeStr;
                switch (type) {
                    case CallLog.Calls.INCOMING_TYPE: typeStr = "incoming"; break;
                    case CallLog.Calls.OUTGOING_TYPE: typeStr = "outgoing"; break;
                    case CallLog.Calls.MISSED_TYPE: typeStr = "missed"; break;
                    default: typeStr = "unknown";
                }

                FirebaseManager.getInstance()
                        .uploadCallLog(number, typeStr, duration, date);

                Log.d(TAG, "Call log: " + number + " " + typeStr);
            }

            if (cursor != null) cursor.close();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: " + e.getMessage());
        }
    }

    private android.app.Notification createNotification() {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_CALL)
                .setContentTitle("Call Log Monitor")
                .setContentText("Monitoring call logs...")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        if (callLogObserver != null) {
            getContentResolver().unregisterContentObserver(callLogObserver);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
