package com.example.app_technical_admin.model;

public class NotificationModel {
    private String id;
    private String title;
    private String body;
    private String timestamp;
    private int orderId;
    private int userId; // Thêm trường userId
    private boolean seen;

    public NotificationModel() {
    }

    public NotificationModel(String id, String title, String body, String timestamp, int orderId, int userId, boolean seen) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.orderId = orderId;
        this.userId = userId;
        this.seen = seen;
    }

    // Getter và Setter cho userId
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Các getter và setter khác giữ nguyên
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}