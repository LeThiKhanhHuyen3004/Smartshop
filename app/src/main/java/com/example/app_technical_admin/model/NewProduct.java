package com.example.app_technical_admin.model;

import android.os.Parcelable;

import java.io.Serializable;
import java.util.List;

public class NewProduct implements Serializable{
    int id;
    String productName;
    private List<String> image;
    String price;
    String description;
    int category;
    String linkVideo;
    int countStock;
    List<Promotion> promotions;

    public List<String> getImage() {
        return image;
    }

    public void setImage(List<String> image) {
        this.image = image;
    }

    // Danh sách khuyến mãi

    // Constructor
    public NewProduct() {}

    // Getters và Setters
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



    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getLinkVideo() {
        return linkVideo;
    }

    public void setLinkVideo(String linkVideo) {
        this.linkVideo = linkVideo;
    }

    public int getCountStock() {
        return countStock;
    }

    public void setCountStock(int countStock) {
        this.countStock = countStock;
    }

    public List<Promotion> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<Promotion> promotions) {
        this.promotions = promotions;
    }
}