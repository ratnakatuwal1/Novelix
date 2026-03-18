package com.example.novelix.Model;

import com.google.firebase.firestore.PropertyName;

public class Book {
    private String id;
    private String title;
    private String coverUrl;
    private String author;
    private String category = "Unknown"; // Default value to prevent null

    public Book() {
        // Required empty constructor for Firestore
    }

    public Book(String id, String title, String coverUrl, String author, String category) {
        this.id = id;
        this.title = title;
        this.coverUrl = coverUrl;
        this.author = author;
        this.category = category != null ? category : "Unknown";
    }

    @PropertyName("id")
    public String getId() { return id; }

    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("title")
    public String getTitle() { return title; }

    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("coverUrl")
    public String getCoverUrl() { return coverUrl; }

    @PropertyName("coverUrl")
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    @PropertyName("author")
    public String getAuthor() { return author; }

    @PropertyName("author")
    public void setAuthor(String author) { this.author = author; }

    @PropertyName("category")
    public String getCategory() { return category; }

    @PropertyName("category")
    public void setCategory(String category) { this.category = category != null ? category : "Unknown"; }
}