package com.baconga.kttstore.Models;

public class Address {
    private long addressID;
    private long userID;
    private String address;
    private boolean isDefault;
    private boolean isDelete;
    private String createdAt;
    private String updatedAt;

    public Address() {
    }

    public Address(long addressID, long userID, String address, boolean isDefault, boolean isDelete, String createdAt, String updatedAt) {
        this.addressID = addressID;
        this.userID = userID;
        this.address = address;
        this.isDefault = isDefault;
        this.isDelete = isDelete;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public long getAddressID() {
        return addressID;
    }

    public long getUserID() {
        return userID;
    }

    public String getAddress() {
        return address;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setAddressID(long addressID) {
        this.addressID = addressID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setDelete(boolean isDelete) {
        this.isDelete = isDelete;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
} 