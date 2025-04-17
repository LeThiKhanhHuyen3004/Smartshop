package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.Item;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.MyViewHolder> {
    private Context context;
    private List<Item> itemList;

    public DetailAdapter(Context context, List<Item> itemList) {
        this.context = context;
        this.itemList = itemList != null ? itemList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Item item = itemList.get(position);
        Log.d("DetailAdapter", "Product Name: " + item.getProductName() + " | Count: " + item.getCount() + " | Image: " + item.getImage());

        // Hiển thị tên sản phẩm
        holder.txtName.setText(item.getProductName());

        // Hiển thị số lượng
        holder.txtCount.setText("Count: " + item.getCount());

        // Xử lý hình ảnh
        String imageUrl = extractImageUrl(item.getImage());
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .into(holder.imageDetail);
        } else {
            holder.imageDetail.setImageResource(R.drawable.ic_image);
        }
    }

    // Phương thức để trích xuất URL hình ảnh từ item.getImage()
    private String extractImageUrl(String imageData) {
        if (imageData == null || imageData.isEmpty()) {
            return null;
        }

        // Kiểm tra xem imageData có phải là chuỗi JSON (danh sách) hay không
        if (imageData.startsWith("[") && imageData.endsWith("]")) {
            try {
                JSONArray jsonArray = new JSONArray(imageData);
                if (jsonArray.length() > 0) {
                    // Lấy URL hình ảnh đầu tiên từ danh sách
                    return jsonArray.getString(0);
                }
            } catch (JSONException e) {
                Log.e("DetailAdapter", "Error parsing image JSON: " + imageData, e);
            }
            return null;
        } else {
            // Nếu không phải chuỗi JSON, coi nó là URL hình ảnh đơn
            return imageData;
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public void updateItemList(List<Item> newItemList) {
        Log.d("DetailAdapter", "Updating item list, new size: " + (newItemList != null ? newItemList.size() : 0));
        this.itemList.clear();
        this.itemList.addAll(newItemList);
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageDetail;
        TextView txtName, txtCount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageDetail = itemView.findViewById(R.id.item_imgdetail);
            txtName = itemView.findViewById(R.id.item_detailProductName);
            txtCount = itemView.findViewById(R.id.item_countDetail);
        }
    }
}