package com.baconga.kttstore.Models;

public class MOrderDetail {
    private int orderDetailID;
    private int quantity;
    private String SKU;
    private String size;
    private int stock;
    private MProduct product;

    // Constructor
    public MOrderDetail(int orderDetailID, int quantity, String SKU, String size, int stock, MProduct product) {
        this.orderDetailID = orderDetailID;
        this.quantity = quantity;
        this.SKU = SKU;
        this.size = size;
        this.stock = stock;
        this.product = product;
    }

    // Getters
    public int getOrderDetailID() { return orderDetailID; }
    public int getQuantity() { return quantity; }
    public String getSKU() { return SKU; }
    public String getSize() { return size; }
    public int getStock() { return stock; }
    public MProduct getProduct() { return product; }

    // Product subclass
    public static class MProduct {
        private int productID;
        private String name;
        private double price;
        private String colorName;
        private String image;

        public MProduct(int productID, String name, double price, String colorName, String image) {
            this.productID = productID;
            this.name = name;
            this.price = price;
            this.colorName = colorName;
            this.image = image;
        }

        // Getters
        public int getProductID() { return productID; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getColorName() { return colorName; }
        public String getImage() { return image; }
    }
}
