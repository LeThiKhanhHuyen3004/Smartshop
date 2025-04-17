package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.Interface.ItemClickListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.DetailActivity;
import com.example.app_technical_admin.model.NewProduct;

import java.text.DecimalFormat;
import java.util.List;

public class PhoneAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<NewProduct> array;
    private static final int VIEW_TYPE_DATA = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public PhoneAdapter(Context context, List<NewProduct> array) {
        this.context = context;
        this.array = array;
    }





    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_DATA){
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_phone, parent, false);
            return new MyViewHolder(view);
        }else{
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof MyViewHolder){
            MyViewHolder myViewHolder = (MyViewHolder) holder;
            NewProduct newProduct = array.get(position);

            // Kiểm tra null trước khi gọi trim()
            if (newProduct.getProductName() != null) {
                myViewHolder.productName.setText(newProduct.getProductName().trim());
            } else {
                myViewHolder.productName.setText("N/A"); // Giá trị mặc định khi null
            }

            // Kiểm tra null trước khi xử lý giá
            if (newProduct.getPrice() != null) {
                DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
                myViewHolder.productPrice.setText(decimalFormat.format(Double.parseDouble(newProduct.getPrice())) + " VND");
            } else {
                myViewHolder.productPrice.setText("Price: N/A");
            }

//            // Kiểm tra null trước khi đặt mô tả
//            if (newProduct.getDescription() != null) {
//                myViewHolder.description.setText(newProduct.getDescription());
//            } else {
//                myViewHolder.description.setText("No description available.");
//            }

            // Kiểm tra null trước khi load ảnh
            if (newProduct.getImage() != null && !newProduct.getImage().isEmpty()) {
                Glide.with(context).load(newProduct.getImage().get(0)).into(myViewHolder.image);
            } else {
                myViewHolder.image.setImageResource(R.drawable.ic_password); // Ảnh mặc định
            }

            myViewHolder.setItemClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int pos, boolean isLongClick) {
                    if(!isLongClick){
                        Intent intent = new Intent(context, DetailActivity.class);
                        intent.putExtra("detail", newProduct);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            });
        } else {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }


    @Override
    public int getItemViewType(int position) {

        return array.get(position) == null ? VIEW_TYPE_LOADING: VIEW_TYPE_DATA;
    }

    @Override
    public int getItemCount() {
        return array.size();
    }

    public class LoadingViewHolder extends RecyclerView.ViewHolder{
        ProgressBar progressBar;

        public LoadingViewHolder(@NonNull View itemview){
            super(itemview);
            progressBar = itemview.findViewById(R.id.progressbar);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView productName, productPrice, idProduct;
        ImageView image;
        private ItemClickListener itemClickListener;
        public MyViewHolder(@NonNull View itemView){
            super(itemView);
            productName = itemView.findViewById(R.id.itemPhone_name);
            productPrice = itemView.findViewById(R.id.itemPhone_price);
//            description = itemView.findViewById(R.id.itemPhone_des);
            //idProduct = itemView.findViewById(R.id.itemPhone_idProduct);
            image = itemView.findViewById(R.id.itemPhone_image);
            itemView.setOnClickListener(this);
        }

        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onClick(view, getAdapterPosition(), false);
        }
    }

}
