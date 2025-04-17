package com.example.app_technical_admin.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.MainActivity;
import com.example.app_technical_admin.model.Order;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderStatusListenerService extends Service {
    private FirebaseFirestore db;
    private static final String CHANNEL_ID = "order_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();
        listenForOrderStatusChanges();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Đảm bảo service khởi động lại nếu bị kill
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Order Notifications";
            String description = "Notifications for order updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("Notification", "Notification channel created in service");
            } else {
                Log.e("Notification", "NotificationManager is null when creating channel in service");
            }
        }
    }

    private void listenForOrderStatusChanges() {
        int userId = Utils.user_current.getId();
        if (userId <= 0) {
            Log.e("OrderStatusListenerService", "User ID không hợp lệ: " + userId);
            stopSelf();
            return;
        }

        Log.d("OrderStatusListenerService", "Lắng nghe thay đổi trạng thái đơn hàng cho userId: " + userId);

        db.collection("orders")
                .whereEqualTo("id_user", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Lỗi lắng nghe thay đổi trạng thái đơn hàng: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_image)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(orderId, builder.build());
            Log.d("Notification", "Notification sent from service: " + title + " - " + body);
        } else {
            Log.e("Notification", "NotificationManager is null in service");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("OrderStatusListenerService", "Service destroyed");
    }
}