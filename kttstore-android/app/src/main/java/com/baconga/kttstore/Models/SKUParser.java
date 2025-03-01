package com.baconga.kttstore.Models;

public class SKUParser {
    private String productID;
    private String colorID; 
    private String size;
    private String sizeStockID;
    private String fullSKU;

    public SKUParser(String sku) {
        this.fullSKU = sku;
        String[] parts = sku.split("_");
        if (parts.length == 4) {
            this.productID = parts[0];
            this.colorID = parts[1];
            this.size = parts[2];
            this.sizeStockID = parts[3];
        }
    }

    public static String createSKU(String productID, String colorID, String size, String sizeStockID) {
        return productID + "_" + colorID + "_" + size + "_" + sizeStockID;
    }

    public String getProductID() {
        return productID;
    }

    public String getColorID() {
        return colorID;
    }

    public String getSize() {
        return size;
    }

    public String getSizeStockID() {
        return sizeStockID;
    }

    public String getFullSKU() {
        return fullSKU;
    }
} 