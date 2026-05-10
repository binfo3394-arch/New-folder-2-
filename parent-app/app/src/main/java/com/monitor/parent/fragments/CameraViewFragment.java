package com.monitor.parent.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.monitor.parent.R;
import com.monitor.parent.manager.FirebaseManager;
import com.monitor.parent.utils.Constants;

public class CameraViewFragment extends Fragment {
    private ImageView ivCameraFeed;
    private Button btnSwitchCamera, btnRefresh;
    private TextView tvCameraStatus;
    private Handler handler;
    private Runnable pollRunnable;
    private boolean isPolling = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_view, container, false);

        ivCameraFeed = view.findViewById(R.id.iv_camera_feed);
        btnSwitchCamera = view.findViewById(R.id.btn_switch_camera);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvCameraStatus = view.findViewById(R.id.tv_camera_status);

        btnSwitchCamera.setOnClickListener(v -> {
            FirebaseManager.getInstance().sendCommand(Constants.COMMAND_SWITCH_CAMERA);
            tvCameraStatus.setText("Switching camera...");
        });

        btnRefresh.setOnClickListener(v -> refreshFrame());

        handler = new Handler();
        startPolling();

        return view;
    }

    private void startPolling() {
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    refreshFrame();
                    handler.postDelayed(this, Constants.POLL_INTERVAL_MS);
                }
            }
        };
        handler.post(pollRunnable);
    }

    private void refreshFrame() {
        FirebaseManager.getInstance().getLatestCameraFrameUrl(url -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (url != null) {
                        Glide.with(CameraViewFragment.this)
                                .load(url)
                                .placeholder(R.drawable.placeholder)
                                .error(R.drawable.placeholder)
                                .into(ivCameraFeed);
                        tvCameraStatus.setText("Live");
                    } else {
                        tvCameraStatus.setText("Waiting for feed...");
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isPolling = false;
        if (handler != null) {
            handler.removeCallbacks(pollRunnable);
        }
    }
}
