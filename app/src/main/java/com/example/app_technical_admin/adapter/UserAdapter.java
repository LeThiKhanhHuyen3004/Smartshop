package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.Interface.ItemClickListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.AdminChatActivity;
import com.example.app_technical_admin.model.User;

import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyViewHolder> {
    Context context;
    List<User> userList;
    Map<Integer, Boolean> newMessageStatus; // Map để lưu trạng thái tin nhắn mới

    public UserAdapter(Context context, List<User> userList, Map<Integer, Boolean> newMessageStatus) {
        this.context = context;
        this.userList = userList;
        this.newMessageStatus = newMessageStatus;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        User user = userList.get(position);
        holder.txtid.setText(user.getId() + " ");
        holder.txtuser.setText(user.getUserName());

        // Hiển thị dấu chấm thông báo nếu có tin nhắn mới
        boolean hasNewMessage = newMessageStatus.getOrDefault(user.getId(), false);
        holder.notificationDot.setVisibility(hasNewMessage ? View.VISIBLE : View.GONE);

        holder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int pos, boolean isLongClick) {
                if (!isLongClick) {
                    // Xóa trạng thái tin nhắn mới khi người dùng nhấn vào user
                    newMessageStatus.put(user.getId(), false);
                    notifyItemChanged(pos);

                    Intent intent = new Intent(context, AdminChatActivity.class);
                    intent.putExtra("id", user.getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txtid, txtuser;
        View notificationDot; // View cho dấu chấm thông báo
        ItemClickListener itemClickListener;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtid = itemView.findViewById(R.id.id_user);
            txtuser = itemView.findViewById(R.id.userName);
            notificationDot = itemView.findViewById(R.id.notificationDot); // Ánh xạ View dấu chấm
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