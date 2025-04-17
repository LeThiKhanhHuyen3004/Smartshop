package com.example.app_technical_admin.model;

import java.util.List;

public class TypeOfProductModel {
    private boolean success;
    private String message;
    private List<TypeOfProduct> result;

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

    public List<TypeOfProduct> getResult() {
        return result;
    }

    public void setResult(List<TypeOfProduct> result) {
        this.result = result;
    }
}
