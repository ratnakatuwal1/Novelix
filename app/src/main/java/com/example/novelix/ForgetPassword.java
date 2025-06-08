package com.example.novelix;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgetPassword extends AppCompatActivity {
    private TextInputEditText emailEditText;
    private MaterialButton sendVerificationButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.enterEmail);
        sendVerificationButton = findViewById(R.id.sendVerificationButton);
        View backButton = findViewById(R.id.imageViewBack);
        View backToSignIn = findViewById(R.id.backToSignIn);

        backButton.setOnClickListener(v -> finish());

        // Back to Sign In click listener
        backToSignIn.setOnClickListener(v -> finish());

        // Send verification code button click listener
        sendVerificationButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (validateEmail(email)) {
                sendPasswordResetEmail(email);
                Toast.makeText(ForgetPassword.this, "Verification code sent to " + email, Toast.LENGTH_SHORT).show();
            } else {
                emailEditText.setError("Please enter a valid email address");
                emailEditText.requestFocus();
            }
        });
    }

    private boolean validateEmail(String email) {
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Email cannot be empty");
                return false;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Please enter a valid email address");
                return false;
            }

            // Additional check for common typos
            if (email.contains(" ")) {
                emailEditText.setError("Email should not contain spaces");
                return false;
            }

            if (!email.contains("@")) {
                emailEditText.setError("Missing @ symbol in email");
                return false;
            }

            return true;
        }

    private void sendPasswordResetEmail(String email){
        sendVerificationButton.setEnabled(false);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset email...");
        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    sendVerificationButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(ForgetPassword.this, "Password reset email sent to " + email, Toast.LENGTH_SHORT).show();
                        // Navigate to Verification activity
                        Intent intent = new Intent(ForgetPassword.this, Verification.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            emailEditText.setError("No account found with this email");
                            emailEditText.requestFocus();
                        } catch (FirebaseNetworkException e) {
                            Toast.makeText(ForgetPassword.this,
                                    "Network error. Please check your connection.",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(ForgetPassword.this,
                                    "Failed to send reset email: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}