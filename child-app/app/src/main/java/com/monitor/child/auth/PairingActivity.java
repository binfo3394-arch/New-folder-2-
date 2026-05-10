package com.monitor.child.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monitor.child.MainActivity;
import com.monitor.child.R;
import com.monitor.child.utils.Constants;

public class PairingActivity extends AppCompatActivity {
    private EditText etCode;
    private Button btnPair;
    private ProgressBar progressBar;
    private boolean authReady = false;
    private boolean isPairing = false;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        String pairedEmail = prefs.getString(Constants.PREF_PAIRED_EMAIL, null);
        if (pairedEmail != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_pairing);

        etCode = findViewById(R.id.et_pairing_code);
        btnPair = findViewById(R.id.btn_pair);
        progressBar = findViewById(R.id.progress_bar);

        authStateListener = auth -> {
            if (auth.getCurrentUser() != null) {
                authReady = true;
                btnPair.setEnabled(true);
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            authReady = true;
            btnPair.setEnabled(true);
        } else {
            btnPair.setEnabled(false);
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
        }

        btnPair.setOnClickListener(v -> attemptPair());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
            authStateListener = null;
        }
    }

    private void attemptPair() {
        if (isPairing) return;
        if (!authReady) {
            Toast.makeText(this, "Still connecting. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        String code = etCode.getText().toString().trim();
        if (TextUtils.isEmpty(code) || code.length() != 6) {
            Toast.makeText(this, "Enter a valid 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        isPairing = true;
        showProgress(true);

        DatabaseReference pairRef = FirebaseDatabase.getInstance()
                .getReference(Constants.FIREBASE_PAIRINGS_NODE)
                .child(code);

        pairRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isPairing = false;
                showProgress(false);
                String parentEmail = snapshot.child("email").getValue(String.class);
                if (parentEmail != null) {
                    SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
                    prefs.edit().putString(Constants.PREF_PAIRED_EMAIL, parentEmail).apply();
                    Toast.makeText(PairingActivity.this,
                            "Paired with " + parentEmail, Toast.LENGTH_LONG).show();
                    goToMain();
                } else {
                    Toast.makeText(PairingActivity.this,
                            "Invalid code. Check the Parent app.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isPairing = false;
                showProgress(false);
                Toast.makeText(PairingActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPair.setEnabled(!show);
        etCode.setEnabled(!show);
    }

    private void goToMain() {
        if (isFinishing()) return;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
