package com.example.novelix.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.novelix.Adapter.SearchAdapter;
import com.example.novelix.Model.Novel;
import com.example.novelix.NovelDescription;
import com.example.novelix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchFragment extends Fragment {
    private SearchView searchView;
    private RecyclerView recycler_search_results;
    private ProgressBar searchProgressBar;
    private TextView noResultsText;
    private SearchAdapter searchAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Novel> cachedNovels;
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private String currentSearchQuery = "";
    private static final long DEBOUNCE_DELAY_MS = 300;
    private static final int LEVENSHTEIN_THRESHOLD = 2;
    private static final String TAG = "SearchFragment";

    public SearchFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        cachedNovels = new ArrayList<>();
        debounceHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchView = view.findViewById(R.id.searchView);
        recycler_search_results = view.findViewById(R.id.recycler_search_results);
        searchProgressBar = view.findViewById(R.id.searchProgressBar);
        noResultsText = view.findViewById(R.id.noResultsText);

        recycler_search_results.setLayoutManager(new GridLayoutManager(getContext(), 1));
        searchAdapter = new SearchAdapter(novel -> {
            Intent intent = new Intent(getContext(), NovelDescription.class);
            intent.putExtra("novel_id", novel.getId());
            startActivity(intent);
        }, novel -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Please log in to bookmark", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId).collection("bookmarks")
                    .document(novel.getId())
                    .set(new HashMap<String, Object>())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), novel.getTitle() + " bookmarked", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Bookmarked novel: " + novel.getTitle());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to bookmark novel: " + novel.getTitle(), e);
                    });
        });
        recycler_search_results.setAdapter(searchAdapter);

        // Remove underline from SearchView
        View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackground(null);
        }

        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            searchEditText.setTextColor(getResources().getColor(R.color.PrimaryText));
            searchEditText.setHintTextColor(getResources().getColor(R.color.SecondaryText));
            searchEditText.setTextSize(16);
            searchEditText.setHint("Search novels...");
            searchEditText.setBackgroundResource(android.R.color.transparent);
            searchEditText.setPadding(16, 16, 16, 16);

            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String query = s.toString().trim();
                    Log.d(TAG, "Search query entered: " + query);
                    debounceHandler.removeCallbacks(debounceRunnable);
                    debounceRunnable = () -> {
                        currentSearchQuery = query;
                        searchProgressBar.setVisibility(View.VISIBLE);
                        noResultsText.setVisibility(View.GONE);
                        filterNovels(query);
                        searchProgressBar.setVisibility(View.GONE);
                    };
                    debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
                }
            });
        } else {
            Log.e(TAG, "SearchView EditText not found");
        }

        fetchNovels();
    }

    private void fetchNovels() {
        searchProgressBar.setVisibility(View.VISIBLE);
        noResultsText.setVisibility(View.GONE);

        db.collection("novels").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cachedNovels.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Novel novel = document.toObject(Novel.class);
                        novel.setId(document.getId());
                        cachedNovels.add(novel);
                    }
                    if (cachedNovels.isEmpty()) {
                        noResultsText.setVisibility(View.VISIBLE);
                        recycler_search_results.setVisibility(View.GONE);
                    } else {
                        noResultsText.setVisibility(View.GONE);
                        recycler_search_results.setVisibility(View.VISIBLE);
                        filterNovels(currentSearchQuery);
                    }
                    searchProgressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load novels: " + e.getMessage(), e);
                    noResultsText.setVisibility(View.VISIBLE);
                    recycler_search_results.setVisibility(View.GONE);
                    searchProgressBar.setVisibility(View.GONE);
                });
    }

    private void filterNovels(String query) {
        List<Novel> filteredNovels = new ArrayList<>();
        if (query.isEmpty()) {
            searchAdapter.setNovelList(filteredNovels);
            noResultsText.setVisibility(View.VISIBLE);
            recycler_search_results.setVisibility(View.GONE);
            return;
        }

        String queryLower = query.toLowerCase();
        for (Novel novel : cachedNovels) {
            if (isMatch(novel, queryLower)) {
                filteredNovels.add(novel);
            }
        }

        searchAdapter.setNovelList(filteredNovels);
        if (filteredNovels.isEmpty()) {
            noResultsText.setVisibility(View.VISIBLE);
            recycler_search_results.setVisibility(View.GONE);
        } else {
            noResultsText.setVisibility(View.GONE);
            recycler_search_results.setVisibility(View.VISIBLE);
        }
    }
    private boolean isMatch(Novel novel, String queryLower) {
        String title = safeLower(novel.getTitle());
        String author = safeLower(novel.getAuthor());
        String category = safeLower(novel.getCategory());
        String language = safeLower(novel.getLanguage());

        // Direct substring match
        if (title.contains(queryLower) || author.contains(queryLower)
                || category.contains(queryLower) || language.contains(queryLower)) {
            return true;
        }

        // Fuzzy match for short queries (e.g., typo handling)
        if (queryLower.length() <= 10) {
            return isFuzzyMatch(title, queryLower, LEVENSHTEIN_THRESHOLD)
                    || isFuzzyMatch(author, queryLower, LEVENSHTEIN_THRESHOLD)
                    || isFuzzyMatch(category, queryLower, LEVENSHTEIN_THRESHOLD)
                    || isFuzzyMatch(language, queryLower, LEVENSHTEIN_THRESHOLD);
        }

        return false;
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private boolean isFuzzyMatch(String source, String target, int maxDistance) {
        if (source == null || target == null) return false;
        source = source.toLowerCase();
        target = target.toLowerCase();

        if (source.contains(target)) return true;

        int len1 = source.length();
        int len2 = target.length();
        if (len2 == 0) return true;
        if (Math.abs(len1 - len2) > maxDistance) return false;

        int[] dp = new int[len2 + 1];
        for (int j = 0; j <= len2; j++) dp[j] = j;

        for (int i = 1; i <= len1; i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= len2; j++) {
                int temp = dp[j];
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                dp[j] = Math.min(Math.min(dp[j] + 1, dp[j - 1] + 1), prev + cost);
                prev = temp;
            }
        }

        return dp[len2] <= maxDistance;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceHandler != null) debounceHandler.removeCallbacks(debounceRunnable);
    }
}
