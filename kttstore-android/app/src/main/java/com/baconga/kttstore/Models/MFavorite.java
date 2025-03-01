package com.baconga.kttstore.Models;

public class MFavorite {
    public String favoriteID;
    public String userID;
    public String SKU;
    public String note;
    public String addedAt;

    public MFavorite(String favoriteID, String userID, String SKU, String note, String addedAt) {
        this.favoriteID = favoriteID;
        this.userID = userID;
        this.SKU = SKU;
        this.note = note;
        this.addedAt = addedAt;
    }

    public String getFavoriteID() {
        return favoriteID;
    }

    public void setFavoriteID(String favoriteID) {
        this.favoriteID = favoriteID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getSKU() {
        return SKU;
    }

    public void setSKU(String SKU) {
        this.SKU = SKU;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }
}
