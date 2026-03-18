package com.example.novelix;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText email_input, password_input;
    private TextView textViewRegister, textViewForgetPassword;
    private Button buttonLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Add FirebaseFirestore instance
    private ProgressDialog progressDialog;
    private static final String OLD_GLOBAL_PREFS = "NovelixPrefs";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final long SESSION_DURATION_SECONDS = 1500 * 3600L; // 1500 hours

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        setupProgressDialog();
        initializeViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in and session is valid
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && isSessionValid()) {
            updateLastActivityTimestamp(currentUser.getUid());
            clearOldGlobalPrefsAndMigrate(); // Critical fix
            navigateToMainActivity();
        } else if (currentUser != null) {
            mAuth.signOut();
            clearLoginTimestamp();
            clearUserSpecificPrefs(currentUser.getUid()); // Extra safety
            Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private void initializeViews() {
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewForgetPassword = findViewById(R.id.textViewForgetPassword);

        password_input.setTextIsSelectable(false);
        password_input.setCustomSelectionActionModeCallback(null);
    }

    private void setupListeners() {
        buttonLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUser();
            }
        });

        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        textViewForgetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgetPassword.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean validateInputs() {
        String email = email_input.getText().toString().trim();
        String password = password_input.getText().toString().trim();

        if (email.isEmpty()) {
            email_input.setError("Email is required");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_input.setError("Please enter a valid email");
            return false;
        }

        if (password.isEmpty()) {
            password_input.setError("Password is required");
            return false;
        } else if (password.length() < 6) {
            password_input.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void loginUser() {
        progressDialog.show();
        String email = email_input.getText().toString().trim();
        String password = password_input.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveLoginTimestamp(); // Save login time to SharedPreferences
                            updateLastActivityTimestamp(user.getUid()); // Update timestamp in Firestore
                            progressDialog.dismiss();
                            navigateToMainActivity();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this,
                                    "Login failed: User not found.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progressDialog.dismiss();
                        String errorMessage = "Login failed.";
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "No account found with this email.";
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid password.";
                        } else if (exception instanceof FirebaseNetworkException) {
                            errorMessage = "Network error. Please check your internet connection.";
                        } else if (exception != null && exception.getMessage() != null) {
                            errorMessage = "Error: " + exception.getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearOldGlobalPrefsAndMigrate() {
        // Clear old global SharedPreferences (used by old buggy version)
        SharedPreferences oldPrefs = getSharedPreferences(OLD_GLOBAL_PREFS, MODE_PRIVATE);
        oldPrefs.edit().clear().apply();

        // Optional: Migrate old data to new user-specific prefs (if needed)
        // For now, we just clear it — safer for privacy
    }

    public static void clearUserSpecificPrefs(String userId) {
        // This method can be called from anywhere (e.g., Logout button)
        SharedPreferences prefs = getInstance().getSharedPreferences("NovelixPrefs_" + userId, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private static LoginActivity instance;
    public static LoginActivity getInstance() { return instance; }
    @Override protected void onResume() { super.onResume(); instance = this; }

    // New method to update lastActivityTimestamp in Firestore
    private void updateLastActivityTimestamp(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastActivityTimestamp", System.currentTimeMillis());

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Log or handle success if needed
                    // Log.d("LoginActivity", "lastActivityTimestamp updated for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    // Log or handle failure if needed
                    // Log.e("LoginActivity", "Failed to update lastActivityTimestamp for user: " + userId, e);
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void saveLoginTimestamp() {
        SharedPreferences prefs = getSharedPreferences(OLD_GLOBAL_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis() / 1000); // Store in seconds
        editor.apply();
    }

    private boolean isSessionValid() {
        SharedPreferences prefs = getSharedPreferences(OLD_GLOBAL_PREFS, MODE_PRIVATE);
        long loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0); // Retrieved in seconds
        long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
        return loginTimestamp > 0 && (currentTime - loginTimestamp) < SESSION_DURATION_SECONDS;
    }

    private void clearLoginTimestamp() {
        SharedPreferences prefs = getSharedPreferences(OLD_GLOBAL_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_LOGIN_TIMESTAMP);
        editor.apply();
    }

    public static void logout(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            SharedPreferences userPrefs = context.getSharedPreferences("NovelixPrefs_" + userId, Context.MODE_PRIVATE);
            userPrefs.edit().clear().apply(); // Clear Continue Reading + Weekly Goal
        }

        // Clear session
        SharedPreferences oldPrefs = context.getSharedPreferences("NovelixPrefs", Context.MODE_PRIVATE);
        oldPrefs.edit().clear().apply();

        FirebaseAuth.getInstance().signOut();

        // Go to Login
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}