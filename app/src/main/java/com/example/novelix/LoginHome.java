package com.example.novelix;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.chromium.base.Callback;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import kotlinx.coroutines.Dispatchers;

public class LoginHome extends AppCompatActivity {
    private static final String TAG = "LoginHome";
    private static final int RC_SIGN_IN = 9001;
    private ImageView imageViewGoogleLogin;
    private Button registerButton, loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        executor = Executors.newSingleThreadExecutor();

        initializeViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity();
        }
    }

    private void initializeViews() {
        imageViewGoogleLogin = findViewById(R.id.imageViewGoogleLogin);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
    }

    private void setupListeners() {
        imageViewGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginHome.this, LoginActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginHome.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void signInWithGoogle() {
        showProgress(true);
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // *** FIX: Replaced the incorrect call with the correct callback-based implementation ***
        credentialManager.getCredentialAsync(
                this,
                request,
                null, // CancellationSignal
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // This is called on the executor thread, so switch to the main thread for UI.
                        runOnUiThread(() -> {
                            Log.d(TAG, "getCredential success");
                            handleSignInResult(result.getCredential());
                        });
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        // This is called on the executor thread, so switch to the main thread for UI.
                        runOnUiThread(() -> {
                            showProgress(false);
                            Log.e(TAG, "getCredential failed", e);
                            // If no credentials are found (e.g., user cancels or no saved accounts),
                            // fall back to the traditional sign-in flow.
                            if (e instanceof NoCredentialException) {
                                Toast.makeText(LoginHome.this, "No accounts found. Please sign in.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginHome.this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                            // Fallback to the traditional sign-in for a better user experience
                            startTraditionalGoogleSignIn();
                        });
                    }
                }
        );
    }

    private void handleSignInResult(Credential credential) {
        if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                try {
                    GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.getData());
                    String idToken = googleIdTokenCredential.getIdToken();
                    firebaseAuthWithGoogle(idToken);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse Google ID token credential", e);
                    runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(this, "Failed to process Google credentials", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                Log.e(TAG, "Unexpected credential type: " + credential.getType());
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(this, "Unsupported credential type", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            Log.e(TAG, "Unexpected credential class: " + credential.getClass().getName());
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Unsupported credential format", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        navigateToMainActivity();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginHome.this, "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Fallback method using traditional Google Sign-In
    private void startTraditionalGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Log.w(TAG, "Google sign in failed: account or ID token is null");
                    showProgress(false);
                    Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                showProgress(false);
                Toast.makeText(this, "Google Sign-In failed: " + e.getStatusCode(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginHome.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
