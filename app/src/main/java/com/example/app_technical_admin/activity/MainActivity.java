package com.example.app_technical_admin.activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.fragment.AccountFragment;
import com.example.app_technical_admin.activity.fragment.ChatUserFragment;
import com.example.app_technical_admin.activity.fragment.MainFragment;
import com.example.app_technical_admin.activity.fragment.OrderNotificationFragment;
import com.example.app_technical_admin.model.NotificationModel;
import com.example.app_technical_admin.model.Order;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ApiSale apiSale;
    private BadgeDrawable badgeDrawable;
    private Handler handler;
    private Runnable checkNotificationsRunnable;
    private boolean hasUnreadMessages = false;
    private FirebaseFirestore db;

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Xử lý thông báo nếu cần
        }
    };

    private BroadcastReceiver orderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("NEW_ORDER_PLACED".equals(intent.getAction())) {
                Log.d("MainActivity", "Received NEW_ORDER_PLACED broadcast");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        db = FirebaseFirestore.getInstance();

        requestNotificationPermission();

        createNotificationChannel();

        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.nav_category);
        badgeDrawable.setVisible(false);

        if (savedInstanceState == null) {
            loadFragment(new MainFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }



        setupBottomNavigation();

        handler = new Handler(Looper.getMainLooper());
        checkNotificationsRunnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 30000);
            }
        };
        handler.post(checkNotificationsRunnable);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notificationReceiver, new IntentFilter("NEW_NOTIFICATION"));

        // Đăng ký orderReceiver với cờ RECEIVER_NOT_EXPORTED
        IntentFilter intentFilter = new IntentFilter("NEW_ORDER_PLACED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(orderReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(orderReceiver, intentFilter);
        }

        handleNotificationNavigation();

        updateChatBadge();

        listenForNewOrders();

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "order_channel",
                    "Order Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for order updates");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void listenForNewOrders() {
        db.collection("orders")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MainActivity", "Error listening for orders: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Order order = dc.getDocument().toObject(Order.class);
                                int orderId = order.getId();
                                int userId = order.getId_user();

                                // Kiểm tra xem thông báo cho đơn hàng này đã tồn tại chưa
                                db.collection("notifications")
                                        .whereEqualTo("orderId", orderId)
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            if (queryDocumentSnapshots.isEmpty()) {
                                                // Nếu chưa có thông báo, tạo thông báo mới
                                                String title = "New Order Placed";
                                                String body = "Đơn hàng #" + orderId + " của bạn đã được đặt thành công.";
                                                saveNotificationToFirestore(title, body, orderId, userId);
                                                sendNotification(title, body, orderId, userId);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MainActivity", "Error checking existing notifications: " + e.getMessage());
                                        });
                            }
                        }
                    }
                });
    }

    private void saveNotificationToFirestore(String title, String body, int orderId, int userId) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        NotificationModel notification = new NotificationModel(null, title, body, timestamp, orderId, userId, false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Notification saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error saving notification: " + e.getMessage());
                    Toast.makeText(this, "Lỗi lưu thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification(String title, String body, int orderId, int userId) {
        saveNotificationToFirestore(title, body, orderId, userId);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigate_to_notifications", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "order_channel")
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new MainFragment();
            } else if (itemId == R.id.nav_category) {
                selectedFragment = new OrderNotificationFragment();
                badgeDrawable.setVisible(false);
            } else if (itemId == R.id.imgMessage) {
                if (Utils.user_current != null) {
                    selectedFragment = new ChatUserFragment();
                    clearChatBadge();
                } else {
                    Toast.makeText(this, "Please log in to access chat", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new AccountFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showChatBadge() {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.imgMessage);
        badge.setVisible(true);
        badge.setBadgeGravity(BadgeDrawable.TOP_END);
        hasUnreadMessages = true;
    }

    private void clearChatBadge() {
        bottomNavigationView.removeBadge(R.id.imgMessage);
        hasUnreadMessages = false;
    }

    private void updateChatBadge() {
        if (hasUnreadMessages) {
            showChatBadge();
        } else {
            clearChatBadge();
        }
    }

    public void onNewMessageReceived() {
        hasUnreadMessages = true;
        updateChatBadge();
    }

    private String getStatusMessage(int status) {
        switch (status) {
            case 0:
                return "Đơn hàng của bạn đang chờ xử lý";
            case 1:
                return "Đơn hàng của bạn đã được chấp nhận";
            case 2:
                return "Đơn hàng của bạn đang được giao";
            case 3:
                return "Đơn hàng của bạn đã giao thành công";
            case 4:
                return "Đơn hàng của bạn đã bị hủy";
            default:
                return "Trạng thái không xác định";
        }
    }

    private void handleNotificationNavigation() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("navigate_to_notifications", false)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_category);
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        handler.removeCallbacks(checkNotificationsRunnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        unregisterReceiver(orderReceiver);
        super.onDestroy();
    }
}