package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.Interface.ItemClickListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.EventBus.OrderEvent;
import com.example.app_technical_admin.model.Order;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.MyViewHolder> {
    private Context context;
    private List<Order> listOrder;

    public OrderAdapter(Context context, List<Order> listOrder) {
        this.context = context;
        this.listOrder = listOrder;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Order order = listOrder.get(position);
        holder.txtOrder.setText("Order ID: " + order.getId());
        holder.status.setText(orderStatus(order.getStatus()));
        holder.address_order.setText("Address: "+order.getAddress());
        holder.user_order.setText("User: " + order.getUserName());

        Log.d("OrderAdapter", "Order ID: " + order.getId() + " có " + (order.getItem() != null ? order.getItem().size() : 0) + " sản phẩm.");

        if (order.getItem() != null && !order.getItem().isEmpty()) {
            DetailAdapter detailAdapter = new DetailAdapter(context, order.getItem());
            holder.reDetail.setAdapter(detailAdapter);

            // Đặt LayoutManager cho RecyclerView con
            holder.reDetail.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

            // Ngăn chặn RecyclerView con bị tái sử dụng sai cách
            holder.reDetail.setRecycledViewPool(new RecyclerView.RecycledViewPool());
            holder.reDetail.setNestedScrollingEnabled(false);
            holder.setListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int pos, boolean isLongClick) {
                    if(isLongClick){
                        EventBus.getDefault().postSticky(new OrderEvent(order));
                    }
                }
            });
        }
    }

    private String orderStatus(int status){
        String result = "";
        switch (status){
            case 0:
                result = "Order is pending payment";
                break;
            case 1:
                result = "Order accepted";
                break;
            case 2:
                result = "Order is delivering";
                break;
            case 3:
                result = "Order delivered successfully";
                break;
            case 4:
                result = "Order canceled";
                break;
        }
        return result;
    }


    @Override
    public int getItemCount() {
        return listOrder != null ? listOrder.size() : 0;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        TextView txtOrder, status, address_order, user_order;
        RecyclerView reDetail;
        ItemClickListener listener;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrder = itemView.findViewById(R.id.id_order);
            reDetail = itemView.findViewById(R.id.recycleview_detail);
            status = itemView.findViewById(R.id.status);
            address_order = itemView.findViewById(R.id.address_order);
            user_order = itemView.findViewById(R.id.user_order);
            itemView.setOnLongClickListener( this);
        }

        public void setListener(ItemClickListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean onLongClick(View view) {
            listener.onClick(view, getAdapterPosition(), true);
            return false;
        }
    }
}
