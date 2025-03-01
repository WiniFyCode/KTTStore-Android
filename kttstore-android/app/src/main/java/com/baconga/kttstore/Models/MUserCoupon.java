package com.baconga.kttstore.Models;

import java.util.Date;
import java.util.List;

public class MUserCoupon {
    public String userCouponsID;
    public String couponID;
    public String userID;
    public boolean isRead;
    public String readAt;
    public int usageLeft;
    public boolean isExpired;
    public String status;
    public List<UsageHistory> usageHistory;
    public Date expiryDate;

    public static class UsageHistory {
        public String orderID;
        public Date usedAt;
        public int discountAmount;
    }

    public MUserCoupon(String userCouponsID, String couponID, String userID, boolean isRead, String readAt, int usageLeft, boolean isExpired, String status, List<UsageHistory> usageHistory, Date expiryDate) {
        this.userCouponsID = userCouponsID;
        this.couponID = couponID;
        this.userID = userID;
        this.isRead = isRead;
        this.readAt = readAt;
        this.usageLeft = usageLeft;
        this.isExpired = isExpired;
        this.status = status;
        this.usageHistory = usageHistory;
        this.expiryDate = expiryDate;
    }

    public String getUserCouponsID() {
        return userCouponsID;
    }

    public void setUserCouponsID(String userCouponsID) {
        this.userCouponsID = userCouponsID;
    }

    public String getCouponID() {
        return couponID;
    }

    public void setCouponID(String couponID) {
        this.couponID = couponID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getReadAt() {
        return readAt;
    }

    public void setReadAt(String readAt) {
        this.readAt = readAt;
    }

    public int getUsageLeft() {
        return usageLeft;
    }

    public void setUsageLeft(int usageLeft) {
        this.usageLeft = usageLeft;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<UsageHistory> getUsageHistory() {
        return usageHistory;
    }

    public void setUsageHistory(List<UsageHistory> usageHistory) {
        this.usageHistory = usageHistory;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
}
