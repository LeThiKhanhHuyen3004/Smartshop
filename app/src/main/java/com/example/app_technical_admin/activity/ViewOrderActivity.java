package com.example.app_technical_admin.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.fragment.OrderNotificationFragment;
import com.example.app_technical_admin.adapter.OrderAdapter;
import com.example.app_technical_admin.model.EventBus.OrderEvent;
import com.example.app_technical_admin.model.NotiSendData;
import com.example.app_technical_admin.model.NotificationModel;
import com.example.app_technical_admin.model.Order;
import com.example.app_technical_admin.model.OrderModel;
import com.example.app_technical_admin.retrofit.ApiPushNotification;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.retrofit.RetrofitClientNoti;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.paperdb.Paper;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import android.app.NotificationChannel;
import android.os.Build;

public class ViewOrderActivity extends AppCompatActivity {
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiSale apiSale;
    RecyclerView reOrder;
    Toolbar toolbar;
    OrderAdapter adapter;
    Order order;
    int status;
    AlertDialog dialog;
    FirebaseFirestore db;
    private boolean isDialogShowing = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order);

        db = FirebaseFirestore.getInstance();

        Utils.initUser(this);

        checkAndRefreshUserData();
        createNotificationChannel();
    }




    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Order Notifications";
            String description = "Notifications for order updates";
            int importance = NotificationManager.IMPORTANCE_HIGH; // Đặt mức độ quan trọng cao
            NotificationChannel channel = new NotificationChannel("order_channel", name, importance);
            channel.setDescription(description);
            channel.enableVibration(true); // Bật rung
            channel.setShowBadge(true); // Hiển thị badge trên icon ứng dụng

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("Notification", "Notification channel created");
            } else {
                Log.e("Notification", "NotificationManager is null when creating channel");
            }
        }
    }

    private void sendNotification(String title, String body, int orderId) {
        // Tạo notification channel trước khi gửi thông báo
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigate_to_notifications", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "order_channel")
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Tăng mức độ ưu tiên
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
    private void checkAndRefreshUserData() {
        if (Utils.user_current == null || Utils.user_current.getId() <= 0 || Utils.user_current.getEmail() == null) {
            Log.w("ViewOrderActivity", "Thông tin người dùng không đầy đủ, làm mới từ server");
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
                                        initView();
                                        initToolbar();
                                        getOrder();
                                    } else {
                                        Toast.makeText(this, "Không thể làm mới dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    }
                                },
                                throwable -> {
                                    Log.e("ViewOrderActivity", "Lỗi làm mới dữ liệu: " + throwable.getMessage());
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
            initView();
            initToolbar();
            getOrder();
        }
    }

    private void getOrder() {
        int userId = Utils.user_current.getId();
        Log.d("ViewOrderActivity", "Lấy đơn hàng với userId: " + userId);
        if (userId <= 0) {
            Toast.makeText(this, "ID người dùng không hợp lệ, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        compositeDisposable.add(apiSale.viewOrder(0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orderModel -> {
                            Log.d("API_RESPONSE", "JSON trả về: " + new Gson().toJson(orderModel));
                            if (orderModel != null && orderModel.getResult() != null && !orderModel.getResult().isEmpty()) {
                                syncOrdersToFirestore(orderModel.getResult());
                                adapter = new OrderAdapter(getApplicationContext(), orderModel.getResult());
                                reOrder.setAdapter(adapter);
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

    private void syncOrdersToFirestore(List<Order> orders) {
        for (Order order : orders) {
            db.collection("orders")
                    .document(String.valueOf(order.getId()))
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            Map<String, Object> orderData = new HashMap<>();
                            orderData.put("id", order.getId());
                            orderData.put("id_user", order.getId_user());
                            orderData.put("address", order.getAddress());
                            orderData.put("phoneNumber", order.getPhoneNumber());
                            orderData.put("email", order.getEmail());
                            orderData.put("total", order.getTotal());
                            orderData.put("status", order.getStatus());
                            orderData.put("userName", order.getUserName());
                            orderData.put("orderDate", order.getOrderDate());
                            orderData.put("momo", order.getMomo());
                            orderData.put("item", order.getItem());

                            db.collection("orders")
                                    .document(String.valueOf(order.getId()))
                                    .set(orderData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Đơn hàng " + order.getId() + " đã được thêm vào Firestore");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Lỗi khi thêm đơn hàng " + order.getId() + ": " + e.getMessage());
                                    });
                        } else {
                            Log.d("Firestore", "Đơn hàng " + order.getId() + " đã tồn tại trong Firestore");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Lỗi khi kiểm tra đơn hàng " + order.getId() + ": " + e.getMessage());
                    });
        }
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

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void initView() {
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        reOrder = findViewById(R.id.recycleview_order);
        toolbar = findViewById(R.id.toolbar);
        reOrder.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showCustomDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_order, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_background);
        }

        Spinner spinner = view.findViewById(R.id.spinner_dialog);
        AppCompatButton btnConsent = view.findViewById(R.id.consent_dialog);

        List<String> list = new ArrayList<>();
        list.add("Order is pending");
        list.add("Order accept");
        list.add("Order is delivering");
        list.add("Order deliver successfully");
        list.add("Order cancel");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);
        spinner.setSelection(order.getStatus());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                status = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btnConsent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateOrder();
            }
        });

        dialog.setOnDismissListener(dialogInterface -> isDialogShowing = false);
        dialog.show();
        this.dialog = dialog;
    }

    @Subscribe(sticky = false, threadMode = ThreadMode.MAIN)
    public void eventOrder(OrderEvent event) {
        if (event != null && !isDialogShowing) {
            order = event.getOrder();
            showCustomDialog();
            isDialogShowing = true;
        }
    }

    private void updateOrder() {
        compositeDisposable.add(apiSale.updateOrder(order.getId(), status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("status", status);
                            db.collection("orders")
                                    .document(String.valueOf(order.getId()))
                                    .update(updateData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Cập nhật trạng thái đơn hàng " + order.getId() + " thành công");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Lỗi khi cập nhật trạng thái đơn hàng " + order.getId() + ": " + e.getMessage());
                                    });

                            getOrder();
                            dialog.dismiss();

                            // Lưu thông báo cho client (dựa trên id_user của đơn hàng)
                            int clientUserId = order.getId_user();
                            saveNotificationToFirestoreWithRetry("Order Status Updated", orderStatus(status), order.getId(), clientUserId, 3);

                            // (Tùy chọn) Lưu thông báo cho admin
                            int adminUserId = Utils.user_current.getId();
                            saveNotificationToFirestoreWithRetry("Order Status Updated (Admin)", "Order #" + order.getId() + " status updated to: " + orderStatus(status), order.getId(), adminUserId, 3);

                            // Gửi thông báo push cho client
                            pushNotiToUser();
                        },
                        throwable -> {
                            Log.e("UpdateOrder", "Lỗi cập nhật đơn hàng: " + throwable.getMessage());
                            Toast.makeText(getApplicationContext(), "Lỗi cập nhật đơn hàng: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }    private void saveNotificationToFirestoreWithRetry(String title, String body, int orderId, int userId, int maxRetries) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        NotificationModel notification = new NotificationModel(null, title, body, timestamp, orderId, userId, false);

        // Kiểm tra xem thông báo với orderId và title đã tồn tại chưa
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("title", title)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Nếu không có thông báo trùng lặp, tiến hành lưu
                        saveWithRetry(notification, maxRetries, 0);
                    } else {
                        // Nếu đã tồn tại, cập nhật body của thông báo hiện có
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            db.collection("notifications")
                                    .document(doc.getId())
                                    .update("body", body, "timestamp", timestamp)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Cập nhật thông báo cho orderId: " + orderId + ", userId: " + userId);
                                        // Gửi thông báo cục bộ sau khi cập nhật
                                        if (userId == Utils.user_current.getId()) {
                                            sendNotification(title, body, orderId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Lỗi khi cập nhật thông báo: " + e.getMessage());
                                        // Gửi thông báo cục bộ ngay cả khi cập nhật thất bại
                                        if (userId == Utils.user_current.getId()) {
                                            sendNotification(title, body, orderId);
                                        }
                                    });
                            break; // Chỉ cập nhật document đầu tiên (nếu có nhiều document trùng lặp)
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Lỗi khi kiểm tra thông báo trùng lặp: " + e.getMessage());
                    // Nếu kiểm tra thất bại, vẫn thử lưu
                    saveWithRetry(notification, maxRetries, 0);
                });
    }

    private void saveWithRetry(NotificationModel notification, int maxRetries, int currentRetry) {
        if (currentRetry >= maxRetries) {
            Log.e("Firestore", "Failed to save notification after " + maxRetries + " retries");
            Toast.makeText(this, "Không thể lưu thông báo sau " + maxRetries + " lần thử", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Notification saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error saving notification (Retry " + (currentRetry + 1) + "): " + e.getMessage());
                    // Retry after a delay
                    new android.os.Handler().postDelayed(() -> saveWithRetry(notification, maxRetries, currentRetry + 1), 2000);
                });
    }

    private void pushNotiToUser() {
        compositeDisposable.add(apiSale.getTokenAdmin(order.getId_user())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userModel -> {
                            if (userModel.isSuccess()) {
                                for (int i = 0; i < userModel.getResult().size(); i++) {
                                    Map<String, String> data = new HashMap<>();
                                    data.put("title", "Order Status Update");
                                    data.put("body", orderStatus(status));
                                    NotiSendData notiSendData = new NotiSendData(userModel.getResult().get(i).getToken(), data);
                                    ApiPushNotification apiPushNotification = RetrofitClientNoti.getInstance().create(ApiPushNotification.class);
                                    compositeDisposable.add(apiPushNotification.sendNotification(notiSendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                    notiResponse -> {
                                                        Log.d("FCM Response", "Success: " + notiResponse.getName());
                                                    },
                                                    throwable -> {
                                                        Log.e("FCM Error", "Error: " + throwable.getMessage());
                                                    }
                                            ));
                                }
                            } else {
                                Log.w("PushNoti", "Không tìm thấy token của user với id: " + order.getId_user());
                            }
                        },
                        throwable -> {
                            Log.e("PushNoti", "Lỗi lấy token: " + throwable.getMessage());
                        }
                ));
    }



    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}