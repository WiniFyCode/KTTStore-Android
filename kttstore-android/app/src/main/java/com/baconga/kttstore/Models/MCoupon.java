package com.baconga.kttstore.Models;

import java.util.List;

public class MCoupon {
    public String couponID;
    public String code;
    public String description;
    public String discountType;
    public double discountValue;
    public double minOrderValue;
    public double maxDiscountAmount;
    public String startDate;
    public String endDate;
    public int usageLimit;
    public int totalUsageLimit;
    public int usedCount;
    public boolean isActive;
    public String couponType;
    public int minimumQuantity;
    public List<String> appliedCategories;

    public MCoupon(String couponID, String code, String description, String discountType, double discountValue, double minOrderValue, double maxDiscountAmount, String startDate, String endDate, int usageLimit, int totalUsageLimit, int usedCount, boolean isActive, String couponType, int minimumQuantity, List<String> appliedCategories) {
        this.couponID = couponID;
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.totalUsageLimit = totalUsageLimit;
        this.usedCount = usedCount;
        this.isActive = isActive;
        this.couponType = couponType;
        this.minimumQuantity = minimumQuantity;
        this.appliedCategories = appliedCategories;
    }

    public String getCouponID() {
        return couponID;
    }

    public void setCouponID(String couponID) {
        this.couponID = couponID;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getTotalUsageLimit() {
        return totalUsageLimit;
    }

    public void setTotalUsageLimit(int totalUsageLimit) {
        this.totalUsageLimit = totalUsageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCouponType() {
        return couponType;
    }

    public void setCouponType(String couponType) {
        this.couponType = couponType;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public void setMinimumQuantity(int minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
    }

    public List<String> getAppliedCategories() {
        return appliedCategories;
    }

    public void setAppliedCategories(List<String> appliedCategories) {
        this.appliedCategories = appliedCategories;
    }
}
