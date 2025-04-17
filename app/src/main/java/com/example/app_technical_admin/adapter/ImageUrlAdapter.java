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

import java.util.List;

public class ImageUrlAdapter extends RecyclerView.Adapter<ImageUrlAdapter.ImageUrlViewHolder> {
    private Context context;
    private List<String> imageUrls;
    private OnImageUrlRemoveListener removeListener;

    public interface OnImageUrlRemoveListener {
        void onRemove(int position);
    }

    public ImageUrlAdapter(Context context, List<String> imageUrls, OnImageUrlRemoveListener removeListener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ImageUrlViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_url, parent, false);
        return new ImageUrlViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageUrlViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        holder.tvImageUrl.setText(imageUrl);
        holder.ivRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    public static class ImageUrlViewHolder extends RecyclerView.ViewHolder {
        TextView tvImageUrl;
        ImageView ivRemove;

        public ImageUrlViewHolder(@NonNull View itemView) {
            super(itemView);
            tvImageUrl = itemView.findViewById(R.id.tvImageUrl);
            ivRemove = itemView.findViewById(R.id.ivRemove);
        }
    }
}