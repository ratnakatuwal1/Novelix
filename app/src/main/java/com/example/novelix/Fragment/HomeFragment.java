package com.example.novelix.Fragment;

import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.novelix.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private List<String> genres;
    private List<Book> recommendedBooks;
    private List<Book> continueReadingBooks;

    private static class Book {
        String title;
        int imageResId;

        Book(String title, int imageResId) {
            this.title = title;
            this.imageResId = imageResId;
        }
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        setupGenreClickListeners(view);
        setupBookDisplays(view);
        return view;
    }

    private void initializeData() {
        genres = new ArrayList<>();
        genres.add("All");
        genres.add("Romance");
        genres.add("Crime");
        genres.add("Fantasy");
        genres.add("Sci-Fi");

        recommendedBooks = new ArrayList<>();
        recommendedBooks.add(new Book("Book 1", R.drawable.itendwithus));
        recommendedBooks.add(new Book("Book 2", R.drawable.itendwithus));
        recommendedBooks.add(new Book("Book 3", R.drawable.itendwithus));
        recommendedBooks.add(new Book("Book 4", R.drawable.itendwithus));
        recommendedBooks.add(new Book("Book 5", R.drawable.itendwithus));

        continueReadingBooks = new ArrayList<>();
        continueReadingBooks.add(new Book("Continue 1", R.drawable.itendwithus));
        continueReadingBooks.add(new Book("Continue 2", R.drawable.itendwithus));
        continueReadingBooks.add(new Book("Continue 3", R.drawable.itendwithus));
        continueReadingBooks.add(new Book("Continue 4", R.drawable.itendwithus));
        continueReadingBooks.add(new Book("Continue 5", R.drawable.itendwithus));
    }

    private void setupGenreClickListeners(View view) {
        int[] genreCardIds = {R.id.cardView2, R.id.cardView3, R.id.cardView4, R.id.cardView5, R.id.cardView6};
        for (int i = 0; i < genreCardIds.length; i++) {
            final int index = i;
            CardView genreCard = view.findViewById(genreCardIds[i]);
            genreCard.setOnClickListener(v -> {
                String selectedGenre = genres.get(index);
                // Handle genre selection here
                Toast.makeText(getActivity(), "Selected Genre: " + selectedGenre, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBookDisplays(View view) {
        int[] recommendedCardIds = {R.id.cardView7, R.id.cardView8, R.id.cardView9, R.id.cardView10, R.id.cardView11};
        int[] recommendedImageIds = {R.id.imageView5, R.id.imageView6, R.id.imageView7, R.id.imageView8, R.id.imageView9};

        for (int i = 0; i < recommendedCardIds.length; i++) {
            CardView card = view.findViewById(recommendedCardIds[i]);
            ImageView imageView = view.findViewById(recommendedImageIds[i]);
            Book book = recommendedBooks.get(i);
            imageView.setImageResource(book.imageResId);
            card.setOnClickListener(v -> {
                // Handle book click here
                Toast.makeText(getActivity(), "Clicked on: " + book.title, Toast.LENGTH_SHORT).show();
            });
        }

        int[] continueCardIds = {R.id.cardView12, R.id.cardView13, R.id.cardView14, R.id.cardView15, R.id.cardView16};
        int[] continueImageIds = {R.id.imageView17, R.id.imageView18, R.id.imageView19, R.id.imageView20, R.id.imageView21};

        for (int i = 0; i < continueCardIds.length; i++) {
            CardView card = view.findViewById(continueCardIds[i]);
            ImageView imageView = view.findViewById(continueImageIds[i]);
            Book book = continueReadingBooks.get(i);
            imageView.setImageResource(book.imageResId);
            card.setOnClickListener(v -> {
                // Handle book click here
                Toast.makeText(getActivity(), "Clicked on: " + book.title, Toast.LENGTH_SHORT).show();
            });
        }

        // Setup genre books
        int[] genreBookCardIds = {R.id.cardView130, R.id.cardView131};
        int[] genreBookImageIds = {R.id.imageView180, R.id.imageView181};

        for (int i = 0; i < genreBookCardIds.length; i++) {
            CardView card = view.findViewById(genreBookCardIds[i]);
            ImageView imageView = view.findViewById(genreBookImageIds[i]);
            Book book = recommendedBooks.get(i); // Using recommended books as placeholder

            imageView.setImageResource(book.imageResId);
            card.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Selected book: " + book.title,
                        Toast.LENGTH_SHORT).show();
                // Add logic to open book details
            });
        }
    }
}