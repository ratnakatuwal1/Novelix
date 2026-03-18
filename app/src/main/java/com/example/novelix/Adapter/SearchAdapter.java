package com.example.novelix.Adapter;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.novelix.Model.Novel;
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
import java.util.function.Consumer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.NovelViewHolder> {
    private List<Novel> novelList;
    private Consumer<Novel> onItemClickListener;
    private Consumer<Novel> onBookmarkClickListener;
    private static final String TAG = "SearchAdapter";
    private static final String B2_BUCKET_NAME = "novelix-novel-Uploads";
    private static final String b2KeyId = "005727c657c8df80000000002";
    private static final String b2ApplicationKey = "K005IxPf8+b23o5U/NiUL9dm1g4cTu0";
    private static final String b2BucketId = "4712c78cb635a77c987d0f18";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public SearchAdapter(Consumer<Novel> onItemClickListener, Consumer<Novel> onBookmarkClickListener) {
        this.novelList = new ArrayList<>();
        this.onItemClickListener = onItemClickListener;
        this.onBookmarkClickListener = onBookmarkClickListener;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void setNovelList(List<Novel> novelList) {
        this.novelList = novelList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NovelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new NovelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelViewHolder holder, int position) {
        Novel novel = novelList.get(position);
        holder.novelName.setText(novel.getTitle() != null ? novel.getTitle() : "Unknown Title");
        holder.novelAuthorName.setText(novel.getAuthor() != null ? novel.getAuthor() : "Unknown Author");
        holder.genre.setText(novel.getCategory() != null ? novel.getCategory() : "Unknown Category");

        // Load cover image with signed URL
        String coverUrl = novel.getCoverUrl();
        if (coverUrl != null && !coverUrl.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    String signedCoverUrl = getSignedUrl(coverUrl);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Glide.with(holder.itemView.getContext())
                                .load(signedCoverUrl)
                                .apply(new RequestOptions()
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
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
            Log.w(TAG, "Cover URL is null or empty for novel: " + novel.getTitle());
        }

        // Check bookmark status and update icon
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId).collection("bookmarks")
                    .document(novel.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        boolean isBookmarked = documentSnapshot.exists();
                        holder.bookmarkIcon.setImageResource(isBookmarked ? R.drawable.bookmark_fill : R.drawable.bookmark);
                        holder.bookmarkIcon.setTag(isBookmarked ? "filled" : "unfilled");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to check bookmark status for novel: " + novel.getTitle(), e);
                        holder.bookmarkIcon.setImageResource(R.drawable.bookmark);
                        holder.bookmarkIcon.setTag("unfilled");
                    });
        } else {
            holder.bookmarkIcon.setImageResource(R.drawable.bookmark);
            holder.bookmarkIcon.setTag("unfilled");
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                onItemClickListener.accept(novel);
            }
        });

        // Handle bookmark click
        holder.bookmarkIcon.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(holder.itemView.getContext(), "Please log in to bookmark", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = auth.getCurrentUser().getUid();
            String tag = (String) holder.bookmarkIcon.getTag();
            if ("filled".equals(tag)) {
                // Unbookmark: Remove from Firestore
                db.collection("users").document(userId).collection("bookmarks")
                        .document(novel.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(), novel.getTitle() + " removed from bookmarks", Toast.LENGTH_SHORT).show();
                            holder.bookmarkIcon.setImageResource(R.drawable.bookmark);
                            holder.bookmarkIcon.setTag("unfilled");
                            Log.d(TAG, "Removed bookmark for novel: " + novel.getTitle());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Failed to remove bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to remove bookmark for novel: " + novel.getTitle(), e);
                        });
            } else {
                // Bookmark: Call the provided listener to add to Firestore
                if (onBookmarkClickListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onBookmarkClickListener.accept(novel);
                    // Update icon immediately to reflect the change
                    holder.bookmarkIcon.setImageResource(R.drawable.bookmark_fill);
                    holder.bookmarkIcon.setTag("filled");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return novelList.size();
    }

    private String getSignedUrl(String fileUrl) throws Exception {
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

            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("bucketId", b2BucketId);
            requestBodyJson.put("fileNamePrefix", fileKey);
            requestBodyJson.put("validDurationInSeconds", 3600);

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
                String encodedFileKey = URLEncoder.encode(fileKey, "UTF-8").replace("+", "%20");
                return downloadUrl + "/file/" + B2_BUCKET_NAME + "/" + encodedFileKey + "?Authorization=" + downloadAuthToken;
            }
        }
    }

    static class NovelViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage, bookmarkIcon;
        TextView novelName, novelAuthorName, genre;

        public NovelViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.coverImage);
            bookmarkIcon = itemView.findViewById(R.id.favNovel);
            novelName = itemView.findViewById(R.id.novelName);
            novelAuthorName = itemView.findViewById(R.id.novelAutherName);
            genre = itemView.findViewById(R.id.genre);
        }
    }
}