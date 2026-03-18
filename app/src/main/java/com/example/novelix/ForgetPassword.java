package com.example.novelix;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.novelix.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgetPassword extends AppCompatActivity {
    private EditText emailEditText;
    private AppCompatButton sendVerificationButton;
    private TextView backToSignIn;
    private ImageView backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

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
        firestore = FirebaseFirestore.getInstance();
        emailEditText = findViewById(R.id.editTextEmail);
        sendVerificationButton = findViewById(R.id.btnSendVerification);
        backButton = findViewById(R.id.imageBack);
        backToSignIn = findViewById(R.id.backToSignIn);

        backButton.setOnClickListener(v -> onBackPressed());

        backToSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(ForgetPassword.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        sendVerificationButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (validateEmail(email)) {
                checkEmailAndSendResetLink(email);
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

    private void checkEmailAndSendResetLink(String email) {
        sendVerificationButton.setEnabled(false);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Checking email...");
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
        progressDialog.getWindow().setDimAmount(0.8f);
        progressDialog.show();

        // Check if email exists in Firestore 'users' collection
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Email exists in Firestore, proceed with password reset
                        sendPasswordResetEmail(email, progressDialog);
                    } else {
                        // Email not found in Firestore
                        progressDialog.dismiss();
                        sendVerificationButton.setEnabled(true);
                        emailEditText.setError("This email is not registered");
                        emailEditText.requestFocus();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    sendVerificationButton.setEnabled(true);
                    Toast.makeText(ForgetPassword.this,
                            "Failed to check email: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void sendPasswordResetEmail(String email, ProgressDialog progressDialog) {
        progressDialog.setMessage("Sending password reset email...");
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    sendVerificationButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgetPassword.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                        // Navigate back to LoginActivity
                        Intent intent = new Intent(ForgetPassword.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ForgetPassword.this,
                                "Failed to send reset email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}