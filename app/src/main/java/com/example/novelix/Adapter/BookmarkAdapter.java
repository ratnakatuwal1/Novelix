package com.example.novelix.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.novelix.Fragment.BookmarkFragment;
import com.example.novelix.R;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookViewHolder> {
    private List<BookmarkFragment.Book> bookList;
    public BookmarkAdapter(List<BookmarkFragment.Book> bookList) {
        this.bookList = bookList;
    }

    @NonNull
    @Override
    public BookmarkAdapter.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkAdapter.BookViewHolder holder, int position) {
        BookmarkFragment.Book book = bookList.get(position);
        holder.bookImage.setImageResource(book.getImageResId());
        holder.bookTitle.setText(book.getTitle());
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView bookImage;
        TextView bookTitle;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            bookImage = itemView.findViewById(R.id.bookImage);
            bookTitle = itemView.findViewById(R.id.bookTitle);
        }
    }
}
