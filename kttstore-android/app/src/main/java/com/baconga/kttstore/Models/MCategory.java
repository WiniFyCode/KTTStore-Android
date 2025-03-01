package com.baconga.kttstore.Models;

import java.io.Serializable;

public class MCategory implements Serializable {
    private String categoryID;
    private String name;
    private String description;
    private String imageURL;

    public MCategory(String categoryID, String name, String description, String imageURL) {
        this.categoryID = categoryID;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
    }

    public String getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(String categoryID) {
        this.categoryID = categoryID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
