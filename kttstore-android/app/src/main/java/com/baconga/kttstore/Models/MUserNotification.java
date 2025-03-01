package com.baconga.kttstore.Models;

public class MUserNotification {
    public String userNotificationID;
    public String notificationID;
    public String userID;
    public boolean isRead;
    public String readAt;

    public MUserNotification(String userNotificationID, String notificationID, String userID, boolean isRead, String readAt) {
        this.userNotificationID = userNotificationID;
        this.notificationID = notificationID;
        this.userID = userID;
        this.isRead = isRead;
        this.readAt = readAt;
    }

    public String getUserNotificationID() {
        return userNotificationID;
    }

    public void setUserNotificationID(String userNotificationID) {
        this.userNotificationID = userNotificationID;
    }

    public String getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(String notificationID) {
        this.notificationID = notificationID;
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
}
