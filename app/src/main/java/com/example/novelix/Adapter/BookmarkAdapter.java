package com.example.novelix.Adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.novelix.Model.Book;
import com.example.novelix.NovelDescription;
import com.example.novelix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookViewHolder> {
    private List<Book> bookList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String TAG = "BookmarkAdapter";
    private static final String B2_BUCKET_NAME = "novelix-novel-Uploads";
    private static final String b2KeyId = "005727c657c8df80000000002";
    private static final String b2ApplicationKey = "K005IxPf8+b23o5U/NiUL9dm1g4cTu0";
    private static final String b2BucketId = "4712c78cb635a77c987d0f18";

    public BookmarkAdapter(List<Book> bookList) {
        this.bookList = bookList;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fav, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Book book = bookList.get(position);
        holder.novelName.setText(book.getTitle() != null ? book.getTitle() : "Unknown Title");
        holder.novelAuthor.setText(book.getAuthor() != null ? book.getAuthor() : "Unknown Author");

        // Handle category display by parsing genres
        List<String> genres = parseGenres(book.getCategory());
        if (!genres.isEmpty()) {
            holder.genre.setText(String.join(", ", genres));
        } else {
            holder.genre.setText("Unknown Category");
            Log.w(TAG, "No valid genres found for book: " + book.getTitle());
        }

        // Handle cover image with signed URL
        String coverUrl = book.getCoverUrl();
        if (coverUrl != null && !coverUrl.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    String signedCoverUrl = getSignedUrl(coverUrl);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Glide.with(holder.itemView.getContext())
                                .load(signedCoverUrl)
                                .apply(new RequestOptions()
                                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache signed URLs
                                        .skipMemoryCache(true)
                                        .placeholder(R.drawable.itendwithus)
                                        .error(R.drawable.itendwithus)
                                        .dontAnimate())
                                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                        Log.e(TAG, "Failed to load cover image for URL: " + coverUrl, e);
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                        Log.d(TAG, "Successfully loaded cover image for URL: " + coverUrl);
                                        return false;
                                    }
                                })
                                .into(holder.coverImage);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get signed URL for cover image: " + coverUrl, e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        holder.coverImage.setImageResource(R.drawable.itendwithus);
                    });
                }
            }).start();
        } else {
            holder.coverImage.setImageResource(R.drawable.itendwithus);
            Log.w(TAG, "Cover URL is null or empty for book: " + book.getTitle());
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), NovelDescription.class);
            intent.putExtra("novel_id", book.getId());
            holder.itemView.getContext().startActivity(intent);
        });

        holder.deleteIcon.setOnClickListener(v -> {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId).collection("bookmarks")
                    .document(book.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        bookList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, bookList.size());
                        Log.d(TAG, "Bookmark removed for book: " + book.getTitle());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove bookmark for book: " + book.getTitle(), e);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    // Parse comma-separated category string into a list of genres
    private List<String> parseGenres(String category) {
        List<String> genres = new ArrayList<>();
        if (category != null && !category.trim().isEmpty()) {
            String[] genreArray = category.replaceAll(",$", "").split(",");
            for (String genre : genreArray) {
                String trimmed = genre.trim();
                if (!trimmed.isEmpty()) {
                    genres.add(trimmed);
                }
            }
        }
        return genres;
    }

    private String getSignedUrl(String fileUrl) throws Exception {
        // 1. Extract the B2 file key (e.g., "novels/112cbec9....jpg") from the full URL.
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

    public class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        ImageView deleteIcon;
        TextView novelName;
        TextView novelAuthor;
        TextView genre;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.coverImage);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
            novelName = itemView.findViewById(R.id.novelName);
            novelAuthor = itemView.findViewById(R.id.novelAutherName);
            genre = itemView.findViewById(R.id.genre);
        }
    }
}