package com.example.app_technical_admin.model;

import com.example.app_technical_admin.Interface.NotificationItem;

import java.io.Serializable;

public class PromotionNotification implements Serializable, NotificationItem {
    private Promotion promotion;
    private boolean isRead;

    public PromotionNotification(Promotion promotion, boolean isRead) {
        this.promotion = promotion;
        this.isRead = isRead;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    @Override
    public boolean isRead() {
        return isRead;
    }

    @Override
    public void setRead(boolean read) {
        isRead = read;
    }
}