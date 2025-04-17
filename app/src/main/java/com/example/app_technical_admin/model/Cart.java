package com.example.app_technical_admin.model;

import java.util.List;

public class Cart {
    int id;
    String productName;
    long price; // Giá gốc
    long discountedPrice; // Giá sau giảm (nếu có khuyến mãi)
    String productImg;
    int count;
    boolean isChecked;
    int countStock;
    List<Promotion> promotions; // Danh sách khuyến mãi (nếu có)

    public Cart() {
    }

    public Cart(int id, String productName, long price, String productImg, int count) {
        this.id = id;
        this.productName = productName;
        this.price = price;
        this.productImg = productImg;
        this.count = count;
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

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(long discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public String getProductImg() {
        return productImg;
    }

    public void setProductImg(String productImg) {
        this.productImg = productImg;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
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