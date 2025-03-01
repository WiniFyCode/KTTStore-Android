package com.baconga.kttstore.Models;

public class CartItem {
    private MCart cart;
    private String productName;
    private String colorName;
    private String imageUrl;
    private double originalPrice;
    private Double discountPrice;
    private int stock;
    private Double finalPrice;
    private boolean isSelected;

    public CartItem(MCart cart, String productName, String colorName, 
                   String imageUrl, double originalPrice, Double discountPrice, int stock) {
        this.cart = cart;
        this.productName = productName;
        this.colorName = colorName;
        this.imageUrl = imageUrl;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.stock = stock;
        this.finalPrice = discountPrice != null ? discountPrice : originalPrice;
        this.isSelected = false;
    }

    // Getters và setters
    public MCart getCart() {
        return cart;
    }

    public String getProductName() {
        return productName;
    }

    public String getColorName() {
        return colorName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public Double getFinalPrice() {
        return finalPrice;
    }

    public int getStock() {
        return stock;
    }

    public double getSubtotal() {
        return finalPrice * cart.getQuantity();
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
        this.finalPrice = discountPrice != null ? discountPrice : originalPrice;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getProductId() {
        return cart.getProductId();
    }

    public String getColorId() {
        return cart.getColorId();
    }

    public String getSizeId() {
        return cart.getSizeId();
    }

    public String getSizestockId() {
        return cart.getSizestockId();
    }
} 