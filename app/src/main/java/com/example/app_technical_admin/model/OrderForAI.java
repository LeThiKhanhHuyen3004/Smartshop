package com.example.app_technical_admin.model;

import java.util.List;

public class OrderForAI {
    int id;
    int id_user;
    String address;
    String phoneNumber;
    String email;
    String total;
    int status;
    String userName;
    String orderDate;
    String momo;
    List<ItemForAI> item; // Sửa từ List<Item> thành List<ItemForAI>

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getMomo() {
        return momo;
    }

    public void setMomo(String momo) {
        this.momo = momo;
    }

    public List<ItemForAI> getItem() { // Sửa từ List<Item> thành List<ItemForAI>
        return item;
    }

    public void setItem(List<ItemForAI> item) { // Sửa từ List<Item> thành List<ItemForAI>
        this.item = item;
    }
}