package com.baconga.kttstore.Models;

public class MBanner {
    private int imageResId;
    private String title;
    private String description;

    public MBanner(int imageResId, String title, String description) {
        this.imageResId = imageResId;
        this.title = title;
        this.description = description;
    }

    // Getters
    public int getImageResId() { return imageResId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
} 