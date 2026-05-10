package com.monitor.child.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.monitor.child.ChildApp;
import com.monitor.child.MainActivity;
import com.monitor.child.R;
import com.monitor.child.manager.FirebaseManager;
import com.monitor.child.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioRecordService extends Service {
    private static final String TAG = "AudioRecordService";

    private final Object recorderLock = new Object();
    private MediaRecorder mediaRecorder;
    private ScheduledExecutorService scheduler;
    private File currentAudioFile;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.NOTIFICATION_ID_AUDIO, createNotification());
        startAudioCapture();
        return START_STICKY;
    }

    private void startAudioCapture() {
        if (isRecording) return;
        isRecording = true;

        scheduler.scheduleAtFixedRate(this::recordChunk, 0,
                Constants.AUDIO_CHUNK_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    private void recordChunk() {
        synchronized (recorderLock) {
            stopCurrentRecording();
            startNewRecording();
        }
    }

    private void startNewRecording() {
        try {
            File audioDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio_chunks");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            currentAudioFile = new File(audioDir, "audio_" + System.currentTimeMillis() + ".amr");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioSamplingRate(8000);
            mediaRecorder.setAudioEncodingBitRate(12200);
            mediaRecorder.setOutputFile(currentAudioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            Log.d(TAG, "Audio recording started: " + currentAudioFile.getName());

        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Error starting audio recording: " + e.getMessage());
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
        }
    }

    private void stopCurrentRecording() {
        if (mediaRecorder == null) return;
        try {
            mediaRecorder.stop();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recorder: " + e.getMessage());
        }
        try {
            mediaRecorder.release();
        } catch (Exception e) {
            Log.e(TAG, "Error releasing recorder: " + e.getMessage());
        }
        mediaRecorder = null;

        if (currentAudioFile != null && currentAudioFile.exists()) {
            FirebaseManager.getInstance().uploadAudioChunk(currentAudioFile);
        }
    }

    private android.app.Notification createNotification() {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, ChildApp.CHANNEL_AUDIO)
                .setContentTitle("Audio Monitor")
                .setContentText("Recording audio...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        isRecording = false;
        synchronized (recorderLock) {
            stopCurrentRecording();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
