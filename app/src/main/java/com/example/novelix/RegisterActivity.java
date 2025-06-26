package com.example.novelix;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText nameInput, addressInput, contactInput, emailInput, passwordInput, confirmPasswordInput;
    private TextInputLayout passwordInputLayout, confirmPasswordInputLayout;
    private TextView textViewLogin;
    private AppCompatButton buttonRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Z][a-z]*(\\s[A-Z][a-z]*)+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s,.#-]+$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        setupProgressDialog();
        initializeViews();
        setupListeners();
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private void initializeViews() {
        // Initialize all input fields
        nameInput = findViewById(R.id.name_input);
        addressInput = findViewById(R.id.address_input);
        contactInput = findViewById(R.id.contact_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        confirmPasswordInputLayout = findViewById(R.id.confirm_password_input_layout);

        // Disable copy-paste for password fields
        passwordInput.setTextIsSelectable(false);
        passwordInput.setCustomSelectionActionModeCallback(null);
        confirmPasswordInput.setTextIsSelectable(false);
        confirmPasswordInput.setCustomSelectionActionModeCallback(null);
    }

    private void setupListeners() {
        buttonRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });

        textViewLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = passwordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();
                if (!confirmPassword.isEmpty()) {
                    if (password.equals(confirmPassword)) {
                        confirmPasswordInputLayout.setHelperText("Passwords match");
                        confirmPasswordInputLayout.setHelperTextColor(getResources().getColorStateList(R.color.green));
                    } else {
                        confirmPasswordInputLayout.setHelperText("Passwords do not match");
                        confirmPasswordInputLayout.setHelperTextColor(getResources().getColorStateList(R.color.error_red));
                    }
                } else {
                    confirmPasswordInputLayout.setHelperText(null);
                }
            }
    };
        passwordInput.addTextChangedListener(passwordWatcher);
        confirmPasswordInput.addTextChangedListener(passwordWatcher);
    }

    private boolean validateInputs() {
        // Get input values
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate each field
        if (name.isEmpty()) {
            nameInput.setError("Full Name is required");
            return false;
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            nameInput.setError("Enter valid full name");
            return false;
        }

        if (address.isEmpty()) {
            addressInput.setError("Address is required");
            return false;
        } else if (!ADDRESS_PATTERN.matcher(address).matches()) {
            addressInput.setError("Enter a valid address");
            return false;
        }

        if (contact.isEmpty()) {
            contactInput.setError("Contact number is required");
            return false;
        } else if (!CONTACT_PATTERN.matcher(contact).matches()) {
            contactInput.setError("Enter a valid 10-digit phone number");
            return false;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return false;
        } else if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return false;
        }

        if (!confirmPassword.equals(password)) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void registerUser() {
        progressDialog.show();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("address", address);
                        userData.put("contact", contact);
                        userData.put("email", email);
                        userData.put("password", password);
                        userData.put("userId", user.getUid());
                        userData.put("role", "user");

                        db.collection("users")
                                .document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(RegisterActivity.this,
                                            "Registration successful!",
                                            Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    String errorMessage = "Failed to save user data.";
                                    if (e instanceof FirebaseNetworkException) {
                                        errorMessage = "Network error. Please check your internet connection.";
                                    } else if (e.getMessage() != null) {
                                        errorMessage = "Error: " + e.getMessage();
                                    }
                                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
            progressDialog.dismiss();
            String errorMessage = "Registration failed.";
            Exception exception = task.getException();
            if (exception instanceof FirebaseAuthUserCollisionException) {
                errorMessage = "This email is already registered.";
            } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                errorMessage = "Password is too weak.";
            } else if (exception instanceof FirebaseNetworkException) {
                errorMessage = "Network error. Please check your internet connection.";
            } else if (exception != null && exception.getMessage() != null) {
                errorMessage = "Error: " + exception.getMessage();
            }
            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
                });
    }
}