package com.baconga.kttstore.Models;

public class MAddress {
    public String addressID;
    public String userID;
    public String address;
    public boolean isDefault;
    public boolean isDelete;

    public MAddress(String addressID, String userID, String address, boolean isDefault, boolean isDelete) {
        this.addressID = addressID;
        this.userID = userID;
        this.address = address;
        this.isDefault = isDefault;
        this.isDelete = isDelete;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getAddressID() {
        return addressID;
    }

    public void setAddressID(String addressID) {
        this.addressID = addressID;
    }
}
