package com.example.app_technical_admin.model.EventBus;

import com.example.app_technical_admin.model.Order;

public class OrderEvent {
    Order order;

    public OrderEvent(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
