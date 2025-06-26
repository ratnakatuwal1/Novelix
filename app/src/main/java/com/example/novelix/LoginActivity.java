package com.example.novelix;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText email_input, password_input;
    private TextView textViewRegister, textViewForgetPassword;
    private Button buttonLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private static final String PREFS_NAME = "NovelixPrefs";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final long SESSION_DURATION_SECONDS = 1500 * 3600L; // 1500 hours

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
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
            navigateToMainActivity();
        } else if (currentUser != null) {
            // Session expired, sign out user
            mAuth.signOut();
            clearLoginTimestamp();
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
        // Initialize all input fields
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewForgetPassword = findViewById(R.id.textViewForgetPassword);

        // Disable copy-paste for password field
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
                            saveLoginTimestamp(); // Save login time
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

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void saveLoginTimestamp() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis() / 1000);
        editor.apply();
    }

    private boolean isSessionValid() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis() / 1000;
        return loginTimestamp > 0 && (currentTime - loginTimestamp) < SESSION_DURATION_SECONDS;
    }

    private void clearLoginTimestamp() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_LOGIN_TIMESTAMP);
        editor.apply();
    }
}