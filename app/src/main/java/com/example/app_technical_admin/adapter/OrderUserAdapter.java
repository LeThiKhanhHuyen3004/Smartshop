package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.Interface.ItemClickDeleteListener;
import com.example.app_technical_admin.Interface.ItemClickListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.Order;
import com.example.app_technical_admin.utils.Utils;

import java.util.List;

import okio.Utf8;

public class OrderUserAdapter extends RecyclerView.Adapter<OrderUserAdapter.MyViewHolder> {
    private RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
    private Context context;
    private List<Order> listOrder;
    ItemClickDeleteListener deleteListener;

    public OrderUserAdapter(Context context, List<Order> listOrder, ItemClickDeleteListener itemClickDeleteListener) {
        this.context = context;
        this.listOrder = listOrder;
        this.deleteListener = itemClickDeleteListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_user, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Order order = listOrder.get(position);
        holder.txtOrder.setText("Order ID: " + order.getId());
        holder.txtStatus.setText(Utils.statusOrder(order.getStatus()));
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                deleteListener.onClickDelete(order.getId());
                return false;
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                holder.reDetail.getContext(),
                LinearLayoutManager.VERTICAL,
                false
        );
        layoutManager.setInitialPrefetchItemCount(order.getItem().size());
        DetailAdapter detailAdapter = new DetailAdapter(context, order.getItem());
        holder.reDetail.setLayoutManager(layoutManager);
        holder.reDetail.setAdapter(detailAdapter);
        holder.reDetail.setRecycledViewPool(viewPool);

    }



    @Override
    public int getItemCount() {
        return listOrder.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder  {
        TextView txtOrder, txtStatus;
        RecyclerView reDetail;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrder = itemView.findViewById(R.id.id_order);
            reDetail = itemView.findViewById(R.id.recycleview_detail);
            txtStatus = itemView.findViewById(R.id.statusOrder);

        }
    }


}
