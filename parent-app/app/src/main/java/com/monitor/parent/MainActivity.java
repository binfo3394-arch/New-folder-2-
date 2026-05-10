package com.monitor.parent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.monitor.parent.adapters.MonitorPagerAdapter;
import com.monitor.parent.manager.FirebaseManager;

public class MainActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView tvChildEmail, tvStatus;
    private MonitorPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = FirebaseManager.getInstance();

        tvChildEmail = findViewById(R.id.tv_child_email);
        tvStatus = findViewById(R.id.tv_status);
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        tvChildEmail.setText("Child: " + firebaseManager.getCurrentEmail());

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseManager.cleanup();
    }
}
