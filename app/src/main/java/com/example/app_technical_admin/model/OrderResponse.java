package com.example.app_technical_admin.model;

import java.util.List;

public class OrderResponse {
    private boolean success;
    private String message;
    private List<OrderForAI> result;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<OrderForAI> getResult() {
        return result;
    }
}