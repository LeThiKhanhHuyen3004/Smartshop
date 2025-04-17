package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.Interface.ItemClickListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.DetailActivity;
import com.example.app_technical_admin.model.EventBus.EditDeleteEvent;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.Promotion;
import com.example.app_technical_admin.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NewProductAdapter extends RecyclerView.Adapter<NewProductAdapter.MyViewHolder> {
    Context context;
    List<NewProduct> array;

    public NewProductAdapter(Context context, List<NewProduct> array) {
        this.context = context;
        this.array = array;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_product, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        NewProduct newProduct = array.get(position);
        holder.txtname.setText(newProduct.getProductName());
        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

        // Giá gốc
        double originalPrice = Double.parseDouble(newProduct.getPrice());
        holder.txtprice.setText(decimalFormat.format(originalPrice) + " VND");

        // Load hình ảnh đầu tiên từ danh sách image
        List<String> imageUrls = newProduct.getImage();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String firstImageUrl = imageUrls.get(0); // Lấy URL hình ảnh đầu tiên
            if (firstImageUrl.contains("http")) {
                Glide.with(context)
                        .load(firstImageUrl)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(holder.imgimage);
            } else {
                String imageUrl = Utils.BASE_URL + "images/" + firstImageUrl;
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(holder.imgimage);
            }
        } else {
            // Nếu không có hình ảnh, hiển thị hình ảnh mặc định
            Glide.with(context)
                    .load(R.drawable.ic_image)
                    .into(holder.imgimage);
        }

        // Hiển thị thông tin khuyến mãi (giữ nguyên code hiện tại)
        if (newProduct.getPromotions() != null && !newProduct.getPromotions().isEmpty()) {
            StringBuilder promotionInfo = new StringBuilder();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date currentDate = new Date();
            boolean hasValidPromotion = false;
            double discountedPrice = originalPrice;

            for (Promotion promotion : newProduct.getPromotions()) {
                try {
                    Date startDate = dateFormat.parse(promotion.getStartDate());
                    Date endDate = dateFormat.parse(promotion.getEndDate());
                    if (currentDate.after(startDate) && currentDate.before(endDate)) {
                        promotionInfo.append(promotion.getDiscount())
                                .append(promotion.getDiscountType().equals("percent") ? "%" : " VNĐ")
                                .append(" Off") // Add " off" after the discount value and unit
                                .append(" (").append(promotion.getName()).append(")");
                        double discount = promotion.getDiscount();
                        if (promotion.getDiscountType().equals("percent")) {
                            discountedPrice = originalPrice * (1 - discount / 100);
                        } else {
                            discountedPrice = originalPrice - discount;
                        }
                        hasValidPromotion = true;
                        break;
                    }
                } catch (ParseException e) {
                    Log.e("NewProductAdapter", "Error parsing promotion dates: " + e.getMessage());
                }
            }

            if (hasValidPromotion) {
                holder.txtprice.setPaintFlags(holder.txtprice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.txtdiscountedprice.setText(decimalFormat.format(discountedPrice) + " VND");
                holder.txtdiscountedprice.setVisibility(View.VISIBLE);
                holder.txtpromotion.setText(promotionInfo.toString());
                holder.txtpromotion.setVisibility(View.VISIBLE);
            } else {
                holder.txtprice.setPaintFlags(holder.txtprice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.txtdiscountedprice.setVisibility(View.GONE);
                holder.txtpromotion.setVisibility(View.GONE);
            }
        } else {
            holder.txtprice.setPaintFlags(holder.txtprice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.txtdiscountedprice.setVisibility(View.GONE);
            holder.txtpromotion.setVisibility(View.GONE);
        }

        holder.setItemClickListener((view, pos, isLongClick) -> {
            if (!isLongClick) {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra("detail", newProduct);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                EventBus.getDefault().postSticky(new EditDeleteEvent(newProduct));
            }
        });
    }

    @Override
    public int getItemCount() {
        return array.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, View.OnLongClickListener {
        TextView txtprice, txtname, txtpromotion, txtdiscountedprice;
        ImageView imgimage;
        private ItemClickListener itemClickListener;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtprice = itemView.findViewById(R.id.item_price);
            txtname = itemView.findViewById(R.id.itemProduct_name);
            imgimage = itemView.findViewById(R.id.itemProduct_image);
            txtpromotion = itemView.findViewById(R.id.item_promotion);
            txtdiscountedprice = itemView.findViewById(R.id.item_discounted_price); // Thêm TextView giá sau giảm
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.onClick(view, getAdapterPosition(), false);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            contextMenu.add(0, 0, getAdapterPosition(), "Edit");
            contextMenu.add(0, 1, getAdapterPosition(), "Delete");
        }

        @Override
        public boolean onLongClick(View view) {
            itemClickListener.onClick(view, getAdapterPosition(), true);
            return false;
        }
    }
}