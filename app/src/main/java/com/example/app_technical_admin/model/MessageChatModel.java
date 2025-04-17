package com.example.app_technical_admin.model;

import java.util.Collection;

public class MessageChatModel {
    private boolean success;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Collection<? extends Message> getResult() {
        return java.util.Collections.emptyList();
    }

}