package com.example.novelix;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePassword extends AppCompatActivity {
    private ImageView imageBack;
    private EditText editTextCurrentPassword;
    private TextView textViewForgetPassword;
    private EditText editTextNewPassword;
    private EditText editTextConfirmNewPassword;
    private TextView btnChangePassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imageBack = findViewById(R.id.imageBack);
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword);
        textViewForgetPassword = findViewById(R.id.textViewForgetPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmNewPassword = findViewById(R.id.editTextConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        imageBack.setOnClickListener(v -> finish());

        editTextCurrentPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editTextCurrentPassword.getRight() - editTextCurrentPassword.getCompoundDrawables()[2].getBounds().width())) {
                    isCurrentPasswordVisible = !isCurrentPasswordVisible;
                    togglePasswordVisibility(editTextCurrentPassword, isCurrentPasswordVisible);
                    return true;
                }
            }
            return false;
        });

        editTextNewPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editTextNewPassword.getRight() - editTextNewPassword.getCompoundDrawables()[2].getBounds().width())) {
                    isNewPasswordVisible = !isNewPasswordVisible;
                    togglePasswordVisibility(editTextNewPassword, isNewPasswordVisible);
                    return true;
                }
            }
            return false;
        });

        editTextConfirmNewPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editTextConfirmNewPassword.getRight() - editTextConfirmNewPassword.getCompoundDrawables()[2].getBounds().width())) {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible;
                    togglePasswordVisibility(editTextConfirmNewPassword, isConfirmPasswordVisible);
                    return true;
                }
            }
            return false;
        });

        // Change password button click listener
        btnChangePassword.setOnClickListener(v -> changePassword());

        textViewForgetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(ChangePassword.this, ForgetPassword.class);
            startActivity(intent);
            finish();
        });
    }

    private void togglePasswordVisibility(EditText editText, boolean isVisible) {
        if (isVisible) {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye, 0);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eyeoff, 0);
        }
        editText.setSelection(editText.getText().length()); // Move cursor to end
    }

    private void changePassword() {
        String currentPassword = editTextCurrentPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmNewPassword = editTextConfirmNewPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(currentPassword)) {
            editTextCurrentPassword.setError("Enter current password");
            editTextCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            editTextNewPassword.setError("Enter new password");
            editTextNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            editTextNewPassword.setError("Password must be at least 6 characters");
            editTextNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            editTextConfirmNewPassword.setError("Passwords do not match");
            editTextConfirmNewPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Reauthenticate user
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        // Update password in Firebase Authentication
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid1 -> {
                                    // Update password in Firestore
                                    db.collection("users").document(user.getUid())
                                            .update("password", newPassword)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(ChangePassword.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(ChangePassword.this, "Error updating Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ChangePassword.this, "Error updating password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChangePassword.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}