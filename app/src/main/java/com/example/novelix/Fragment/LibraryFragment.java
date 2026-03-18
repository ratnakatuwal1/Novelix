package com.example.novelix.Fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.novelix.Adapter.SearchAdapter;
import com.example.novelix.Model.Book;
import com.example.novelix.Model.Novel;
import com.example.novelix.NovelDescription;
import com.example.novelix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class LibraryFragment extends Fragment {
    private RecyclerView novelRecyclerView;
    private RecyclerView genreRecyclerView;
    private SearchAdapter novelAdapter;
    private GenreAdapter genreAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Novel> novelList;
    private List<String> genreList;

    private static final String[] PREDEFINED_GENRES = {
            "Fiction", "Non-Fiction", "Fantasy", "Science Fiction",
            "Mystery", "Romance", "Thriller", "Historical",
            "Adventure", "Horror"
    };

    private static final String TAG = "LibraryFragment";
    private String selectedGenre = "All"; // Default filter

    public LibraryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        novelList = new ArrayList<>();

        // Default + special filters (Removed Featured)
        genreList = new ArrayList<>(Arrays.asList("All", "Trending", "New Arrivals"));

        // Add predefined static genres
        genreList.addAll(Arrays.asList(PREDEFINED_GENRES));

        // Fetch Firestore dynamic genres
        fetchGenresFromFirestore();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        novelRecyclerView = view.findViewById(R.id.novelRecyclerView);
        genreRecyclerView = view.findViewById(R.id.genreRecyclerView);

        // Set up novel RecyclerView
        novelRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        novelAdapter = new SearchAdapter(novel -> {
            Intent intent = new Intent(getContext(), NovelDescription.class);
            intent.putExtra("novel_id", novel.getId());
            startActivity(intent);
        }, novel -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Please log in to bookmark", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = auth.getCurrentUser().getUid();
            Book book = new Book(novel.getId(), novel.getTitle(), novel.getCoverUrl(), novel.getAuthor(), novel.getCategory());
            db.collection("users").document(userId).collection("bookmarks")
                    .document(novel.getId())
                    .set(book)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), novel.getTitle() + " bookmarked", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Bookmarked novel: " + novel.getTitle());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to bookmark novel: " + novel.getTitle(), e);
                    });
        });
        novelRecyclerView.setAdapter(novelAdapter);

        // Set up genre RecyclerView
        genreRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        genreAdapter = new GenreAdapter(genreList, genre -> {
            selectedGenre = genre;
            fetchNovels(); // Refetch novels based on selected genre
        });
        genreRecyclerView.setAdapter(genreAdapter);

        // Fetch novels for initial display
        fetchNovels();
    }

    private void fetchGenresFromFirestore() {
        db.collection("novels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Object categoryObj = doc.get("custom_categories");

                        if (categoryObj instanceof String) {
                            String category = (String) categoryObj;
                            if (category != null && !genreList.contains(category)) {
                                genreList.add(category);
                            }
                        } else if (categoryObj instanceof List) {
                            List<String> categories = (List<String>) categoryObj;
                            for (String category : categories) {
                                if (category != null && !genreList.contains(category)) {
                                    genreList.add(category);
                                }
                            }
                        }
                    }
                    if (genreAdapter != null) {
                        genreAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch categories: " + e.getMessage()));
    }

    private void fetchNovels() {
        Query query;
        if (selectedGenre.equals("All")) {
            query = db.collection("novels");
        } else if (selectedGenre.equals("Trending")) {
            // Sort by a combined metric of searchCount and readCount
            query = db.collection("novels").orderBy("searchCount", Query.Direction.DESCENDING);
        } else if (selectedGenre.equals("New Arrivals")) {
            query = db.collection("novels").orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            // Handle both string and array field for custom_categories
            query = db.collection("novels").whereArrayContains("custom_categories", selectedGenre);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    novelList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Novel novel = document.toObject(Novel.class);
                        novel.setId(document.getId());
                        novelList.add(novel);
                    }
                    if (selectedGenre.equals("Trending")) {
                        // Sort by combined searchCount and readCount
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            novelList.sort((n1, n2) -> {
                                long score1 = (n1.getSearchCount() != null ? n1.getSearchCount() : 0) +
                                        (n1.getReadCount() != null ? n1.getReadCount() : 0);
                                long score2 = (n2.getSearchCount() != null ? n2.getSearchCount() : 0) +
                                        (n2.getReadCount() != null ? n2.getReadCount() : 0);
                                return Long.compare(score2, score1); // Descending order
                            });
                        }
                    }
                    novelAdapter.setNovelList(novelList);
                    if (novelList.isEmpty()) {
                        Toast.makeText(getContext(), "No novels found for " + selectedGenre, Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "Fetched " + novelList.size() + " novels for " + selectedGenre);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load novels: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load novels: " + e.getMessage(), e);
                });
    }

    // Genre Adapter for the Horizontal RecyclerView
    private static class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.ViewHolder> {
        private final List<String> genres;
        private final Consumer<String> onGenreClickListener;

        public GenreAdapter(List<String> genres, Consumer<String> onGenreClickListener) {
            this.genres = genres;
            this.onGenreClickListener = onGenreClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String genre = genres.get(position);
            holder.genreName.setText(genre);
            holder.itemView.setOnClickListener(v -> {
                if (onGenreClickListener != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onGenreClickListener.accept(genre);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return genres.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView genreName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                genreName = itemView.findViewById(R.id.genre_name);
            }
        }
    }
}
