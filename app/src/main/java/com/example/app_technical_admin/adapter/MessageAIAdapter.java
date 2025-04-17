package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.DetailActivity; // Cập nhật import
import com.example.app_technical_admin.model.MessageAI;
import com.example.app_technical_admin.model.NewProduct;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MessageAIAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
//    private static final int VIEW_TYPE_BOT_PRODUCT = 3;

    private List<MessageAI> messageList;

    public MessageAIAdapter(List<MessageAI> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        MessageAI message = messageList.get(position);
        return message.getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_BOT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_bot, parent, false);
            return new BotMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_suggestion, parent, false);
            return new ProductMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageAI message = messageList.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).textViewMessage.setText(message.getContent());
        } else if (holder instanceof BotMessageViewHolder) {
            ((BotMessageViewHolder) holder).textViewMessage.setText(message.getContent());
        } else if (holder instanceof ProductMessageViewHolder) {
            ProductMessageViewHolder productHolder = (ProductMessageViewHolder) holder;
            List<NewProduct> products = message.getProducts();
            if (products != null && !products.isEmpty()) {
                NewProduct product = products.get(0); // Vì chúng ta xử lý một sản phẩm cho mỗi tin nhắn
                productHolder.textViewProductName.setText(product.getProductName());

                // Định dạng giá với dấu phân cách hàng nghìn
                try {
                    DecimalFormat decimalFormat = new DecimalFormat("#,###");
                    // Chuyển giá từ String sang số, sau đó định dạng
                    String price = product.getPrice().replaceAll("[^0-9]", ""); // Loại bỏ ký tự không phải số
                    long priceValue = Long.parseLong(price);
                    String formattedPrice = decimalFormat.format(priceValue) + " VNĐ";
                    productHolder.textViewProductPrice.setText(formattedPrice);
                } catch (NumberFormatException e) {
                    // Nếu giá không thể parse thành số, hiển thị giá gốc
                    productHolder.textViewProductPrice.setText(product.getPrice() + " VNĐ");
                }

                // Tải hình ảnh sản phẩm bằng Glide
                if (product.getImage() != null && !product.getImage().isEmpty()) {
                    Glide.with(productHolder.itemView.getContext())
                            .load(product.getImage().get(0))
                            .placeholder(R.drawable.ic_image)
                            .into(productHolder.imageViewProduct);
                } else {
                    productHolder.imageViewProduct.setImageResource(R.drawable.ic_image);
                }

                // Thiết lập sự kiện click để chuyển hướng đến DetailActivity
                productHolder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(productHolder.itemView.getContext(), DetailActivity.class);
                    intent.putExtra("detail", product); // Truyền đối tượng NewProduct
                    productHolder.itemView.getContext().startActivity(intent);
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;

        BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
        }
    }

    static class ProductMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewProductName;
        TextView textViewProductPrice;

        ProductMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewProductName = itemView.findViewById(R.id.textViewProductName);
            textViewProductPrice = itemView.findViewById(R.id.textViewProductPrice);
        }
    }
}