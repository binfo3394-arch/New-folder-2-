package com.monitor.parent.fragments;

import android.media.MediaPlayer;
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

import java.io.IOException;

public class AudioPlayerFragment extends Fragment {
    private Button btnPlay, btnStop;
    private TextView tvAudioStatus;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable pollRunnable;
    private boolean isPolling = false;
    private String lastPlayedUrl = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_player, container, false);

        btnPlay = view.findViewById(R.id.btn_play);
        btnStop = view.findViewById(R.id.btn_stop);
        tvAudioStatus = view.findViewById(R.id.tv_audio_status);

        mediaPlayer = new MediaPlayer();

        btnPlay.setOnClickListener(v -> playLatestAudio());
        btnStop.setOnClickListener(v -> stopAudio());

        handler = new Handler();
        startPolling();

        return view;
    }

    private void startPolling() {
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling) return;
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    checkForNewAudio();
                }
                handler.postDelayed(this, Constants.POLL_INTERVAL_MS);
            }
        };
        handler.post(pollRunnable);
    }

    private void checkForNewAudio() {
        FirebaseManager.getInstance().getLatestAudioUrl(url -> {
            if (!isAdded()) return;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (url != null && !url.equals(lastPlayedUrl)) {
                        lastPlayedUrl = url;
                        playAudio(url);
                    }
                });
            }
        });
    }

    private void playLatestAudio() {
        FirebaseManager.getInstance().getLatestAudioUrl(url -> {
            if (!isAdded()) return;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (url != null) {
                        lastPlayedUrl = url;
                        playAudio(url);
                    } else if (tvAudioStatus != null) {
                        tvAudioStatus.setText("No audio available");
                    }
                });
            }
        });
    }

    private void playAudio(String url) {
        if (tvAudioStatus == null) return;
        try {
            stopAudio();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (!isAdded()) return;
                mp.start();
                if (tvAudioStatus != null) tvAudioStatus.setText("Playing...");
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (tvAudioStatus != null) tvAudioStatus.setText("Completed");
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                if (tvAudioStatus != null) tvAudioStatus.setText("Error playing audio");
                return true;
            });
        } catch (IOException e) {
            tvAudioStatus.setText("Error: " + e.getMessage());
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer = new MediaPlayer();
            if (tvAudioStatus != null) tvAudioStatus.setText("Stopped");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isPolling = false;
        if (handler != null) {
            handler.removeCallbacks(pollRunnable);
        }
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }
}
