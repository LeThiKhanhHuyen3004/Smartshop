package com.example.app_technical_admin.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class MessageAI implements Parcelable {
    public static final int TYPE_USER = 1; // Tin nhắn của người dùng
    public static final int TYPE_BOT = 2;  // Tin nhắn văn bản của bot
    public static final int TYPE_BOT_PRODUCT = 3; // Tin nhắn của bot chứa danh sách sản phẩm

    private String content; // Nội dung văn bản của tin nhắn
    private int type; // Loại tin nhắn
    private List<NewProduct> products; // Danh sách sản phẩm (dùng cho TYPE_BOT_PRODUCT)

    // Constructor cho tin nhắn văn bản
    public MessageAI(String content, int type) {
        this.content = content;
        this.type = type;
        this.products = new ArrayList<>();
    }

    // Constructor cho tin nhắn chứa sản phẩm
    public MessageAI(String content, int type, List<NewProduct> products) {
        this.content = content;
        this.type = type;
        this.products = products != null ? products : new ArrayList<>();
    }

    // Constructor từ Parcel
    protected MessageAI(Parcel in) {
        content = in.readString();
        type = in.readInt();
        products = new ArrayList<>();
        in.readList(products, NewProduct.class.getClassLoader());
    }

    public static final Creator<MessageAI> CREATOR = new Creator<MessageAI>() {
        @Override
        public MessageAI createFromParcel(Parcel in) {
            return new MessageAI(in);
        }

        @Override
        public MessageAI[] newArray(int size) {
            return new MessageAI[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(content);
        dest.writeInt(type);
        dest.writeList(products);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters và Setters
    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    public List<NewProduct> getProducts() {
        return products;
    }
}