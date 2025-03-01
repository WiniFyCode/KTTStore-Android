package com.baconga.kttstore.Models;

import java.io.Serializable;

public class MOrder implements Serializable {
    private String _id;
    private int orderID;
    private long userID;
    private String fullname;
    private String phone;
    private String address;
    private double totalPrice;
    private double paymentPrice;
    private String orderStatus;
    private String shippingStatus;
    private boolean isPayed;
    private String createdAt;
    private String updatedAt;

    // Constructor
    public MOrder(String _id, int orderID, long userID, String fullname, String phone, String address,
                  double totalPrice, double paymentPrice, String orderStatus, String shippingStatus,
                  boolean isPayed, String createdAt, String updatedAt) {
        this._id = _id;
        this.orderID = orderID;
        this.userID = userID;
        this.fullname = fullname;
        this.phone = phone;
        this.address = address;
        this.totalPrice = totalPrice;
        this.paymentPrice = paymentPrice;
        this.orderStatus = orderStatus;
        this.shippingStatus = shippingStatus;
        this.isPayed = isPayed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter & Setter
    public String getId() { return _id; }
    public int getOrderID() { return orderID; }
    public long getUserID() { return userID; }
    public String getFullname() { return fullname; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public double getTotalPrice() { return totalPrice; }
    public double getPaymentPrice() { return paymentPrice; }
    public String getOrderStatus() { return orderStatus; }
    public String getShippingStatus() { return shippingStatus; }
    public boolean isPayed() { return isPayed; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
