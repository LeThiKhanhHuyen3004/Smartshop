package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
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
import com.example.app_technical_admin.activity.DetailActivity;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.Promotion;
import com.example.app_technical_admin.utils.Utils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PromotionProductAdapter extends RecyclerView.Adapter<PromotionProductAdapter.MyViewHolder> {
    Context context;
    List<NewProduct> array;

    public PromotionProductAdapter(Context context, List<NewProduct> array) {
        this.context = context;
        this.array = array;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion_product, parent, false);
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

        // Kiểm tra và load hình ảnh đầu tiên từ danh sách image
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

        // Hiển thị thông tin khuyến mãi
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
                    // Chỉ hiển thị khuyến mãi nếu ngày hiện tại nằm trong khoảng thời gian áp dụng
                    if (currentDate.after(startDate) && currentDate.before(endDate)) {
                        // Tạo thông tin khuyến mãi
                        promotionInfo.append(promotion.getDiscount())
                                .append(promotion.getDiscountType().equals("percent") ? "%" : " VNĐ")
                                .append(" Off") // Add " off" after the discount value and unit
                                .append(" (").append(promotion.getName()).append(")");

                        // Tính giá sau giảm
                        double discount = promotion.getDiscount();
                        if (promotion.getDiscountType().equals("percent")) {
                            discountedPrice = originalPrice * (1 - discount / 100);
                        } else {
                            discountedPrice = originalPrice - discount;
                        }
                        hasValidPromotion = true;
                        break; // Chỉ lấy khuyến mãi đầu tiên hợp lệ
                    }
                } catch (ParseException e) {
                    Log.e("PromotionProductAdapter", "Error parsing promotion dates: " + e.getMessage());
                }
            }

            if (hasValidPromotion) {
                // Gạch ngang giá gốc
                holder.txtprice.setPaintFlags(holder.txtprice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                // Hiển thị giá sau giảm
                holder.txtdiscountedprice.setText(decimalFormat.format(discountedPrice) + " VND");
                holder.txtdiscountedprice.setVisibility(View.VISIBLE);
                // Hiển thị thông tin khuyến mãi
                holder.txtpromotion.setText(promotionInfo.toString());
                holder.txtpromotion.setVisibility(View.VISIBLE);
            } else {
                // Không có khuyến mãi hợp lệ
                holder.txtprice.setPaintFlags(holder.txtprice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.txtdiscountedprice.setVisibility(View.GONE);
                holder.txtpromotion.setVisibility(View.GONE);
            }
        } else {
            // Không có khuyến mãi
            holder.txtprice.setPaintFlags(holder.txtprice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.txtdiscountedprice.setVisibility(View.GONE);
            holder.txtpromotion.setVisibility(View.GONE);
        }

        // Sự kiện click để mở DetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("detail", newProduct);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return array.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtprice, txtname, txtpromotion, txtdiscountedprice;
        ImageView imgimage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtprice = itemView.findViewById(R.id.itemPromotion_price);
            txtname = itemView.findViewById(R.id.itemPromotion_name);
            imgimage = itemView.findViewById(R.id.itemPromotion_image);
            txtpromotion = itemView.findViewById(R.id.itemPromotion_promotion);
            txtdiscountedprice = itemView.findViewById(R.id.itemPromotion_discounted_price);
        }
    }
}