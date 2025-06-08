package com.example.novelix;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Verification extends AppCompatActivity {
    private TextInputEditText digit1, digit2, digit3, digit4;
    private MaterialButton verifyButton;
    private View backButton, backToSignIn;
    private String email;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        email = getIntent().getStringExtra("email");

        digit1 = findViewById(R.id.digit1);
        digit2 = findViewById(R.id.digit2);
        digit3 = findViewById(R.id.digit3);
        digit4 = findViewById(R.id.digit4);
        verifyButton = findViewById(R.id.verifyButton);
        backButton = findViewById(R.id.imageViewBack);
        backToSignIn = findViewById(R.id.backToSignIn);

        setupDigitInputFocus();

        verifyButton.setOnClickListener(v -> verifyCode());
        backButton.setOnClickListener(v -> finish());
        backToSignIn.setOnClickListener(v -> finish());
    }

    private void setupDigitInputFocus() {
        TextInputEditText[] digits = {digit1, digit2, digit3, digit4};
        for (int i = 0; i < digits.length; i++) {
            final int index = i;
            digits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < digits.length - 1) {
                        digits[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        digits[index - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void verifyCode() {
        String code = digit1.getText().toString() +
                digit2.getText().toString() +
                digit3.getText().toString() +
                digit4.getText().toString();

        if (code.length() != 4) {
            showCodeError("Please enter complete 4-digit code");
            return;
        }

        if (!code.matches("\\d{4}")) {
            showCodeError("Code should contain only numbers");
            return;
        }

        verifyButton.setEnabled(false);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying code...");
        progressDialog.show();

        new Handler().postDelayed(() -> {
            progressDialog.dismiss();
            verifyButton.setEnabled(true);

            if (isValidCode(code)) {
                Intent intent = new Intent(Verification.this, NewPassword.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            } else {
                showCodeError("Invalid verification code");
            }
        }, 1500);
    }

    private boolean isValidCode(String code) {
        return true;
            }

    private void showCodeError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        // Highlight all digit fields
        int errorColor = ContextCompat.getColor(this, R.color.error_red);
        digit1.setBackgroundTintList(ColorStateList.valueOf(errorColor));
        digit2.setBackgroundTintList(ColorStateList.valueOf(errorColor));
        digit3.setBackgroundTintList(ColorStateList.valueOf(errorColor));
        digit4.setBackgroundTintList(ColorStateList.valueOf(errorColor));

        // Reset colors after 2 seconds
        new Handler().postDelayed(() -> {
            int normalColor = ContextCompat.getColor(this, R.color.PrimaryText);
            digit1.setBackgroundTintList(ColorStateList.valueOf(normalColor));
            digit2.setBackgroundTintList(ColorStateList.valueOf(normalColor));
            digit3.setBackgroundTintList(ColorStateList.valueOf(normalColor));
            digit4.setBackgroundTintList(ColorStateList.valueOf(normalColor));
        }, 2000);
        }
    }