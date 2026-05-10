package com.monitor.child;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.monitor.child.manager.FirebaseManager;
import com.monitor.child.service.AudioRecordService;
import com.monitor.child.service.CallLogMonitor;
import com.monitor.child.service.CameraCaptureService;
import com.monitor.child.service.LocationService;
import com.monitor.child.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private FirebaseManager firebaseManager;
    private Button btnStartMonitor, btnStopMonitor, btnSwitchCamera;
    private Button btnEnableNotifAccess;
    private TextView tvStatus, tvCameraMode, tvEmail;

    private final String[] REQUIRED_PERMISSIONS;
    {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        perms.add(Manifest.permission.READ_CALL_LOG);
        perms.add(Manifest.permission.POST_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        REQUIRED_PERMISSIONS = perms.toArray(new String[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = FirebaseManager.getInstance();

        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        String pairedEmail = prefs.getString(Constants.PREF_PAIRED_EMAIL, null);
        if (pairedEmail != null) {
            firebaseManager.setCurrentEmail(pairedEmail);
        }

        btnStartMonitor = findViewById(R.id.btn_start_monitor);
        btnStopMonitor = findViewById(R.id.btn_stop_monitor);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnEnableNotifAccess = findViewById(R.id.btn_enable_notif_access);
        tvStatus = findViewById(R.id.tv_status);
        tvCameraMode = findViewById(R.id.tv_camera_mode);
        tvEmail = findViewById(R.id.tv_email);

        tvEmail.setText("Paired as: " + firebaseManager.getCurrentEmail());
        tvCameraMode.setText("Camera: " + Constants.CAMERA_FRONT);
        tvStatus.setText("Status: Stopped");

        firebaseManager.setOnCommandListener(command -> {
            runOnUiThread(() -> {
                switch (command) {
                    case Constants.COMMAND_SWITCH_CAMERA:
                        toggleCamera();
                        break;
                    case Constants.COMMAND_START_MONITORING:
                        startAllServices();
                        break;
                    case Constants.COMMAND_STOP_MONITORING:
                        stopAllServices();
                        break;
                }
            });
        });

        btnStartMonitor.setOnClickListener(v -> startAllServices());
        btnStopMonitor.setOnClickListener(v -> stopAllServices());
        btnSwitchCamera.setOnClickListener(v -> toggleCamera());
        btnEnableNotifAccess.setOnClickListener(v -> openNotifAccessSettings());

        requestAllPermissions();
    }

    private void requestAllPermissions() {
        List<String> missing = new ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }

        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missing.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "All permissions must be granted for full functionality",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startAllServices() {
        startService(new Intent(this, CameraCaptureService.class));
        startService(new Intent(this, AudioRecordService.class));
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, CallLogMonitor.class));

        firebaseManager.updateStatus("monitoring");
        tvStatus.setText("Status: Monitoring Active");
        Toast.makeText(this, "All monitoring services started", Toast.LENGTH_SHORT).show();
    }

    private void stopAllServices() {
        stopService(new Intent(this, CameraCaptureService.class));
        stopService(new Intent(this, AudioRecordService.class));
        stopService(new Intent(this, LocationService.class));
        stopService(new Intent(this, CallLogMonitor.class));

        firebaseManager.updateStatus("stopped");
        tvStatus.setText("Status: Stopped");
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
    }

    private String currentCamera = Constants.CAMERA_FRONT;

    private void toggleCamera() {
        if (currentCamera.equals(Constants.CAMERA_FRONT)) {
            currentCamera = Constants.CAMERA_BACK;
        } else {
            currentCamera = Constants.CAMERA_FRONT;
        }
        firebaseManager.updateCameraMode(currentCamera);
        tvCameraMode.setText("Camera: " + currentCamera);
        Toast.makeText(this, "Switched to " + currentCamera + " camera", Toast.LENGTH_SHORT).show();
    }

    private void openNotifAccessSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private boolean isNotificationAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

