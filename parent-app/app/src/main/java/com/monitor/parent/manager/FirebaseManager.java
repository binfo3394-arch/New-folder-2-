package com.monitor.parent.manager;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.monitor.parent.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager_Parent";
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private String currentEmail;
    private ValueEventListener childStatusListener;

    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public String getCurrentEmail() {
        return currentEmail;
    }

    public void setCurrentEmail(String email) {
        this.currentEmail = email;
    }

    public String getSanitizedEmail() {
        if (currentEmail == null) return "unknown";
        return currentEmail.replace(".", "_").replace("@", "_at_");
    }

    public Task<FirebaseUser> signInWithEmail(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        currentEmail = email;
                    }
                    return mAuth.getCurrentUser();
                });
    }

    public Task<FirebaseUser> createAccount(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        currentEmail = email;
                    }
                    return mAuth.getCurrentUser();
                });
    }

    public void sendCommand(String command) {
        String sanitized = getSanitizedEmail();
        DatabaseReference cmdRef = mDatabase.getReference(Constants.FIREBASE_COMMANDS_NODE)
                .child(sanitized);

        String cmdId = cmdRef.push().getKey();
        if (cmdId != null) {
            cmdRef.child(cmdId).setValue(command);
            Log.d(TAG, "Command sent: " + command);
        }
    }

    public void getLatestCameraFrameUrl(final OnFrameUrlListener listener) {
        String sanitized = getSanitizedEmail();
        StorageReference ref = mStorage.getReference()
                .child(Constants.STORAGE_CAMERA_PATH)
                .child(sanitized);

        ref.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> items = listResult.getItems();
            if (items.isEmpty()) {
                listener.onUrl(null);
                return;
            }

            StorageReference latest = items.get(items.size() - 1);
            latest.getDownloadUrl().addOnSuccessListener(uri -> {
                listener.onUrl(uri.toString());
            }).addOnFailureListener(e -> {
                listener.onUrl(null);
            });
        }).addOnFailureListener(e -> {
            listener.onUrl(null);
        });
    }

    public void getLatestAudioUrl(final OnAudioUrlListener listener) {
        String sanitized = getSanitizedEmail();
        StorageReference ref = mStorage.getReference()
                .child(Constants.STORAGE_AUDIO_PATH)
                .child(sanitized);

        ref.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> items = listResult.getItems();
            if (items.isEmpty()) {
                listener.onUrl(null);
                return;
            }

            StorageReference latest = items.get(items.size() - 1);
            latest.getDownloadUrl().addOnSuccessListener(uri -> {
                listener.onUrl(uri.toString());
            }).addOnFailureListener(e -> {
                listener.onUrl(null);
            });
        }).addOnFailureListener(e -> {
            listener.onUrl(null);
        });
    }

    public void getChildLocation(final OnLocationListener listener) {
        String sanitized = getSanitizedEmail();
        DatabaseReference locRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_LOCATION_PATH);

        locRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("lat").getValue(Double.class);
                Double lng = snapshot.child("lng").getValue(Double.class);
                listener.onLocation(lat, lng);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onLocation(null, null);
            }
        });
    }

    public void getChildStatus(final OnStatusListener listener) {
        if (childStatusListener != null) {
            String sanitized = getSanitizedEmail();
            mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                    .child(sanitized)
                    .removeEventListener(childStatusListener);
            childStatusListener = null;
        }

        String sanitized = getSanitizedEmail();
        DatabaseReference statusRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized);

        childStatusListener = statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                String cameraMode = snapshot.child("cameraMode").getValue(String.class);
                listener.onStatus(status, cameraMode);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onStatus(null, null);
            }
        });
    }

    public void listenCallLogs(final OnCallLogsListener listener) {
        String sanitized = getSanitizedEmail();
        DatabaseReference ref = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_CALL_LOGS_PATH);

        ref.orderByKey().limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, Object>> logs = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> entry = (Map<String, Object>) child.getValue();
                    if (entry != null) logs.add(entry);
                }
                listener.onCallLogs(logs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCallLogs(null);
            }
        });
    }

    public void listenMessages(final OnMessagesListener listener) {
        String sanitized = getSanitizedEmail();
        DatabaseReference ref = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_MESSAGES_PATH);

        ref.orderByKey().limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, Object>> msgs = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> entry = (Map<String, Object>) child.getValue();
                    if (entry != null) msgs.add(entry);
                }
                listener.onMessages(msgs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onMessages(null);
            }
        });
    }

    public void listenNotifications(final OnNotificationsListener listener) {
        String sanitized = getSanitizedEmail();
        DatabaseReference ref = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_NOTIFICATIONS_PATH);

        ref.orderByKey().limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, Object>> notifs = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> entry = (Map<String, Object>) child.getValue();
                    if (entry != null) notifs.add(entry);
                }
                listener.onNotifications(notifs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onNotifications(null);
            }
        });
    }

    public void savePairingCode(String code) {
        if (currentEmail == null) return;
        DatabaseReference ref = mDatabase.getReference(Constants.FIREBASE_PAIRINGS_NODE)
                .child(code);
        Map<String, Object> data = new HashMap<>();
        data.put("email", currentEmail);
        data.put("_createdAt", System.currentTimeMillis());
        ref.setValue(data);
    }

    public void removePairingCode(String code) {
        mDatabase.getReference(Constants.FIREBASE_PAIRINGS_NODE)
                .child(code)
                .removeValue();
    }

    public void cleanup() {
        if (childStatusListener != null) {
            String sanitized = getSanitizedEmail();
            DatabaseReference statusRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                    .child(sanitized);
            statusRef.removeEventListener(childStatusListener);
            childStatusListener = null;
        }
    }

    public interface OnFrameUrlListener {
        void onUrl(String url);
    }

    public interface OnAudioUrlListener {
        void onUrl(String url);
    }

    public interface OnLocationListener {
        void onLocation(Double lat, Double lng);
    }

    public interface OnStatusListener {
        void onStatus(String status, String cameraMode);
    }

    public interface OnCallLogsListener {
        void onCallLogs(List<Map<String, Object>> logs);
    }

    public interface OnMessagesListener {
        void onMessages(List<Map<String, Object>> messages);
    }

    public interface OnNotificationsListener {
        void onNotifications(List<Map<String, Object>> notifications);
    }
}
