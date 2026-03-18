package com.example.novelix.Model;

public class Novel {
    private String id;
    private String title;
    private String author;
    private String category;
    private String coverUrl;
    private String description;
    private String fileType;
    private String fileUrl;
    private String isbn;
    private String language;
    private double rating;
    private long timestamp;
    private Long searchCount;
    private Long readCount;

    // Empty constructor for Firebase
    public Novel() {}

    // Constructor with fields
    public Novel(String id, String title, String author, String category, String coverUrl, String description,
                 String fileType, String fileUrl, String isbn, String language, double rating, long timestamp, Long searchCount, Long readCount) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.coverUrl = coverUrl;
        this.description = description;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
        this.isbn = isbn;
        this.language = language;
        this.rating = rating;
        this.timestamp = timestamp;
        this.searchCount = searchCount;
        this.readCount = readCount;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Long getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(Long searchCount) {
        this.searchCount = searchCount;
    }

    public Long getReadCount() {
        return readCount;
    }

    public void setReadCount(Long readCount) {
        this.readCount = readCount;
    }
}
