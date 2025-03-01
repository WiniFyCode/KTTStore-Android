package com.baconga.kttstore.Models;

import java.util.Date;

public class Notification {
    private String notificationID;
    private String title;
    private String type;
    private String message;
    private int readCount;
    private Date scheduledFor;
    private Date expiresAt;
    private Date createdAt;
    private String createdBy;
    private boolean isRead;
    private Date readAt;
    private String userNotificationID;

    public Notification(String notificationID, String title, String type, String message,
                       int readCount, Date scheduledFor, Date expiresAt, Date createdAt,
                       String createdBy, boolean isRead, Date readAt, String userNotificationID) {
        this.notificationID = notificationID;
        this.title = title;
        this.type = type;
        this.message = message;
        this.readCount = readCount;
        this.scheduledFor = scheduledFor;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.isRead = isRead;
        this.readAt = readAt;
        this.userNotificationID = userNotificationID;
    }

    // Getters
    public String getNotificationID() { return notificationID; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public int getReadCount() { return readCount; }
    public Date getScheduledFor() { return scheduledFor; }
    public Date getExpiresAt() { return expiresAt; }
    public Date getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public boolean isRead() { return isRead; }
    public Date getReadAt() { return readAt; }
    public String getUserNotificationID() { return userNotificationID; }

    // Setters
    public void setIsRead(boolean isRead) { this.isRead = isRead; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }
} 