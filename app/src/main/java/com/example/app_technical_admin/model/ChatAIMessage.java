package com.example.app_technical_admin.model;

import com.google.gson.annotations.SerializedName;

public class ChatAIMessage {
    @SerializedName("message_content")
    private String messageContent;

    @SerializedName("message_type")
    private int messageType;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("product_ids")
    private String productIds;

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getProductIds() {
        return productIds;
    }

    public void setProductIds(String productIds) {
        this.productIds = productIds;
    }
}