package com.example.novelix;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.novelix.Model.Book;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NovelDescription extends AppCompatActivity {
    private ImageView novelCoverImage;
    private TextView novelTitle;
    private TextView authorName;
    private TextView novelDescription;
    private TextView novelIsbn;
    private ImageView favNovel;
    private Button downloadButton;
    private Button readInAppButton;
    private RecyclerView genreRecyclerView;
    private TextView language;
    private LottieAnimationView loadingAnimation;
    private View overlayView;

    // B2 Bucket Name from your file URLs
    private static final String B2_BUCKET_NAME = "novelix-novel-Uploads";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String NOTIFICATION_CHANNEL_ID = "novel_download_channel";
    private static final int NOTIFICATION_ID = 1;
    private boolean isFavorited = false;
    private String novelFileUrl;
    private String coverUrl;
    private String category;
    private String novelId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String TAG = "NovelDescription";
    private String b2KeyId = "005727c657c8df80000000002";
    private String b2ApplicationKey = "K005IxPf8+b23o5U/NiUL9dm1g4cTu0";
    private String b2BucketId = "4712c78cb635a77c987d0f18";
    private static final String PREFS_NAME = "NovelixPrefs";
    private static final String KEY_OPENED_NOVELS = "opened_novels";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_novel_description);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        createNotificationChannel();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize UI components
        novelCoverImage = findViewById(R.id.novelCoverImage);
        novelTitle = findViewById(R.id.novelTitle);
        authorName = findViewById(R.id.authorName);
        novelDescription = findViewById(R.id.novelDescription);
        novelIsbn = findViewById(R.id.novelIsbn);
        favNovel = findViewById(R.id.favNovel);
        downloadButton = findViewById(R.id.downloadButton);
        readInAppButton = findViewById(R.id.readInAppButton);
        genreRecyclerView = findViewById(R.id.genreRecyclerView);
        language = findViewById(R.id.language);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        overlayView = findViewById(R.id.overlayView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            overlayView.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                    10f, 10f, android.graphics.Shader.TileMode.CLAMP));
        }

        Intent intent = getIntent();
        novelId = intent.getStringExtra("novel_id");

        if (novelId != null) {
            fetchNovelMetadata(novelId);
        } else {
            Toast.makeText(this, "Novel ID not provided.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no ID is found
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        favNovel.setOnClickListener(v -> {
            isFavorited = !isFavorited;
            favNovel.setImageResource(isFavorited ? R.drawable.bookmark_fill : R.drawable.bookmark);
            // Animate bookmark
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.2f, 1.0f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(100);
            scaleAnimation.setRepeatCount(1);
            scaleAnimation.setRepeatMode(Animation.REVERSE);
            favNovel.startAnimation(scaleAnimation);

            if (auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();
                String title = novelTitle.getText().toString();
                String author = authorName.getText().toString();
                Book book = new Book(novelId, title, coverUrl, author, category); // Updated to use coverUrl and category

                if (isFavorited) {
                    // Add to bookmarks
                    db.collection("users").document(userId).collection("bookmarks")
                            .document(novelId)
                            .set(book)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added to bookmarks", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding bookmark", e);
                                Toast.makeText(this, "Error adding bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // Remove from bookmarks
                    db.collection("users").document(userId).collection("bookmarks")
                            .document(novelId)
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Removed from bookmarks", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error removing bookmark", e);
                                Toast.makeText(this, "Error removing bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            } else {
                Toast.makeText(this, "Please log in to bookmark novels", Toast.LENGTH_SHORT).show();
                isFavorited = false; // Revert state if not logged in
                favNovel.setImageResource(R.drawable.bookmark);
            }
        });

        downloadButton.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                if (novelFileUrl != null && !novelFileUrl.isEmpty()) {
                    // Show overlay and Lottie animation
                    overlayView.setVisibility(View.VISIBLE);
                    loadingAnimation.setVisibility(View.VISIBLE);
                    loadingAnimation.playAnimation();
                    downloadButton.setEnabled(false); // Disable button during download
                    downloadNovel(novelFileUrl, novelTitle.getText().toString());
                } else {
                    Toast.makeText(this, "No download URL available", Toast.LENGTH_SHORT).show();
                }
            } else {
                requestStoragePermission();
            }
        });

        readInAppButton.setOnClickListener(v -> {
            if (isValidUrl(novelFileUrl)) {
                saveNovelAsOpened();
                fetchSignedUrlAndOpenInApp();
            } else {
                Toast.makeText(this, "Invalid URL for streaming.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveNovelAsOpened() {
        if (novelId == null || novelId.isEmpty()) {
            return; // Don't save if there's no ID
        }
        // Get SharedPreferences instance
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Get the existing set of opened novel IDs, or a new empty set if none exists
        Set<String> openedNovelIds = prefs.getStringSet(KEY_OPENED_NOVELS, new HashSet<>());
        // Create a new mutable set from the retrieved set
        Set<String> newOpenedNovelIds = new HashSet<>(openedNovelIds);
        // Add the current novel's ID
        newOpenedNovelIds.add(novelId);

        // Save the updated set back to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_OPENED_NOVELS, newOpenedNovelIds);
        editor.apply(); // apply() saves changes in the background

        Log.d(TAG, "Novel ID " + novelId + " was saved to continue reading list.");
    }

    private List<String> parseGenres(String category) {
        if (category == null || category.isEmpty()) {
            return new ArrayList<>();
        }
        String[] genreArray = category.replaceAll(",$", "").split(",");
        List<String> genres = new ArrayList<>();
        for (String genre : genreArray) {
            String trimmed = genre.trim();
            if (!trimmed.isEmpty()) {
                genres.add(trimmed);
            }
        }
        return genres;
    }

    private void fetchNovelMetadata(String novelId) {
        db.collection("novels").document(novelId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String coverUrl = documentSnapshot.getString("coverUrl");
                        String author = documentSnapshot.getString("author");
                        String description = documentSnapshot.getString("description");
                        String isbn = documentSnapshot.getString("isbn");
                        novelFileUrl = documentSnapshot.getString("fileUrl");
                        String languageStr = documentSnapshot.getString("language");
                        String category = documentSnapshot.getString("category");

                        List<String> genres = parseGenres(category);
                        updateUI(title, coverUrl, author, description, isbn, novelFileUrl, languageStr, genres);

                        if (auth.getCurrentUser() != null) {
                            String userId = auth.getCurrentUser().getUid();
                            db.collection("users").document(userId).collection("bookmarks")
                                    .document(novelId)
                                    .get()
                                    .addOnSuccessListener(bookmarkSnapshot -> {
                                        isFavorited = bookmarkSnapshot.exists();
                                        favNovel.setImageResource(isFavorited ? R.drawable.bookmark_fill : R.drawable.bookmark);
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error checking bookmark status", e));
                        }
                    } else {
                        Toast.makeText(this, "Novel not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch novel data: ", e);
                    Toast.makeText(this, "Failed to fetch novel data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUI(String title, String coverUrl, String author, String description, String isbn,
                          String fileUrl, String languageStr, List<String> genres) {
        novelTitle.setText(title != null ? title : "Unknown Title");
        authorName.setText(author != null ? author : "Unknown Author");
        novelDescription.setText(description != null ? description : "No description available");
        novelIsbn.setText(isbn != null ? "ISBN: " + isbn : "ISBN: Not available");
        language.setText(languageStr != null ? "Language: " + languageStr : "Language: Not available");

        // Load cover image using a dynamically generated signed URL
        if (isValidUrl(coverUrl)) {
            // Set a placeholder image first
            novelCoverImage.setImageResource(R.drawable.itendwithus);
            // Generate the signed URL in a background thread
            new Thread(() -> {
                try {
                    final String signedCoverUrl = getSignedUrl(coverUrl);
                    runOnUiThread(() -> Glide.with(NovelDescription.this)
                            .load(signedCoverUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache image against the temporary signed URL
                            .skipMemoryCache(true)
                            .error(R.drawable.itendwithus)
                            .into(novelCoverImage));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get signed URL for cover image", e);
                    runOnUiThread(() -> novelCoverImage.setImageResource(R.drawable.itendwithus)); // Show error image on failure
                }
            }).start();
        } else {
            novelCoverImage.setImageResource(R.drawable.itendwithus);
        }

        // Setup genre RecyclerView
        genreRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        genreRecyclerView.setAdapter(new GenreAdapter(genres));
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Invalid URL syntax: " + url, e);
            return false;
        }
    }

    private String getSignedUrl(String fileUrl) throws Exception {
        // 1. Extract the B2 file key (e.g., "novels/112cbec9....pdf") from the full URL.
        String fileKey;
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            String searchPrefix = "/file/" + B2_BUCKET_NAME + "/";
            int startIndex = path.indexOf(searchPrefix);
            if (startIndex == -1) throw new Exception("Cannot find bucket name in file URL path.");
            fileKey = URLDecoder.decode(path.substring(startIndex + searchPrefix.length()), "UTF-8");
        } catch (Exception e) {
            throw new Exception("Could not parse file key from URL: " + fileUrl);
        }

        // 2. Authorize account to get API URL and a temporary master auth token.
        OkHttpClient client = new OkHttpClient();
        String credentials = b2KeyId + ":" + b2ApplicationKey;
        String encodedCredentials = Base64.encodeToString(credentials.getBytes("UTF-8"), Base64.NO_WRAP);
        Request authorizeRequest = new Request.Builder()
                .url("https://api.backblazeb2.com/b2api/v2/b2_authorize_account")
                .header("Authorization", "Basic " + encodedCredentials)
                .build();

        try (Response authorizeResponse = client.newCall(authorizeRequest).execute()) {
            if (!authorizeResponse.isSuccessful()) {
                throw new IOException("B2 Authorization failed: " + authorizeResponse.body().string());
            }

            String responseBody = authorizeResponse.body().string();
            JSONObject authJson = new JSONObject(responseBody);

            // Updated to use the correct fields from B2 API response
            String apiUrl = authJson.getString("apiUrl");
            String downloadUrl = authJson.getString("downloadUrl");
            String accountAuthToken = authJson.getString("authorizationToken");

            // 3. Get a short-lived download authorization for the specific file.
            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("bucketId", b2BucketId);
            requestBodyJson.put("fileNamePrefix", fileKey);
            requestBodyJson.put("validDurationInSeconds", 3600); // 1 hour validity

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBodyJson.toString());
            Request downloadAuthRequest = new Request.Builder()
                    .url(apiUrl + "/b2api/v2/b2_get_download_authorization")
                    .header("Authorization", accountAuthToken)
                    .post(body)
                    .build();

            try (Response downloadAuthResponse = client.newCall(downloadAuthRequest).execute()) {
                if (!downloadAuthResponse.isSuccessful()) {
                    throw new IOException("B2 Download authorization failed: " + downloadAuthResponse.body().string());
                }

                JSONObject downloadAuthJson = new JSONObject(downloadAuthResponse.body().string());
                String downloadAuthToken = downloadAuthJson.getString("authorizationToken");

                // 4. Construct the final pre-signed URL. The file key must be URL-encoded.
                String encodedFileKey = URLEncoder.encode(fileKey, "UTF-8").replace("+", "%20");
                return downloadUrl + "/file/" + B2_BUCKET_NAME + "/" + encodedFileKey + "?Authorization=" + downloadAuthToken;
            }
        }
    }


    private void downloadNovel(String fileUrl, String fileName) {
        if (!isValidUrl(fileUrl)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Invalid file URL", Toast.LENGTH_SHORT).show();
                loadingAnimation.cancelAnimation();
                loadingAnimation.setVisibility(View.GONE);
                overlayView.setVisibility(View.GONE);
                downloadButton.setEnabled(true);
            });
            return;
        }

        fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
        File novelixDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Novelix/Downloaded Novels");
        if (!novelixDir.exists()) {
            novelixDir.mkdirs();
        }
        File file = new File(novelixDir, fileName);

        showDownloadNotification(fileName, true, null);
        String finalFileName = fileName;

        new Thread(() -> {
            try {
                // Get a temporary signed URL for the download
                String signedUrl = getSignedUrl(fileUrl);
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(signedUrl) // Use the secure, temporary URL
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Download failed: " + response.message());
                }

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    outputStream.write(response.body().bytes());
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Novel downloaded to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    showDownloadNotification(finalFileName, false, file);
                    loadingAnimation.cancelAnimation();
                    loadingAnimation.setVisibility(View.GONE);
                    overlayView.setVisibility(View.GONE);
                    downloadButton.setEnabled(true);
                });
            } catch (Exception e) {
                Log.e(TAG, "Download Error", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_ID);
                    loadingAnimation.cancelAnimation();
                    loadingAnimation.setVisibility(View.GONE);
                    overlayView.setVisibility(View.GONE);
                    downloadButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void fetchSignedUrlAndOpenInApp() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching novel...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                final String signedUrl = getSignedUrl(novelFileUrl);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Intent intent = new Intent(NovelDescription.this, NovelReaderActivity.class);
                    intent.putExtra("novel_url", signedUrl);
                    intent.putExtra("novel_title", novelTitle.getText().toString());
                    intent.putExtra("novel_id", novelId);
                    startActivity(intent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error fetching signed URL for in-app reading: ", e);
                    Toast.makeText(this, "Error fetching novel: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // --- PERMISSIONS and NOTIFICATIONS ---

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Novel Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for novel downloads");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void showDownloadNotification(String fileName, boolean isOngoing, File file) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.novel_logo1)
                .setContentTitle(isOngoing ? "Downloading Novel" : "Download Complete")
                .setContentText(isOngoing ? "Downloading " + fileName : fileName + " downloaded")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(isOngoing)
                .setAutoCancel(!isOngoing);

        // If download is complete, create an Intent to open the file.
        if (!isOngoing && file != null) {
            // NOTE: Ensure your AndroidManifest.xml has a <provider> for FileProvider.
            // The authority must be "com.example.novelix.fileprovider".
            Uri fileUri = FileProvider.getUriForFile(this, "com.example.novelix.fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Check if an app exists to handle the PDF file
            if (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                showInstallPdfViewerPrompt();
                builder.setContentIntent(null); // No app to open it with
            } else {
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                builder.setContentIntent(pendingIntent);
            }
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showInstallPdfViewerPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("No PDF Viewer Found")
                .setMessage("To open this file, you need a PDF viewer. Would you like to search for one on the Play Store?")
                .setPositiveButton("Search Store", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pdf%20viewer")));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=pdf%20viewer")));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean checkStoragePermission() {
        // For Android 10 (API 29) and above, Scoped Storage is used, so we don't need explicit permission for our own app's folders or public media collections.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry download
                if (novelFileUrl != null && !novelFileUrl.isEmpty()) {
                    overlayView.setVisibility(View.VISIBLE);
                    loadingAnimation.setVisibility(View.VISIBLE);
                    loadingAnimation.playAnimation();
                    downloadButton.setEnabled(false);
                    downloadNovel(novelFileUrl, novelTitle.getText().toString());
                }
            } else {
                Toast.makeText(this, "Storage permission is required to download novels.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- RecyclerView Adapter for Genres ---
    private static class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.ViewHolder> {
        private final List<String> genres;

        public GenreAdapter(List<String> genres) {
            this.genres = genres;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new ViewHolder(android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.genre, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.chip.setText(genres.get(position));
        }

        @Override
        public int getItemCount() {
            return genres.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            Chip chip;
            public ViewHolder(android.view.View itemView) {
                super(itemView);
                chip = itemView.findViewById(R.id.genre_name);
            }
        }
    }
}