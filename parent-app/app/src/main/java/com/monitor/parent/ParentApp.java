package com.monitor.parent;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class ParentApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initFirebase();
    }

    private void initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:792529827830:android:0e5d3c30731fd172b079d7")
                    .setApiKey("AIzaSyA1Jc_WVmTMzOEqRXThTm4oJEXzqhvpsiM")
                    .setProjectId("sanchat-611e1")
                    .setStorageBucket("sanchat-611e1.firebasestorage.app")
                    .setGcmSenderId("792529827830")
                    .setDatabaseUrl("https://sanchat-611e1-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }
    }
}
