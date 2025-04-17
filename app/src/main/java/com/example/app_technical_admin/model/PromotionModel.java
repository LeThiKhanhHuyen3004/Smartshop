package com.example.app_technical_admin.model;

import java.util.List;

public class PromotionModel {
    private boolean success;
    private String message;
    private List<NewProduct> result;

    // Getters và Setters
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

    public List<NewProduct> getResult() {
        return result;
    }

    public void setResult(List<NewProduct> result) {
        this.result = result;
    }
}