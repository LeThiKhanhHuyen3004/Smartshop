package com.example.app_technical_admin.model;

public class TypeOfProduct {
    int id;
    String productName;
    String image;

    public TypeOfProduct(String productName, String image) {
        this.productName = productName;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
