package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";
    private TextView txtUserName, txtUserTag;
    private ImageView imgAvatar, imgInfor; // Thêm imgInfor
    private LinearLayout layoutUserInfo, layoutOrders;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameCart;
    private LinearLayout layoutPending, layoutAccepted, layoutDelivering, layoutDelivered, layoutCanceled;
//    Toolbar toolbar ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);

        // Ánh xạ
        mapping();

        displayUserInfo();



        // Đặt mục Account được chọn
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) {
            Log.e(TAG, "bottomNavigationView is null");
            return;
        }
        bottomNavigationView.setSelectedItemId(R.id.nav_account);

        // Gọi phương thức setupBottomNavigation
        setupBottomNavigation();

        // Sự kiện click vào layoutUserInfo
        layoutUserInfo.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, UserInforActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting UserInforActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào imgInfor
        imgInfor.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, MyOrderActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });


        // Sự kiện click vào layoutPending (Tab Pending - vị trí 1)
        layoutPending.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, MyOrderActivity.class);
                intent.putExtra("tab_position", 1); // Tab Pending
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutAccepted (Tab Accepted - vị trí 2)
        layoutAccepted.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, MyOrderActivity.class);
                intent.putExtra("tab_position", 2); // Tab Accepted
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutDelivering (Tab Delivering - vị trí 3)
        layoutDelivering.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, MyOrderActivity.class);
                intent.putExtra("tab_position", 3); // Tab Delivering
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutDelivered (Tab Delivered successfully - vị trí 4)
        layoutDelivered.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, MyOrderActivity.class);
                intent.putExtra("tab_position", 4); // Tab Delivered successfully
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });
        frameCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cart = new Intent(getApplicationContext(), CartActivity.class);
                startActivity(cart);
            }

        });
        // Sự kiện click vào layoutCanceled (Tab Canceled - vị trí 5)
        layoutCanceled.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AccountActivity.this, MyOrderActivity.class);
                intent.putExtra("tab_position", 5); // Tab Canceled
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });
    }


    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Selected item ID: " + itemId);
            Log.d(TAG, "Expected nav_category ID: " + R.id.nav_category);

            if (itemId == R.id.nav_home) {
                Log.d(TAG, "Navigating to MainActivity");
                Intent homeIntent = new Intent(getApplicationContext(), MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_category) {
                Log.d(TAG, "Navigating to ViewOrderUserActivity");
                Intent orderIntent = new Intent(getApplicationContext(), ViewOrderUserActivity.class);
                startActivity(orderIntent);
                finish();
                return true;
            } else if (itemId == R.id.imgMessage) {
                Log.d(TAG, "Navigating to ChatUserActivity");
                Intent chatIntent = new Intent(getApplicationContext(), AdminChatActivity.class);
                startActivity(chatIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                Log.d(TAG, "Already in AccountActivity");
                Toast.makeText(getApplicationContext(), "Account selected", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Log.w(TAG, "Unknown item selected: " + itemId);
                return false;
            }
        });
    }

    private void mapping() {
        txtUserName = findViewById(R.id.txtUserName);
        txtUserTag = findViewById(R.id.txtUserTag);
        imgAvatar = findViewById(R.id.imgAvatar);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        layoutOrders = findViewById(R.id.layoutOrders);
        imgInfor = findViewById(R.id.imgInfor);
        layoutPending = findViewById(R.id.layoutPending);
        layoutAccepted = findViewById(R.id.layoutAccepted);
        layoutDelivering = findViewById(R.id.layoutDelivering);
        layoutDelivered = findViewById(R.id.layoutDelivered);
        layoutCanceled = findViewById(R.id.layoutCanceled);
        frameCart = findViewById(R.id.frameCart);
//        toolbar = findViewById(R.id.toolbar);
// Thêm ánh xạ cho imgInfor
    }

    private void displayUserInfo() {
        // Kiểm tra xem Utils.user_current có dữ liệu không
        if (Utils.user_current != null) {
            // Hiển thị tên người dùng
            if (Utils.user_current.getUserName() != null && !Utils.user_current.getUserName().isEmpty()) {
                txtUserName.setText(Utils.user_current.getUserName());
            } else {
                txtUserName.setText("Account Name"); // Giá trị mặc định nếu không có dữ liệu
            }

            // Hiển thị avatar
            if (Utils.user_current.getAvatar() != null && !Utils.user_current.getAvatar().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(Utils.user_current.getAvatar(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgAvatar.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi giải mã avatar: " + e.getMessage());
                    imgAvatar.setImageResource(R.drawable.ic_username); // Hình ảnh mặc định nếu có lỗi
                }
            } else {
                imgAvatar.setImageResource(R.drawable.ic_username); // Hình ảnh mặc định nếu không có avatar
            }
        } else {
            Log.w(TAG, "Utils.user_current is null");
            txtUserName.setText("Account Name"); // Giá trị mặc định
            imgAvatar.setImageResource(R.drawable.ic_username); // Hình ảnh mặc định
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật lại thông tin người dùng khi activity được quay lại (sau khi chỉnh sửa từ UserInforActivity)
        displayUserInfo();
    }
}