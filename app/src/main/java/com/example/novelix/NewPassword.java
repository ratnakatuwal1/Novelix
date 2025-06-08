package com.example.novelix;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NewPassword extends AppCompatActivity {
    private TextInputEditText newPasswordEditText, confirmPasswordEditText;
    private MaterialButton submitButton;
    private TextView backToSignIn;
    private ImageView backButton;
    private FirebaseAuth mAuth;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        email = getIntent().getStringExtra("email");

        newPasswordEditText = findViewById(R.id.newPassword);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        submitButton = findViewById(R.id.submitButton);
        backToSignIn = findViewById(R.id.backToSignIn);
        backButton = findViewById(R.id.imageViewBack);

        // Submit button click listener
        submitButton.setOnClickListener(v -> {
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(NewPassword.this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(NewPassword.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 8) {
                Toast.makeText(NewPassword.this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

           updatePassword(newPassword);
        });

        // Back to Sign In click listener
        backToSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(NewPassword.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Back button click listener
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(NewPassword.this,
                                    "Password updated successfully",
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(NewPassword.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(NewPassword.this,
                                    "Failed to update password: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // If no user is logged in, prompt them to sign in
            Toast.makeText(this,
                    "Please sign in to update your password",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}