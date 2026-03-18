package com.example.novelix.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.novelix.NovelDescription;
import com.example.novelix.NovelReaderActivity;
import com.example.novelix.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class HomeFragment extends Fragment {
    private TextView progressTextViewWeeklyGoal;
    private LinearProgressIndicator progressBar;
    private TextView progressPercentageTextView;
    private TextView motivationTextView;
    private RecyclerView genresRecyclerView;
    private RecyclerView genreBookRecyclerView;
    private RecyclerView recommendedRecyclerView;
    private RecyclerView continueRecyclerView;
    private TextView noRecommendationTextView;
    private TextView noContinueReadingTextView;
    private List<String> genres;
    private List<Book> recommendedBooks;
    private List<Book> continueReadingBooks;
    private List<Book> genreBooks;
    private List<Book> allFetchedBooks = new ArrayList<>();
    private FirebaseFirestore db;
    private BookAdapter genreBookAdapter;
    private BookAdapter recommendedAdapter;
    private BookAdapter continueAdapter;
    private static final String TAG = "HomeFragment";
    private static final String[] PREDEFINED_GENRES = {
            "Fiction", "Non-Fiction", "Fantasy", "Science Fiction",
            "Mystery", "Romance", "Thriller", "Historical",
            "Adventure", "Horror"
    };

    //private static final String PREFS_NAME = "NovelixPrefs";
    private static final String KEY_OPENED_NOVELS = "opened_novels";
    private static final String KEY_READING_TIME = "reading_time_minutes";
    private static final String KEY_WEEK_NUMBER = "week_number";
    private static final int WEEKLY_GOAL_MINUTES = 500; // Total goal: 500 minutes
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://.*\\.(jpg|jpeg|png|gif|bmp|webp)$", Pattern.CASE_INSENSITIVE);

    // Backblaze B2 credentials
    private static final String B2_KEY_ID = "005727c657c8df80000000002";
    private static final String B2_APPLICATION_KEY = "K005IxPf8+b23o5U/NiUL9dm1g4cTu0";
    private String b2AuthorizationToken = null;
    private String b2DownloadUrl = null;
    private String selectedGenre = "All";
    private boolean isRecommendationServiceRunning = false;

    private static class Book {
        String id;
        String title;
        String coverUrl;
        String genre;
        String author;
        String description;
        String isbn;
        String fileUrl;

        Book(String id, String title, String coverUrl, String genre, String author, String description, String isbn, String fileUrl) {
            this.id = id;
            this.title = title;
            this.coverUrl = coverUrl;
            this.genre = genre;
            this.author = author;
            this.description = description;
            this.isbn = isbn;
            this.fileUrl = fileUrl;
        }
    }

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

    public HomeFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        initializeData();

        // Authenticate with Backblaze B2
        new Thread(() -> {
            try {
                B2Auth.B2AuthResponse authResponse = B2Auth.authorize();
                b2AuthorizationToken = authResponse.authorizationToken;
                b2DownloadUrl = authResponse.downloadUrl;
                Log.d(TAG, "B2 Authorization successful. Token: " + b2AuthorizationToken);
            } catch (Exception e) {
                Log.e(TAG, "Failed to authorize with Backblaze B2", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to connect to image server", Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();

        // Initialize weekly reading time
        initializeWeeklyReadingTime();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupRecyclerViews();
        if (isNetworkAvailable()) {
            fetchGenresFromFirestore();
            fetchNovelsFromFirestore(selectedGenre);
            checkRecommendationServiceStatus();
        } else {
            Toast.makeText(getContext(), "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            isRecommendationServiceRunning = false;
            updateRecommendationUI();
            updateContinueReadingUI();
        }
        updateWeeklyGoalUI(); // Update UI for weekly goal
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            refreshContinueReading();
            updateWeeklyGoalUI(); // Refresh weekly goal on resume
        }
    }

    private SharedPreferences getUserPrefs() {
        String prefsName = getUserPrefsName();
        return requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    private String getUserPrefsName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "NovelixPrefs_" + user.getUid() : "NovelixPrefs_guest";
    }
    private void initializeViews(View view) {
        progressTextViewWeeklyGoal = view.findViewById(R.id.progressTextViewWeeklyGoal);
        progressBar = view.findViewById(R.id.progressBar);
        progressPercentageTextView = view.findViewById(R.id.progressPercentageTextView);
        motivationTextView = view.findViewById(R.id.motivationTextView);
        genresRecyclerView = view.findViewById(R.id.genresRecyclerView);
        genreBookRecyclerView = view.findViewById(R.id.genreBookRecyclerView);
        recommendedRecyclerView = view.findViewById(R.id.recommendedRecyclerView);
        continueRecyclerView = view.findViewById(R.id.continueRecyclerView);
        noRecommendationTextView = view.findViewById(R.id.noRecommendationsTextView);
        noContinueReadingTextView = view.findViewById(R.id.noContinueReadingTextView);

        if (noRecommendationTextView != null) {
            noRecommendationTextView.setVisibility(View.GONE);
        }
        if (noContinueReadingTextView != null) {
            noContinueReadingTextView.setVisibility(View.GONE);
        }
    }

    private void initializeData() {
        genres = new ArrayList<>();
        recommendedBooks = new ArrayList<>();
        continueReadingBooks = new ArrayList<>();
        genreBooks = new ArrayList<>();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Initialize weekly reading time and reset if necessary
    private void initializeWeeklyReadingTime() {
        if (getContext() == null) return;
        SharedPreferences prefs = getUserPrefs();
        int currentWeek = getCurrentWeekNumber();
        int savedWeek = prefs.getInt(KEY_WEEK_NUMBER, -1);

        if (savedWeek != currentWeek) {
            // Reset for new week
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_WEEK_NUMBER, currentWeek);
            editor.putInt(KEY_READING_TIME, 0);
            editor.apply();
            Log.d(TAG, "Weekly reading time reset for week: " + currentWeek);
        }
    }

    // Get current week number
    private int getCurrentWeekNumber() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    // Update weekly goal UI
    private void updateWeeklyGoalUI() {
        if (getContext() == null) return;

        SharedPreferences prefs = getUserPrefs();
        int readingTimeMinutes = prefs.getInt(KEY_READING_TIME, 0);

        // Calculate progress percentage
        int progressPercentage = (readingTimeMinutes * 100) / WEEKLY_GOAL_MINUTES;
        progressPercentage = Math.min(progressPercentage, 100); // Cap at 100%

        // Update UI elements
        if (progressTextViewWeeklyGoal != null) {
            progressTextViewWeeklyGoal.setText(String.format("%d/%d minutes read", readingTimeMinutes, WEEKLY_GOAL_MINUTES));
        }
        if (progressBar != null) {
            progressBar.setProgress(progressPercentage);
        }
        if (progressPercentageTextView != null) {
            progressPercentageTextView.setText(String.format("%d%%", progressPercentage));
        }
        if (motivationTextView != null) {
            String motivationMessage = getMotivationMessage(readingTimeMinutes);
            motivationTextView.setText(motivationMessage);
        }

        Log.d(TAG, "Weekly Goal UI updated: " + readingTimeMinutes + "/" + WEEKLY_GOAL_MINUTES + " minutes, " + progressPercentage + "%");
    }

    // Get motivational message based on reading time
    private String getMotivationMessage(int readingTimeMinutes) {
        if (readingTimeMinutes >= 500) {
            return "Fantastic! You've reached your 500-minute reading goal!";
        } else if (readingTimeMinutes >= 375) {
            return "Almost there! Just a bit more to hit 500 minutes!";
        } else if (readingTimeMinutes >= 250) {
            return "Great job! You're halfway to your reading goal!";
        } else if (readingTimeMinutes >= 125) {
            return "You're making progress! Keep reading!";
        } else {
            return "Start reading to reach your 500-minute weekly goal!";
        }
    }

    // Add reading time (called from NovelReaderActivity)
    public void addReadingTime(long milliseconds) {
        if (getContext() == null) return;

        SharedPreferences prefs = getUserPrefs();
        int currentReadingTime = prefs.getInt(KEY_READING_TIME, 0);
        int additionalMinutes = (int) (milliseconds / 60000); // Convert milliseconds to minutes
        currentReadingTime += additionalMinutes;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_READING_TIME, currentReadingTime);
        editor.apply();

        Log.d(TAG, "Added " + additionalMinutes + " minutes. Total reading time: " + currentReadingTime);
        updateWeeklyGoalUI();
    }

    private void checkRecommendationServiceStatus() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("http://10.0.2.2:5000/health")
                        .build();

                try (okhttp3.Response response = client.newCall(request).execute()) {
                    Log.d(TAG, "Service response code: " + response.code() + ", isSuccessful: " + response.isSuccessful());
                    boolean serviceRunning = response.isSuccessful();
                    isRecommendationServiceRunning = serviceRunning;

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateRecommendationUI();
                            if (serviceRunning) {
                                fetchRecommendationsFromService(selectedGenre);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking recommendation service status", e);
                isRecommendationServiceRunning = false;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Service check failed - showing offline message");
                        updateRecommendationUI();
                    });
                }
            }
        }).start();
    }

    private void updateRecommendationUI() {
        Log.d(TAG, "=== UPDATE RECOMMENDATION UI CALLED ===");
        Log.d(TAG, "Service running: " + isRecommendationServiceRunning +
                ", Books count: " + recommendedBooks.size());
        Log.d(TAG, "noRecommendationTextView: " + (noRecommendationTextView != null));
        Log.d(TAG, "recommendedRecyclerView: " + (recommendedRecyclerView != null));

        if (noRecommendationTextView == null || recommendedRecyclerView == null) {
            Log.e(TAG, "UI elements are null!");
            return;
        }

        if (!isRecommendationServiceRunning) {
            noRecommendationTextView.setVisibility(View.VISIBLE);
            noRecommendationTextView.setText("Recommendation service is not running.\nNothing to show.");
            recommendedRecyclerView.setVisibility(View.VISIBLE);
            Log.d(TAG, "✅ SHOWING: Service not running message");

        } else if (recommendedBooks.isEmpty()) {
            noRecommendationTextView.setVisibility(View.VISIBLE);
            noRecommendationTextView.setText("No recommendations available.");
            recommendedRecyclerView.setVisibility(View.GONE);
            Log.d(TAG, "✅ SHOWING: No recommendations message");

        } else {
            noRecommendationTextView.setVisibility(View.GONE);
            recommendedRecyclerView.setVisibility(View.VISIBLE);

            List<Book> displayBooks = recommendedBooks.subList(0, Math.min(8, recommendedBooks.size()));
            if (recommendedAdapter != null) {
                recommendedAdapter.updateData(displayBooks);
            }
            Log.d(TAG, "✅ SHOWING: " + displayBooks.size() + " recommendations");
        }

        Log.d(TAG, "=== END UPDATE RECOMMENDATION UI ===");
    }

    private void updateContinueReadingUI() {
        Log.d(TAG, "=== UPDATE CONTINUE READING UI CALLED ===");
        Log.d(TAG, "Continue Reading Books count: " + continueReadingBooks.size());
        Log.d(TAG, "noContinueReadingTextView: " + (noContinueReadingTextView != null));
        Log.d(TAG, "continueRecyclerView: " + (continueRecyclerView != null));

        if (noContinueReadingTextView == null || continueRecyclerView == null) {
            Log.e(TAG, "Continue Reading UI elements are null!");
            return;
        }

        if (continueReadingBooks.isEmpty()) {
            noContinueReadingTextView.setVisibility(View.VISIBLE);
            noContinueReadingTextView.setText("No books in progress.\nStart reading to see books here.");
            continueRecyclerView.setVisibility(View.VISIBLE);
            Log.d(TAG, "✅ SHOWING: No continue reading message");
        } else {
            noContinueReadingTextView.setVisibility(View.GONE);
            continueRecyclerView.setVisibility(View.VISIBLE);
            if (continueAdapter != null) {
                continueAdapter.updateData(continueReadingBooks);
            }
            Log.d(TAG, "✅ SHOWING: " + continueReadingBooks.size() + " continue reading books");
        }
        Log.d(TAG, "=== END UPDATE CONTINUE READING UI ===");
    }

    private void fetchRecommendationsFromService(String genreFilter) {
        if (!isRecommendationServiceRunning) return;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = String.format("http://10.0.2.2:5000/recommendations?genre=%s", genreFilter);

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .build();

                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONArray recommendedIds = new JSONArray(responseBody);

                        List<String> ids = new ArrayList<>();
                        for (int i = 0; i < recommendedIds.length() && i < 8; i++) {
                            ids.add(recommendedIds.getString(i));
                        }

                        fetchRecommendedBooksByIds(ids);
                    } else {
                        Log.e(TAG, "Failed to fetch recommendations: " + response.code());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Failed to fetch recommendations", Toast.LENGTH_SHORT).show();
                                updateRecommendationUI();
                            });
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching recommendations", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Recommendation service error", Toast.LENGTH_SHORT).show();
                        updateRecommendationUI();
                    });
                }
            }
        }).start();
    }

    private void fetchRecommendedBooksByIds(List<String> bookIds) {
        if (bookIds.isEmpty()) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updateRecommendationUI);
            }
            return;
        }

        db.collection("novels").whereIn(FieldPath.documentId(), bookIds).limit(8).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String id = document.getId();
                            String title = document.getString("title");
                            String coverUrl = document.getString("coverUrl");
                            String category = document.getString("category");
                            String author = document.getString("author");
                            String description = document.getString("description");
                            String isbn = document.getString("isbn");
                            String fileUrl = document.getString("fileUrl");

                            if (title != null && !title.isEmpty() && coverUrl != null && !coverUrl.isEmpty()) {
                                Book book = new Book(id, title, coverUrl, category, author, description, isbn, fileUrl);
                                books.add(book);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing recommended book", e);
                        }
                    }

                    recommendedBooks.clear();
                    recommendedBooks.addAll(books);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::updateRecommendationUI);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch recommended books", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::updateRecommendationUI);
                    }
                });
    }

    private void setupRecyclerViews() {
        genreBookAdapter = new BookAdapter(genreBooks);
        recommendedAdapter = new BookAdapter(recommendedBooks);
        continueAdapter = new BookAdapter(continueReadingBooks);

        genresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        genresRecyclerView.setAdapter(new GenreAdapter(genres));

        recommendedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendedRecyclerView.setAdapter(recommendedAdapter);
        recommendedRecyclerView.setVisibility(View.GONE);

        continueRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        continueRecyclerView.setAdapter(continueAdapter);
        continueRecyclerView.setVisibility(View.GONE);

        genreBookRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        genreBookRecyclerView.setAdapter(genreBookAdapter);
    }

    private void fetchGenresFromFirestore() {
        Log.d(TAG, "Fetching genres from Firestore...");
        genres.clear();
        genres.add("All");
        genres.addAll(Arrays.asList(PREDEFINED_GENRES));

        db.collection("custom_categories").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String category = document.getString("name");
                        if (category != null && !category.isEmpty() && !genres.contains(category)) {
                            genres.add(category);
                        }
                    }
                    if (genresRecyclerView.getAdapter() != null) {
                        genresRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                    Log.d(TAG, "Genres loaded: " + genres);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load custom genres", e);
                    Toast.makeText(getContext(), "Failed to load custom genres: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (genresRecyclerView.getAdapter() != null) {
                        genresRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    private void fetchNovelsFromFirestore(String genreFilter) {
        Log.d(TAG, "Fetching novels from Firestore with genre filter: " + genreFilter);
        CollectionReference novelsRef = db.collection("novels");

        novelsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    genreBooks.clear();
                    allFetchedBooks.clear();
                    continueReadingBooks.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "No novels found in Firestore");
                        Toast.makeText(getContext(), "No novels found.", Toast.LENGTH_SHORT).show();
                        updateAllAdapters();
                        return;
                    }

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " documents in Firestore");

                    SharedPreferences prefs = getUserPrefs();
                    Set<String> openedNovelIds = prefs.getStringSet(KEY_OPENED_NOVELS, new HashSet<>());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String id = document.getId();
                            String title = document.getString("title");
                            String coverUrl = document.getString("coverUrl");
                            String category = document.getString("category");
                            String author = document.getString("author");
                            String description = document.getString("description");
                            String isbn = document.getString("isbn");
                            String fileUrl = document.getString("fileUrl");

                            Log.d(TAG, "Processing document: " + id);
                            Log.d(TAG, "Title: " + title);
                            Log.d(TAG, "CoverUrl: " + coverUrl);
                            Log.d(TAG, "Category: " + category);

                            if (title == null || title.isEmpty()) {
                                Log.w(TAG, "Skipping document " + id + ": missing or empty title");
                                continue;
                            }

                            if (coverUrl == null || coverUrl.isEmpty()) {
                                Log.w(TAG, "Skipping document " + id + ": missing or empty coverUrl");
                                continue;
                            }

                            if (!isValidImageUrl(coverUrl)) {
                                Log.w(TAG, "Invalid cover URL for " + title + ": " + coverUrl);
                            }

                            Book book = new Book(id, title, coverUrl, category, author, description, isbn, fileUrl);
                            allFetchedBooks.add(book);

                            boolean shouldAddToGenre = genreFilter.equalsIgnoreCase("All");
                            if (!shouldAddToGenre && category != null) {
                                String[] categoryParts = category.split(",");
                                for (String part : categoryParts) {
                                    if (part.trim().equalsIgnoreCase(genreFilter)) {
                                        shouldAddToGenre = true;
                                        break;
                                    }
                                }
                            }

                            if (shouldAddToGenre) {
                                genreBooks.add(book);
                                Log.d(TAG, "Added book to genreBooks: " + title);
                            }

                            if (openedNovelIds.contains(id)) {
                                continueReadingBooks.add(book);
                                Log.d(TAG, "Added book to continueReadingBooks: " + title);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing document " + document.getId(), e);
                        }
                    }

                    updateAllAdapters();
                    updateWeeklyGoalUI(); // Update weekly goal UI after fetching novels
                    Log.d(TAG, "Final counts - Genre books: " + genreBooks.size() +
                            ", Recommended: " + recommendedBooks.size() +
                            ", Continue reading: " + continueReadingBooks.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load novels from Firestore", e);
                    Toast.makeText(getContext(), "Failed to load novels: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void refreshContinueReading() {
        if (getContext() == null) return;

        SharedPreferences prefs = getUserPrefs();
        Set<String> openedNovelIds = prefs.getStringSet(KEY_OPENED_NOVELS, new HashSet<>());

        continueReadingBooks.clear();
        for (Book book : allFetchedBooks) {
            if (openedNovelIds.contains(book.id)) {
                continueReadingBooks.add(book);
            }
        }

        if (continueAdapter != null) {
            continueAdapter.updateData(continueReadingBooks);
        }
        updateContinueReadingUI();
        updateWeeklyGoalUI(); // Update weekly goal UI after refreshing continue reading
        Log.d(TAG, "Refreshed Continue Reading list. Count: " + continueReadingBooks.size());
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }

            return url.contains("backblazeb2.com") ||
                    url.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing URL: " + url, e);
            return false;
        }
    }

    private void updateAllAdapters() {
        if (genreBookAdapter != null) {
            genreBookAdapter.updateData(genreBooks);
        }
        if (continueAdapter != null) {
            continueAdapter.updateData(continueReadingBooks);
        }
        updateContinueReadingUI();
        updateWeeklyGoalUI(); // Ensure weekly goal UI is updated
    }

    private class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
        private List<Book> books;

        BookAdapter(List<Book> books) {
            this.books = new ArrayList<>(books);
        }

        public void updateData(List<Book> newBooks) {
            this.books.clear();
            this.books.addAll(newBooks);
            notifyDataSetChanged();
            Log.d(TAG, "BookAdapter updated with " + newBooks.size() + " books");
        }

        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
            return new BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            Book book = books.get(position);
            holder.bookTitle.setText(book.title);

            Log.d(TAG, "Binding book: " + book.title + " with coverUrl: " + book.coverUrl);

            if (book.coverUrl != null && !book.coverUrl.isEmpty() && b2AuthorizationToken != null && b2DownloadUrl != null) {
                String cleanUrl = book.coverUrl.trim();
                if (!cleanUrl.contains("backblazeb2.com")) {
                    cleanUrl = b2DownloadUrl + "/file/novelix-novel-Uploads/" + cleanUrl;
                }

                GlideUrl glideUrl = new GlideUrl(cleanUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", b2AuthorizationToken)
                        .build());

                RequestOptions requestOptions = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .timeout(10000);

                String finalCleanUrl = cleanUrl;
                Glide.with(holder.itemView.getContext())
                        .asBitmap()
                        .load(glideUrl)
                        .apply(requestOptions)
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                        Object model,
                                                        com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                                                        boolean isFirstResource) {
                                Log.e(TAG, "Glide failed to load image for " + book.title + ", URL: " + finalCleanUrl, e);
                                if (e != null) {
                                    Log.e(TAG, "Glide error details: " + e.getMessage());
                                    for (Throwable cause : e.getRootCauses()) {
                                        Log.e(TAG, "Root cause: " + cause.getMessage());
                                        if (cause.getMessage().contains("status code: 401")) {
                                            new Thread(() -> {
                                                try {
                                                    B2Auth.B2AuthResponse authResponse = B2Auth.authorize();
                                                    b2AuthorizationToken = authResponse.authorizationToken;
                                                    b2DownloadUrl = authResponse.downloadUrl;
                                                    Log.d(TAG, "Refreshed B2 authorization token");
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(() -> {
                                                            Glide.with(holder.itemView.getContext())
                                                                    .asBitmap()
                                                                    .load(new GlideUrl(finalCleanUrl, new LazyHeaders.Builder()
                                                                            .addHeader("Authorization", b2AuthorizationToken)
                                                                            .build()))
                                                                    .apply(requestOptions)
                                                                    .into(holder.bookImage);
                                                        });
                                                    }
                                                } catch (Exception ex) {
                                                    Log.e(TAG, "Failed to refresh B2 token", ex);
                                                }
                                            }).start();
                                        }
                                    }
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.Bitmap resource,
                                                           Object model,
                                                           com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                                                           com.bumptech.glide.load.DataSource dataSource,
                                                           boolean isFirstResource) {
                                Log.d(TAG, "Glide successfully loaded image for " + book.title + ", URL: " + finalCleanUrl);
                                return false;
                            }
                        })
                        .into(holder.bookImage);
            } else {
                Log.w(TAG, "No valid cover URL or B2 auth token for " + book.title);
                holder.bookImage.setImageResource(R.drawable.itendwithus);
            }

            holder.itemView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked on book: " + book.title);
                SharedPreferences prefs = getUserPrefs();
                Set<String> openedNovelIds = new HashSet<>(prefs.getStringSet(KEY_OPENED_NOVELS, new HashSet<>()));
                boolean wasAlreadyOpened = openedNovelIds.contains(book.id);
                openedNovelIds.add(book.id);
                prefs.edit().putStringSet(KEY_OPENED_NOVELS, openedNovelIds).apply();
                if (openedNovelIds.contains(book.id)) {
                    // Book is in continue reading, open directly in NovelReaderActivity
                    if (book.fileUrl != null && !book.fileUrl.isEmpty()) {
                        new Thread(() -> {
                            try {
                                String signedUrl = getSignedUrl(book.fileUrl);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Intent intent = new Intent(getContext(), NovelReaderActivity.class);
                                        intent.putExtra("novel_url", signedUrl);
                                        intent.putExtra("novel_title", book.title);
                                        intent.putExtra("novel_id", book.id);
                                        startActivity(intent);
                                        Toast.makeText(getContext(), "Resuming: " + book.title, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error fetching signed URL for " + book.title, e);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "Error opening novel: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                                }
                            }
                        }).start();
                    } else {
                        Toast.makeText(getContext(), "No file URL available for " + book.title, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Add to opened novels and update continue reading
                    openedNovelIds.add(book.id);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putStringSet(KEY_OPENED_NOVELS, openedNovelIds);
                    editor.apply();
                    refreshContinueReading();

                    // Open in NovelDescription
                    Intent intent = new Intent(getContext(), NovelDescription.class);
                    intent.putExtra("novel_id", book.id);
                    intent.putExtra("title", book.title);
                    intent.putExtra("cover_url", book.coverUrl);
                    intent.putExtra("genre", book.genre);
                    intent.putExtra("author", book.author);
                    intent.putExtra("description", book.description);
                    intent.putExtra("isbn", book.isbn);
                    startActivity(intent);
                    Toast.makeText(getContext(), "Opening: " + book.title, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            ImageView bookImage;
            TextView bookTitle;

            BookViewHolder(@NonNull View itemView) {
                super(itemView);
                bookImage = itemView.findViewById(R.id.bookImage);
                bookTitle = itemView.findViewById(R.id.bookTitle);

                if (bookImage == null) {
                    Log.e(TAG, "bookImage not found in item_book layout");
                }
                if (bookTitle == null) {
                    Log.e(TAG, "bookTitle not found in item_book layout");
                }
            }
        }
    }

    private String getSignedUrl(String fileUrl) throws Exception {
        String fileKey;
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            String searchPrefix = "/file/novelix-novel-Uploads/";
            int startIndex = path.indexOf(searchPrefix);
            if (startIndex == -1) throw new Exception("Cannot find bucket name in file URL path.");
            fileKey = URLDecoder.decode(path.substring(startIndex + searchPrefix.length()), "UTF-8");
        } catch (Exception e) {
            throw new Exception("Could not parse file key from URL: " + fileUrl);
        }

        OkHttpClient client = new OkHttpClient();
        String credentials = B2_KEY_ID + ":" + B2_APPLICATION_KEY;
        String encodedCredentials = android.util.Base64.encodeToString(credentials.getBytes("UTF-8"), android.util.Base64.NO_WRAP);
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
            requestBodyJson.put("bucketId", "4712c78cb635a77c987d0f18");
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
                return downloadUrl + "/file/novelix-novel-Uploads/" + encodedFileKey + "?Authorization=" + downloadAuthToken;
            }
        }
    }

    private class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
        private List<String> genres;
        private int selectedPosition = 0;

        GenreAdapter(List<String> genres) {
            this.genres = genres;
        }

        @NonNull
        @Override
        public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre, parent, false);
            return new GenreViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GenreViewHolder holder, @SuppressLint("RecyclerView") int position) {
            String genre = genres.get(position);
            holder.genreName.setText(genre);
            holder.itemView.setSelected(position == selectedPosition);

            ((androidx.cardview.widget.CardView) holder.itemView).setCardElevation(
                    position == selectedPosition ? 6f : 2f
            );

            holder.itemView.setOnClickListener(v -> {
                if (position != selectedPosition) {
                    int previousPosition = selectedPosition;
                    selectedPosition = position;
                    selectedGenre = genre;

                    notifyItemChanged(previousPosition);
                    notifyItemChanged(selectedPosition);

                    if (isNetworkAvailable()) {
                        Log.d(TAG, "Genre selected: " + genre);
                        fetchNovelsFromFirestore(genre);
                        Toast.makeText(getContext(), "Selected Genre: " + genre, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return genres.size();
        }

        class GenreViewHolder extends RecyclerView.ViewHolder {
            TextView genreName;

            GenreViewHolder(@NonNull View itemView) {
                super(itemView);
                genreName = itemView.findViewById(R.id.genre_name);

                if (genreName == null) {
                    Log.e(TAG, "No TextView found in item_genre layout");
                    throw new IllegalStateException("No TextView found in item_genre layout");
                }
            }
        }
    }
}