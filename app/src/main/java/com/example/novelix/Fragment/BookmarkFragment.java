package com.example.novelix.Fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.novelix.Adapter.BookmarkAdapter;
import com.example.novelix.Model.Book;
import com.example.novelix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookmarkFragment extends Fragment {
    private RecyclerView recyclerView;
    private BookmarkAdapter adapter;
    private List<Book> bookList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String TAG = "BookmarkFragment";
    private boolean isFetching = false;

    public BookmarkFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        bookList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        recyclerView = view.findViewById(R.id.bookmarkRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookmarkAdapter(bookList);
        recyclerView.setAdapter(adapter);

        fetchBookmarkedBooks();
        return view;
    }

    private void fetchBookmarkedBooks() {
        if (isFetching || auth.getCurrentUser() == null) {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Please log in to view bookmarks", Toast.LENGTH_SHORT).show();
                bookList.clear();
                adapter.notifyDataSetChanged();
            }
            return;
        }

        isFetching = true;
        String userId = auth.getCurrentUser().getUid();
        Set<String> bookIds = new HashSet<>();
        List<Book> tempBookList = new ArrayList<>();
        db.collection("users").document(userId).collection("bookmarks")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tempBookList.clear();
                        bookIds.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId(); // Bookmark stores only the bookId

                            if (bookIds.contains(bookId)) {
                                android.util.Log.w(TAG, "Duplicate book ID found: " + bookId);
                                continue; // Skip duplicates
                            }
                            bookIds.add(bookId);

                            // Fetch full metadata from "novels" collection
                            db.collection("novels").document(bookId)
                                    .get()
                                    .addOnSuccessListener(novelDoc -> {
                                        if (novelDoc.exists()) {
                                            Book book = novelDoc.toObject(Book.class);
                                            if (book != null) {
                                                book.setId(novelDoc.getId());
                                                tempBookList.add(book);
                                                android.util.Log.d(TAG, "Loaded book: " + book.getTitle() + " | Category: " + book.getCategory());
                                            }
                                        }
                                        if (tempBookList.size() == bookIds.size()) {
                                            updateBookList(tempBookList);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e(TAG, "Failed to fetch novel details for " + bookId, e);
                                        // Still attempt to update UI if this is the last expected book
                                        if (tempBookList.size() + 1 == bookIds.size()) {
                                            updateBookList(tempBookList);
                                        }
                                    });
                        }

                        // If no bookmarks, update UI immediately
                        if (task.getResult().isEmpty()) {
                            updateBookList(tempBookList);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching bookmarks: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        android.util.Log.e(TAG, "Error fetching bookmarks", task.getException());
                        isFetching = false;
                    }
                });
    }

    private void updateBookList(List<Book> newBookList) {
        bookList.clear();
        bookList.addAll(newBookList);
        adapter.notifyDataSetChanged();
        isFetching = false;
        android.util.Log.d(TAG, "Updated book list with " + bookList.size() + " books");
    }


    @Override
    public void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            fetchBookmarkedBooks();
        } else {
            bookList.clear();
            adapter.notifyDataSetChanged();
        }
    }
}