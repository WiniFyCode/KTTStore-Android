package com.baconga.kttstore.Models;

public class FavoriteItem {
    private String _id;
    private int favoriteID;
    private String note;
    private Product product;

    // Inner Product class
    public static class Product {
        private int productID;
        private String name;
        private double price;
        private String thumbnail;
        private Promotion promotion;

        // Inner Promotion class
        public static class Promotion {
            private int discountPercent;
            private String endDate;
            private double finalPrice;

            // Constructor
            public Promotion(int discountPercent, String endDate, double finalPrice) {
                this.discountPercent = discountPercent;
                this.endDate = endDate;
                this.finalPrice = finalPrice;
            }

            // Getters
            public int getDiscountPercent() { return discountPercent; }
            public String getEndDate() { return endDate; }
            public double getFinalPrice() { return finalPrice; }
        }

        // Constructor
        public Product(int productID, String name, double price, String thumbnail, Promotion promotion) {
            this.productID = productID;
            this.name = name;
            this.price = price;
            this.thumbnail = thumbnail;
            this.promotion = promotion;
        }

        // Getters
        public int getProductID() { return productID; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getThumbnail() { return thumbnail; }
        public Promotion getPromotion() { return promotion; }
        
        // Helper method để lấy giá cuối cùng
        public double getFinalPrice() {
            return promotion != null ? promotion.getFinalPrice() : price;
        }
        
        // Helper method để kiểm tra có khuyến mãi không
        public boolean hasPromotion() {
            return promotion != null;
        }
    }

    // Constructor
    public FavoriteItem(String _id, int favoriteID, Product product, String note) {
        this._id = _id;
        this.favoriteID = favoriteID;
        this.product = product;
        this.note = note;
    }

    // Getters
    public String getId() { return _id; }
    public int getFavoriteID() { return favoriteID; }
    public Product getProduct() { return product; }
    public String getNote() { return note; }

    // Setter for note
    public void setNote(String note) { this.note = note; }
} 