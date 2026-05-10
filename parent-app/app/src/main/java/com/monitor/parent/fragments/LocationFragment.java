package com.monitor.parent.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.monitor.parent.R;
import com.monitor.parent.manager.FirebaseManager;
import com.monitor.parent.utils.Constants;

public class LocationFragment extends Fragment {
    private TextView tvLatitude, tvLongitude, tvLocationStatus;
    private Button btnRefreshLocation;
    private Handler handler;
    private Runnable pollRunnable;
    private boolean isPolling = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location, container, false);

        tvLatitude = view.findViewById(R.id.tv_latitude);
        tvLongitude = view.findViewById(R.id.tv_longitude);
        tvLocationStatus = view.findViewById(R.id.tv_location_status);
        btnRefreshLocation = view.findViewById(R.id.btn_refresh_location);

        btnRefreshLocation.setOnClickListener(v -> refreshLocation());

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
                    refreshLocation();
                    handler.postDelayed(this, Constants.POLL_INTERVAL_MS);
                }
            }
        };
        handler.post(pollRunnable);
    }

    private void refreshLocation() {
        FirebaseManager.getInstance().getChildLocation((lat, lng) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (lat != null && lng != null) {
                        tvLatitude.setText("Latitude: " + String.format("%.6f", lat));
                        tvLongitude.setText("Longitude: " + String.format("%.6f", lng));
                        tvLocationStatus.setText("Last updated: " +
                                java.text.DateFormat.getDateTimeInstance()
                                        .format(new java.util.Date()));

                        String mapsUri = "https://maps.google.com/maps?q=" + lat + "," + lng;
                        tvLocationStatus.setOnClickListener(v -> {
                            android.content.Intent intent =
                                    new android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(mapsUri));
                            startActivity(intent);
                        });
                    } else {
                        tvLocationStatus.setText("Waiting for location data...");
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
