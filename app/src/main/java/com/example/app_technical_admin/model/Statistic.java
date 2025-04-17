package com.example.app_technical_admin.model;

public class Statistic {
    private String productName;
    private int total;
    private String monthTotal;
    private String month;

    public String getMonthTotal() {
        return monthTotal;
    }

    public void setMonthTotal(String monthTotal) {
        this.monthTotal = monthTotal;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
