package com.baconga.kttstore.Models;

import org.json.JSONObject;

public class MProduct {
    private String productID;
    private String name;
    private String targetID;
    private String description;
    private double price;
    private String categoryID;
    private String thumbnail;
    private boolean isActivated;
    private int totalStock;
    private boolean inStock;
    private Promotion promotion;
    
    // Thêm các trường cho promotion details
    private String promotionName;
    private int discountPercent;
    private double discountedPrice;
    private String promotionEndDate;

    // Constructor cho CategoryDetailFragment
    public MProduct(String productID, String name, String targetID, String description, 
                   double price, String categoryID, String thumbnail, boolean isActivated) {
        this.productID = productID;
        this.name = name;
        this.targetID = targetID;
        this.description = description;
        this.price = price;
        this.categoryID = categoryID;
        this.thumbnail = thumbnail;
        this.isActivated = isActivated;
        this.totalStock = 0;
        this.inStock = false;
    }

    // Constructor cho API response
    public MProduct(JSONObject product) {
        try {
            this.productID = String.valueOf(product.getInt("productID"));
            this.name = product.getString("name");
            this.price = product.getDouble("price");
            this.thumbnail = product.getString("thumbnail");
            this.isActivated = product.getBoolean("isActivated");
            this.totalStock = product.getInt("totalStock");
            this.inStock = product.getBoolean("inStock");
            
            if (!product.isNull("promotion")) {
                JSONObject promoObj = product.getJSONObject("promotion");
                this.promotion = new Promotion(
                    promoObj.getInt("discountPercent"),
                    promoObj.getString("finalPrice")
                );
                // Set promotion details
                this.discountPercent = promoObj.getInt("discountPercent");
                this.promotionName = promoObj.optString("name", "");
                this.promotionEndDate = promoObj.optString("endDate", "");
                String finalPrice = promoObj.getString("finalPrice");
                this.discountedPrice = Double.parseDouble(finalPrice);
            }

            this.targetID = product.optString("targetID", "");
            this.description = product.optString("description", "");
            this.categoryID = product.optString("categoryID", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(String categoryID) {
        this.categoryID = categoryID;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public boolean getInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    // Thêm getter/setter cho promotion details
    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(double discountedPrice) { this.discountedPrice = discountedPrice; }

    public String getPromotionEndDate() { return promotionEndDate; }
    public void setPromotionEndDate(String promotionEndDate) { this.promotionEndDate = promotionEndDate; }

    // Inner class Promotion
    public static class Promotion {
        private int discountPercent;
        private String finalPrice;

        public Promotion(int discountPercent, String finalPrice) {
            this.discountPercent = discountPercent;
            this.finalPrice = finalPrice;
        }

        public int getDiscountPercent() { return discountPercent; }
        public String getFinalPrice() { return finalPrice; }
    }
}
