package com.example.app_technical_admin.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.Interface.ItemClickDeleteListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.OrderUserAdapter;
import com.example.app_technical_admin.model.Order;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import io.paperdb.Paper;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ViewOrderUserActivity extends AppCompatActivity {
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiSale apiSale;
    RecyclerView reOrder;
    Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    FirebaseFirestore db;
    private OrderUserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order_user);

        db = FirebaseFirestore.getInstance();

        // Tạo notification channel
        createNotificationChannel();

        // Khởi tạo Utils để khôi phục user_current
        Utils.initUser(this);

        // Kiểm tra và làm mới thông tin user
        checkAndRefreshUserData();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_category);

        setupBottomNavigation();
        initView();
        initToolbar();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Order Notifications";
            String description = "Notifications for order updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("order_channel", name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("Notification", "Notification channel created");
            } else {
                Log.e("Notification", "NotificationManager is null when creating channel");
            }
        }
    }

    private void checkAndRefreshUserData() {
        if (Utils.user_current == null || Utils.user_current.getId() <= 0 || Utils.user_current.getEmail() == null) {
            Log.w("ViewOrderUserActivity", "Thông tin người dùng không đầy đủ, làm mới từ server");
            String savedEmail = Paper.book().read("email");
            String savedPassword = Paper.book().read("password");
            if (savedEmail != null && savedPassword != null) {
                compositeDisposable.add(apiSale.login(savedEmail, savedPassword)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                userModel -> {
                                    if (userModel.isSuccess() && !userModel.getResult().isEmpty()) {
                                        Utils.user_current = userModel.getResult().get(0);
                                        Utils.saveUser(this);
                                        getOrder();
                                        listenForOrderStatusChanges(); // Lắng nghe thay đổi trạng thái đơn hàng
                                    } else {
                                        Toast.makeText(this, "Không thể làm mới dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    }
                                },
                                throwable -> {
                                    Log.e("ViewOrderUserActivity", "Lỗi làm mới dữ liệu: " + throwable.getMessage());
                                    Toast.makeText(this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                }
                        ));
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        } else {
            getOrder();
            listenForOrderStatusChanges(); // Lắng nghe thay đổi trạng thái đơn hàng
        }
    }

    private void listenForOrderStatusChanges() {
        int userId = Utils.user_current.getId();
        Log.d("ViewOrderUserActivity", "Lắng nghe thay đổi trạng thái đơn hàng cho userId: " + userId);

        db.collection("orders")
                .whereEqualTo("id_user", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Lỗi lắng nghe thay đổi trạng thái đơn hàng: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        Log.d("Firestore", "Nhận được sự kiện thay đổi từ Firestore");
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                Order order = dc.getDocument().toObject(Order.class);
                                int orderId = order.getId();
                                int status = order.getStatus();
                                String statusText = orderStatus(status);
                                Log.d("Firestore", "Trạng thái đơn hàng #" + orderId + " đã thay đổi: " + statusText);

                                // Tạo thông báo cục bộ trên thiết bị của client
                                sendNotification("Order Status Updated", "Order #" + orderId + " status updated to: " + statusText, orderId);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Không nhận được dữ liệu từ Firestore");
                    }
                });
    }
    private String orderStatus(int status) {
        String result = "";
        switch (status) {
            case 0:
                result = "Your Order is pending payment";
                break;
            case 1:
                result = "Your Order accepted";
                break;
            case 2:
                result = "Your Order is Shipping";
                break;
            case 3:
                result = "Your Order Delivered successful";
                break;
            case 4:
                result = "Your Order Canceled";
                break;
        }
        return result;
    }

    private void sendNotification(String title, String body, int orderId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigate_to_notifications", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "order_channel")
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(orderId, builder.build());
            Log.d("Notification", "Notification sent: " + title + " - " + body);
        } else {
            Log.e("Notification", "NotificationManager is null");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent homeIntent = new Intent(getApplicationContext(), MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_category) {
                Toast.makeText(getApplicationContext(), "Order selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.imgMessage) {
                Intent chatIntent = new Intent(getApplicationContext(), AdminChatActivity.class);
                startActivity(chatIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                Intent accountIntent = new Intent(getApplicationContext(), AccountActivity.class);
                startActivity(accountIntent);
                finish();
                return true;
            } else {
                return false;
            }
        });
    }

    private void getOrder() {
        int userId = Utils.user_current.getId();
        Log.d("ViewOrderUserActivity", "Lấy đơn hàng với userId: " + userId);
        if (userId <= 0) {
            Toast.makeText(this, "ID người dùng không hợp lệ, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        compositeDisposable.add(apiSale.viewOrder(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orderModel -> {
                            if (orderModel != null && orderModel.getResult() != null) {
                                adapter = new OrderUserAdapter(getApplicationContext(), orderModel.getResult(), new ItemClickDeleteListener() {
                                    @Override
                                    public void onClickDelete(int id_order) {
                                        showDeleteOrder(id_order);
                                    }
                                });
                                reOrder.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getApplicationContext(), "Không có đơn hàng!", Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> {
                            Log.e("API_ERROR", "Lỗi API: " + throwable.getMessage());
                            Toast.makeText(getApplicationContext(), "Lỗi khi lấy đơn hàng!", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void showDeleteOrder(int idOrder) {
        PopupMenu popupMenu = new PopupMenu(this, reOrder.findViewById(R.id.statusOrder));
        popupMenu.inflate(R.menu.menu_delete);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.deleteOrder) {
                    deleteOrder(idOrder);
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void deleteOrder(int idOrder) {
        compositeDisposable.add(apiSale.deleteOrder(idOrder)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            if (messageModel.isSuccess()) {
                                Toast.makeText(getApplicationContext(), "Xóa đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                                getOrder();
                            } else {
                                Toast.makeText(getApplicationContext(), "Xóa đơn hàng thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> {
                            Log.e("DeleteOrder", "Lỗi xóa đơn hàng: " + throwable.getMessage());
                            Toast.makeText(getApplicationContext(), "Lỗi khi xóa đơn hàng!", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void initView() {
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        reOrder = findViewById(R.id.recycleview_order_user);
        toolbar = findViewById(R.id.toolbar);
        reOrder.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}