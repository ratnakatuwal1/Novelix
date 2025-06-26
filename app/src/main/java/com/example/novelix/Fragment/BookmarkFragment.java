package com.example.novelix.Fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.novelix.Adapter.BookmarkAdapter;
import com.example.novelix.R;

import java.util.ArrayList;
import java.util.List;

public class BookmarkFragment extends Fragment {
    private RecyclerView recyclerView;
    private BookmarkAdapter adapter;
    private List<Book> bookList;

    public BookmarkFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize sample book data (replace with actual data source)
        bookList = new ArrayList<>();
        bookList.add(new Book(R.drawable.itendwithus, "It Ends With Us"));
        bookList.add(new Book(R.drawable.itendwithus, "Book Title 2"));
        bookList.add(new Book(R.drawable.itendwithus, "Book Title 3"));
        bookList.add(new Book(R.drawable.itendwithus, "Book Title 4"));
        bookList.add(new Book(R.drawable.itendwithus, "Book Title 5"));
        bookList.add(new Book(R.drawable.itendwithus, "Book Title 6"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        recyclerView = view.findViewById(R.id.bookmarkRecyclerView);
        adapter = new BookmarkAdapter(bookList);
        recyclerView.setAdapter(adapter);
        return view;
    }

    public static class Book {
        private int imageResId;
        private String title;

        public Book(int imageResId, String title) {
            this.imageResId = imageResId;
            this.title = title;
        }

        public int getImageResId() {
            return imageResId;
        }

        public String getTitle() {
            return title;
        }
    }
}