package com.example.app_technical_admin.model.EventBus;

import com.example.app_technical_admin.model.NewProduct;

public class EditDeleteEvent {
    NewProduct newProduct;

    public EditDeleteEvent(NewProduct newProduct) {
        this.newProduct = newProduct;
    }

    public NewProduct getNewProduct() {
        return newProduct;
    }

    public void setNewProduct(NewProduct newProduct) {
        this.newProduct = newProduct;
    }
}
