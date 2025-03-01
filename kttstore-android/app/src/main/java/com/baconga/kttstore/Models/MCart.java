package com.baconga.kttstore.Models;

public class MCart {
    private String cartID;
    private String productId;
    private String colorId;
    private String sizeId;
    private String sizestockId;
    private String size;
    private int quantity;

    public MCart(String cartID, String productId, String colorId, String sizeId, String sizestockId, String size, int quantity) {
        this.cartID = cartID;
        this.productId = productId;
        this.colorId = colorId;
        this.sizeId = sizeId;
        this.sizestockId = sizestockId;
        this.size = size;
        this.quantity = quantity;
    }

    public MCart(String cartId, int quantity) {
    }

    public String getId() {
        return cartID;
    }

    public String getCartID() {
        return cartID;
    }

    public String getProductId() {
        return productId;
    }

    public String getColorId() {
        return colorId;
    }

    public String getSizeId() {
        return sizeId;
    }

    public String getSizestockId() {
        return sizestockId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
