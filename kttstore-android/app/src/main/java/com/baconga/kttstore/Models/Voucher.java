package com.baconga.kttstore.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Voucher {
    private String code;
    private String description;
    private String discountType; // percentage hoặc fixed
    private double discountValue;
    private double minOrderValue;
    private double maxDiscountAmount;
    private Date startDate;
    private Date endDate;
    private int usageLeft;
    private boolean isActive;

    public Voucher(String code, String description, String discountType, double discountValue,
                  double minOrderValue, double maxDiscountAmount, Date startDate, Date endDate,
                  int usageLeft, boolean isActive) {
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLeft = usageLeft;
        this.isActive = isActive;
    }

    // Getters
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getMinOrderValue() { return minOrderValue; }
    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public int getUsageLeft() { return usageLeft; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setCode(String code) { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
    public void setMaxDiscountAmount(double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public void setUsageLeft(int usageLeft) { this.usageLeft = usageLeft; }
    public void setActive(boolean active) { isActive = active; }

    // Helper methods
    public boolean isExpired() {
        return new Date().after(endDate);
    }

    public boolean isUsable() {
        Date now = new Date();
        return isActive && now.after(startDate) && now.before(endDate) && usageLeft > 0;
    }

    public String getFormattedDiscountValue() {
        if (discountType.equals("percentage")) {
            return String.format("%.0f%%", discountValue);
        } else {
            return String.format("%,.0fđ", discountValue);
        }
    }

    public String getFormattedMinOrderValue() {
        return String.format("Đơn tối thiểu %,.0fđ", minOrderValue);
    }

    public String getFormattedMaxDiscount() {
        return String.format("Giảm tối đa %,.0fđ", maxDiscountAmount);
    }

    public String getFormattedExpiry() {
        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", new Locale("vi", "VN"));
        outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return outputFormat.format(endDate);
    }

    public String getFormattedStartDate() {
        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", new Locale("vi", "VN")); 
        outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return outputFormat.format(startDate);
    }

}