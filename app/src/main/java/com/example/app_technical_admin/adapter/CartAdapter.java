package com.example.app_technical_admin.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.Interface.ImageClickListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.Cart;
import com.example.app_technical_admin.model.EventBus.TotalEvent;
import com.example.app_technical_admin.model.Promotion;
import com.example.app_technical_admin.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.paperdb.Paper;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.MyViewHolder> {
    Context context;
    List<Cart> cartList;

    public CartAdapter(Context context, List<Cart> cartList) {
        this.context = context;
        this.cartList = cartList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Cart cart = cartList.get(position);
        holder.item_cart_ProductName.setText(cart.getProductName());
        holder.item_cart_count.setText(cart.getCount() + " ");
        Glide.with(context).load(cart.getProductImg()).into(holder.item_cart_image);
        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

        // Kiểm tra khuyến mãi và tính giá sau giảm
        long priceToUse = cart.getPrice(); // Giá mặc định là giá gốc
        if (cart.getPromotions() != null && !cart.getPromotions().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date currentDate = new Date();
            for (Promotion promotion : cart.getPromotions()) {
                try {
                    Date startDate = dateFormat.parse(promotion.getStartDate());
                    Date endDate = dateFormat.parse(promotion.getEndDate());
                    if (currentDate.after(startDate) && currentDate.before(endDate)) {
                        double discount = promotion.getDiscount();
                        if (promotion.getDiscountType().equals("percent")) {
                            priceToUse = (long) (cart.getPrice() * (1 - discount / 100));
                        } else {
                            priceToUse = cart.getPrice() - (long) discount;
                        }
                        cart.setDiscountedPrice(priceToUse); // Lưu giá sau giảm vào Cart
                        break; // Chỉ lấy khuyến mãi đầu tiên hợp lệ
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        // Hiển thị giá (giá sau giảm nếu có, nếu không thì giá gốc)
        holder.item_cart_price.setText(decimalFormat.format(priceToUse));
        long totalPrice = cart.getCount() * priceToUse;
        holder.item_cart_ProductPrice2.setText(decimalFormat.format(totalPrice));

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Utils.arrayCart.get(holder.getAdapterPosition()).setChecked(true);
                    if (!Utils.arrayBuyProduct.contains(cart)) {
                        Utils.arrayBuyProduct.add(cart);
                    }
                    EventBus.getDefault().postSticky(new TotalEvent());
                } else {
                    Utils.arrayCart.get(holder.getAdapterPosition()).setChecked(false);
                    for (int i = 0; i < Utils.arrayBuyProduct.size(); i++) {
                        if (Utils.arrayBuyProduct.get(i).getId() == cart.getId()) {
                            Utils.arrayBuyProduct.remove(i);
                            EventBus.getDefault().postSticky(new TotalEvent());
                            break;
                        }
                    }
                }
            }
        });
        holder.checkBox.setChecked(cart.isChecked());

        holder.setListener(new ImageClickListener() {
            @Override
            public void onImageClick(View view, int pos, int value) {
                if (value == 1) {
                    if (cartList.get(pos).getCount() > 1) {
                        int newCount = cartList.get(pos).getCount() - 1;
                        cartList.get(pos).setCount(newCount);

                        holder.item_cart_count.setText(cartList.get(pos).getCount() + " ");
                        long price = cartList.get(pos).getCount() * (cartList.get(pos).getDiscountedPrice() > 0 ? cartList.get(pos).getDiscountedPrice() : cartList.get(pos).getPrice());
                        holder.item_cart_ProductPrice2.setText(decimalFormat.format(price));
                        EventBus.getDefault().postSticky(new TotalEvent());
                    } else if (cartList.get(pos).getCount() == 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getRootView().getContext());
                        builder.setTitle("Notification");
                        builder.setMessage("Do you want to remove this product from cart?");
                        builder.setPositiveButton("Consent", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Utils.arrayBuyProduct.remove(cart);
                                Utils.arrayCart.remove(pos);
                                Paper.book().write("cart", Utils.arrayCart);
                                notifyDataSetChanged();
                                EventBus.getDefault().postSticky(new TotalEvent());
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        builder.show();
                    }
                } else if (value == 2) {
                    if (cartList.get(pos).getCount() < cartList.get(pos).getCountStock()) {
                        int newCount = cartList.get(pos).getCount() + 1;
                        cartList.get(pos).setCount(newCount);
                    }
                    holder.item_cart_count.setText(cartList.get(pos).getCount() + " ");
                    long price = cartList.get(pos).getCount() * (cartList.get(pos).getDiscountedPrice() > 0 ? cartList.get(pos).getDiscountedPrice() : cartList.get(pos).getPrice());
                    holder.item_cart_ProductPrice2.setText(decimalFormat.format(price));
                    EventBus.getDefault().postSticky(new TotalEvent());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView item_cart_image, item_cart_remove, item_cart_add;
        TextView item_cart_ProductName, item_cart_price, item_cart_count, item_cart_ProductPrice2;
        ImageClickListener listener;
        CheckBox checkBox;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            item_cart_image = itemView.findViewById(R.id.item_cart_image);
            item_cart_ProductName = itemView.findViewById(R.id.item_cart_productName);
            item_cart_price = itemView.findViewById(R.id.item_cart_price);
            item_cart_count = itemView.findViewById(R.id.item_cart_count);
            item_cart_ProductPrice2 = itemView.findViewById(R.id.item_cart_ProductPrice2);
            item_cart_remove = itemView.findViewById(R.id.item_cart_remove);
            item_cart_add = itemView.findViewById(R.id.item_cart_add);
            checkBox = itemView.findViewById(R.id.item_cart_check);
            // Event click
            item_cart_add.setOnClickListener(this);
            item_cart_remove.setOnClickListener(this);
        }

        public void setListener(ImageClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            if (view == item_cart_remove) {
                listener.onImageClick(view, getAdapterPosition(), 1);
            } else if (view == item_cart_add) {
                listener.onImageClick(view, getAdapterPosition(), 2);
            }
        }
    }
}