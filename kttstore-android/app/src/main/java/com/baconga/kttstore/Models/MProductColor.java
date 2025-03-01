package com.baconga.kttstore.Models;

public class MProductColor {
    public String colorID;
    public String productID;
    public String colorName;
    public String[] images;

    public MProductColor(String colorID, String productID, String colorName, String[] images) {
        this.colorID = colorID;
        this.productID = productID;
        this.colorName = colorName;
        this.images = images;
    }

    public String getColorID() {
        return colorID;
    }

    public void setColorID(String colorID) {
        this.colorID = colorID;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }
}
