package com.monitor.parent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.monitor.parent.adapters.MonitorPagerAdapter;
import com.monitor.parent.manager.FirebaseManager;
import com.monitor.parent.utils.Constants;

import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView tvChildEmail, tvStatus, tvPairingCode;
    private TextView tvDeviceModel, tvDeviceAndroid, tvLastSeen;
    private Button btnNewCode;
    private MonitorPagerAdapter pagerAdapter;
    private String currentPairingCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = FirebaseManager.getInstance();

        tvChildEmail = findViewById(R.id.tv_child_email);
        tvStatus = findViewById(R.id.tv_status);
        tvPairingCode = findViewById(R.id.tv_pairing_code);
        tvDeviceModel = findViewById(R.id.tv_device_model);
        tvDeviceAndroid = findViewById(R.id.tv_device_android);
        tvLastSeen = findViewById(R.id.tv_last_seen);
        btnNewCode = findViewById(R.id.btn_new_code);
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        String email = firebaseManager.getCurrentEmail();
        if (TextUtils.isEmpty(email)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                email = user.getEmail();
                firebaseManager.setCurrentEmail(email);
            }
        }
        tvChildEmail.setText("Parent email: " + (email != null ? email : "unknown"));

        generateAndShowPairingCode();

        btnNewCode.setOnClickListener(v -> generateAndShowPairingCode());

        loadDeviceInfo();

        pagerAdapter = new MonitorPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Camera"); break;
                case 1: tab.setText("Audio"); break;
                case 2: tab.setText("Location"); break;
                case 3: tab.setText("Call Logs"); break;
                case 4: tab.setText("Messages"); break;
                case 5: tab.setText("Notifications"); break;
            }
        }).attach();

        FirebaseManager.getInstance().getChildStatus((status, cameraMode) -> {
            runOnUiThread(() -> {
                String s = "Status: " + (status != null ? status : "unknown");
                if (cameraMode != null) {
                    s += " | Camera: " + cameraMode;
                }
                tvStatus.setText(s);
            });
        });
    }

    private void loadDeviceInfo() {
        FirebaseManager.getInstance().getDeviceInfo(info -> {
            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(() -> {
                if (info == null) return;
                String model = (String) info.get("model");
                String manufacturer = (String) info.get("manufacturer");
                String androidVer = (String) info.get("androidVersion");
                Long lastSeen = (Long) info.get("lastSeen");

                if (tvDeviceModel == null) return;
                if (model != null && manufacturer != null) {
                    tvDeviceModel.setText("Device: " + manufacturer + " " + model);
                } else if (model != null) {
                    tvDeviceModel.setText("Device: " + model);
                }
                if (androidVer != null && tvDeviceAndroid != null) {
                    tvDeviceAndroid.setText("Android: " + androidVer);
                }
                if (lastSeen != null && tvLastSeen != null) {
                    java.text.SimpleDateFormat sdf =
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                    java.util.Locale.getDefault());
                    tvLastSeen.setText("Last seen: " + sdf.format(new java.util.Date(lastSeen)));
                }
            });
        });
    }

    private void generateAndShowPairingCode() {
        if (currentPairingCode != null) {
            firebaseManager.removePairingCode(currentPairingCode);
        }
        currentPairingCode = String.format("%06d", new Random().nextInt(1000000));
        firebaseManager.savePairingCode(currentPairingCode);
        tvPairingCode.setText(currentPairingCode);

        SharedPreferences prefs = getSharedPreferences("parent_prefs", MODE_PRIVATE);
        prefs.edit().putString(Constants.PREF_PAIRING_CODE, currentPairingCode).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseManager.cleanup();
    }
}
