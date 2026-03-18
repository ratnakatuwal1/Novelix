package com.example.novelix;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfile extends AppCompatActivity {
    private static final String TAG = "EditProfile";
    private ImageView imageBack, imageViewProfile;
    private CardView editProfileIcon;
    private EditText editTextName, editTextPhoneNumber, editTextEmail, editTextAddress, editTextGender;
    private Button btnUpdateProfile;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private View overlayLayout;
    private LottieAnimationView lottieAnimationView;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String B2_KEY_ID = "005727c657c8df80000000002";
    private static final String B2_APPLICATION_KEY = "K005IxPf8+b23o5U/NiUL9dm1g4cTu0";
    private static final String B2_BUCKET_NAME = "novelix-novel-Uploads";
    private static final String B2_BUCKET_ID = "4712c78cb635a77c987d0f18";
    private static final String B2_FILE_DOMAIN_FALLBACK = "f005.backblazeb2.com";
    private String b2ApiUrl = null;
    private String b2DownloadUrl = null;
    private String b2UploadUrl = null;
    private String b2UploadAuthToken = null;

    // Backblaze B2 Authentication Helper
    private static class B2Auth {
        private static final String B2_AUTH_URL = "https://api.backblazeb2.com/b2api/v2/b2_authorize_account";

        public static class B2AuthResponse {
            String authorizationToken;
            String apiUrl;
            String downloadUrl;

            B2AuthResponse(String authorizationToken, String apiUrl, String downloadUrl) {
                this.authorizationToken = authorizationToken;
                this.apiUrl = apiUrl;
                this.downloadUrl = downloadUrl;
            }
        }

        public static B2AuthResponse authorize() throws Exception {
            OkHttpClient client = new OkHttpClient();
            String credentials = Credentials.basic(B2_KEY_ID, B2_APPLICATION_KEY);

            Request request = new Request.Builder()
                    .url(B2_AUTH_URL)
                    .header("Authorization", credentials)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to authorize with Backblaze B2: " + response.code());
                }

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                String authorizationToken = json.getString("authorizationToken");
                String apiUrl = json.getString("apiUrl");
                String downloadUrl = json.getString("downloadUrl");
                return new B2AuthResponse(authorizationToken, apiUrl, downloadUrl);
            }
        }
    }

    // Get upload URL and token for the bucket
    private void getUploadUrlAndToken(String apiUrl, String authToken) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("bucketId", B2_BUCKET_ID);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(apiUrl + "/b2api/v2/b2_get_upload_url")
                        .header("Authorization", authToken)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new Exception("Failed to get upload URL: " + response.code());
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    b2UploadUrl = json.getString("uploadUrl");
                    b2UploadAuthToken = json.getString("authorizationToken");

                    runOnUiThread(() -> Log.d(TAG, "Upload URL obtained successfully"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting upload URL: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Failed to prepare image upload: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Authenticate with Backblaze B2 and get upload URL
        new Thread(() -> {
            try {
                B2Auth.B2AuthResponse authResponse = B2Auth.authorize();
                b2ApiUrl = authResponse.apiUrl;
                b2DownloadUrl = authResponse.downloadUrl;
                runOnUiThread(() -> Log.d(TAG, "B2 Auth successful. API URL: " + b2ApiUrl + ", Download URL: " + b2DownloadUrl));
                getUploadUrlAndToken(b2ApiUrl, authResponse.authorizationToken);
            } catch (Exception e) {
                Log.e(TAG, "B2 Auth failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Failed to connect to image server: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();

        InitializedView();
        SetupListeners();
        LoadUserDataFromFirestore();
    }

    private void InitializedView() {
        imageBack = findViewById(R.id.imageBack);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        editProfileIcon = findViewById(R.id.editProfileIcon);
        editTextName = findViewById(R.id.editTextName);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextGender = findViewById(R.id.editTextGender);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        overlayLayout = findViewById(R.id.overlay_layout);
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
    }

    private void LoadUserDataFromFirestore() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editTextName.setText(documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "");
                        editTextEmail.setText(documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "");
                        editTextAddress.setText(documentSnapshot.getString("address") != null ? documentSnapshot.getString("address") : "");
                        editTextPhoneNumber.setText(documentSnapshot.getString("contact") != null ? documentSnapshot.getString("contact") : "");
                        editTextGender.setText(documentSnapshot.getString("gender") != null ? documentSnapshot.getString("gender") : "");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.placeholder)
                                    .error(R.drawable.placeholder)
                                    .circleCrop()
                                    .into(imageViewProfile);
                        } else {
                            imageViewProfile.setImageResource(R.drawable.placeholder);
                        }
                    } else {
                        editTextName.setText("");
                        editTextEmail.setText("");
                        editTextAddress.setText("");
                        editTextPhoneNumber.setText("");
                        editTextGender.setText("");
                        imageViewProfile.setImageResource(R.drawable.placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load profile data: " + e.getMessage());
                    Toast.makeText(this, "Failed to load profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    imageViewProfile.setImageResource(R.drawable.placeholder);
                });
    }

    private void showLoadingAnimation() {
        overlayLayout.setVisibility(View.VISIBLE);
        lottieAnimationView.playAnimation();
    }

    private void hideLoadingAnimation() {
        overlayLayout.setVisibility(View.GONE);
        lottieAnimationView.cancelAnimation();
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .circleCrop()
                    .into(imageViewProfile);
            // Do not call uploadImageToB2() here; defer to btnUpdateProfile
        }
    }

    private CompletableFuture<String> uploadImageToB2() {
        CompletableFuture<String> future;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            future = new CompletableFuture<>();
        } else {
            future = null;
        }
        if (imageUri == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                future.complete(null); // No image to upload
            }
            return future;
        }

        if (b2UploadUrl == null || b2UploadAuthToken == null) {
            runOnUiThread(() -> Toast.makeText(this, "Image server not initialized. Please try again.", Toast.LENGTH_SHORT).show());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                future.completeExceptionally(new Exception("Image server not initialized"));
            }
            return future;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        String fileName = "profile_images/" + uid + ".jpg";

        new Thread(() -> {
            try {
                File tempFile = createTempFileFromUri(imageUri);
                String sha1 = calculateSHA1(tempFile);
                Log.d(TAG, "File SHA1: " + sha1 + ", Size: " + tempFile.length());

                OkHttpClient client = new OkHttpClient();
                RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse("image/jpeg"));
                Request request = new Request.Builder()
                        .url(b2UploadUrl)
                        .header("Authorization", b2UploadAuthToken)
                        .header("X-Bz-File-Name", fileName)
                        .header("X-Bz-Content-Sha1", sha1)
                        .header("Content-Type", "image/jpeg")
                        .post(fileBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Upload response code: " + response.code() + ", Body: " + responseBody);
                    if (!response.isSuccessful()) {
                        throw new Exception("Failed to upload image to Backblaze B2: " + response.code() + " - " + responseBody);
                    }

                    String fileUrl;
                    if (b2DownloadUrl != null && !b2DownloadUrl.isEmpty()) {
                        fileUrl = b2DownloadUrl + "/file/" + B2_BUCKET_NAME + "/" + fileName;
                    } else {
                        fileUrl = "https://" + B2_FILE_DOMAIN_FALLBACK + "/file/" + B2_BUCKET_NAME + "/" + fileName;
                        Log.w(TAG, "Using fallback download URL (check auth response)");
                    }
                    Log.d(TAG, "File uploaded successfully. URL: " + fileUrl);

                    // Save URL to Firestore
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("profileImageUrl", fileUrl);
                    firestore.collection("users").document(uid)
                            .update(profileData)
                            .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                                Log.d(TAG, "Firestore updated with profileImageUrl: " + fileUrl);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    future.complete(fileUrl);
                                }
                            }))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Firestore update failed: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(this, "Failed to update profile picture metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    future.completeExceptionally(e);
                                }
                            });

                    tempFile.delete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Upload error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_LONG).show());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    future.completeExceptionally(e);
                }
            }
        }).start();
        return future;
    }

    private String calculateSHA1(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[1024];
            int read;
            while ((read = fis.read(data)) != -1) {
                md.update(data, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private File createTempFileFromUri(Uri uri) throws Exception {
        File tempFile = File.createTempFile("profile_", ".jpg", getCacheDir());
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private void SetupListeners() {
        imageBack.setOnClickListener(v -> finish());

        editProfileIcon.setOnClickListener(v -> openImagePicker());

        btnUpdateProfile.setOnClickListener(v -> {
            showLoadingAnimation();
            if (firebaseAuth.getCurrentUser() == null) {
                hideLoadingAnimation();
                Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String uid = firebaseAuth.getCurrentUser().getUid();
            Map<String, Object> updatedProfile = new HashMap<>();

            // Get current input values
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String address = editTextAddress.getText().toString().trim();
            String contact = editTextPhoneNumber.getText().toString().trim();
            String gender = editTextGender.getText().toString().trim();

            // First, handle image upload if a new image was selected
            CompletableFuture<String> imageUploadFuture = uploadImageToB2();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                imageUploadFuture.whenComplete((newImageUrl, throwable) -> {
                    if (throwable != null) {
                        runOnUiThread(() -> {
                            hideLoadingAnimation();
                            Toast.makeText(this, "Failed to upload image: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // Fetch current Firestore data to compare
                    firestore.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String originalName = documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "";
                                String originalEmail = documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "";
                                String originalAddress = documentSnapshot.getString("address") != null ? documentSnapshot.getString("address") : "";
                                String originalContact = documentSnapshot.getString("contact") != null ? documentSnapshot.getString("contact") : "";
                                String originalGender = documentSnapshot.getString("gender") != null ? documentSnapshot.getString("gender") : "";
                                String profileImageUrl = newImageUrl != null ? newImageUrl : documentSnapshot.getString("profileImageUrl");

                                // Add fields to update only if they are non-empty and different from original
                                if (!name.isEmpty() && !name.equals(originalName)) {
                                    updatedProfile.put("name", name);
                                }
                                if (!email.isEmpty() && !email.equals(originalEmail)) {
                                    updatedProfile.put("email", email);
                                }
                                if (!address.isEmpty() && !address.equals(originalAddress)) {
                                    updatedProfile.put("address", address);
                                }
                                if (!contact.isEmpty() && !contact.equals(originalContact)) {
                                    updatedProfile.put("contact", contact);
                                }
                                if (!gender.isEmpty() && !gender.equals(originalGender)) {
                                    updatedProfile.put("gender", gender);
                                }
                                if (newImageUrl != null) {
                                    updatedProfile.put("profileImageUrl", newImageUrl);
                                }

                                // Proceed with update only if there are changes or a new image
                                if (!updatedProfile.isEmpty()) {
                                    firestore.collection("users").document(uid)
                                            .update(updatedProfile)
                                            .addOnSuccessListener(aVoid -> {
                                                runOnUiThread(() -> {
                                                    hideLoadingAnimation();
                                                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                                    Intent resultIntent = new Intent();
                                                    resultIntent.putExtra("name", name.isEmpty() ? originalName : name);
                                                    resultIntent.putExtra("email", email.isEmpty() ? originalEmail : email);
                                                    resultIntent.putExtra("address", address.isEmpty() ? originalAddress : address);
                                                    resultIntent.putExtra("contact", contact.isEmpty() ? originalContact : contact);
                                                    resultIntent.putExtra("gender", gender.isEmpty() ? originalGender : gender);
                                                    if (profileImageUrl != null) {
                                                        resultIntent.putExtra("profileImageUrl", profileImageUrl);
                                                    }
                                                    setResult(RESULT_OK, resultIntent);
                                                    finish();
                                                });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Profile update failed: " + e.getMessage());
                                                runOnUiThread(() -> {
                                                    hideLoadingAnimation();
                                                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                            });
                                } else {
                                    runOnUiThread(() -> {
                                        hideLoadingAnimation();
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("name", originalName);
                                        resultIntent.putExtra("email", originalEmail);
                                        resultIntent.putExtra("address", originalAddress);
                                        resultIntent.putExtra("contact", originalContact);
                                        resultIntent.putExtra("gender", originalGender);
                                        if (profileImageUrl != null) {
                                            resultIntent.putExtra("profileImageUrl", profileImageUrl);
                                        }
                                        setResult(RESULT_OK, resultIntent);
                                        Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fetch profile data for update: " + e.getMessage());
                                runOnUiThread(() -> {
                                    hideLoadingAnimation();
                                    Toast.makeText(this, "Failed to fetch profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            });
                });
            }
        });
    }
}