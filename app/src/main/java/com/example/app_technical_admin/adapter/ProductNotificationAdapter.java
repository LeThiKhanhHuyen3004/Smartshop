package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.Item;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ProductNotificationAdapter extends RecyclerView.Adapter<ProductNotificationAdapter.ViewHolder> {

    private Context context;
    private List<Item> productList;

    public ProductNotificationAdapter(Context context, List<Item> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item product = productList.get(position);

        // Hiển thị tên sản phẩm
        holder.textViewProductName.setText(product.getProductName());

        // Hiển thị hình ảnh sản phẩm
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            Picasso.get().load(product.getImage()).into(holder.imageViewProduct);
        } else {
            holder.imageViewProduct.setImageResource(R.drawable.ic_image); // Hình ảnh mặc định nếu không có ảnh
        }
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewProductName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewProductName = itemView.findViewById(R.id.textViewProductName);
        }
    }
}