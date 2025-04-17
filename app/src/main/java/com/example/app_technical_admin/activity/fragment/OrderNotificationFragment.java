package com.example.app_technical_admin.activity.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.NotificationAdapter;
import com.example.app_technical_admin.model.NotificationModel;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrderNotificationFragment extends Fragment {

    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList;
    private FirebaseFirestore db;
    private TextView emptyTextView;
    private static final String LAST_ACCESS_TIMESTAMP_KEY = "last_access_timestamp";
    private Set<String> displayedNotifications = new HashSet<>();

    public OrderNotificationFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_notification, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        emptyTextView = view.findViewById(R.id.emptyTextView);

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(getContext(), notificationList); // Sửa ở đây: thêm getContext()
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewNotifications.setAdapter(notificationAdapter);

        // In log UID của người dùng từ Firebase Authentication
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d("OrderNotificationFragment", "UID từ Firebase Auth: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        } else {
            Log.e("OrderNotificationFragment", "Người dùng chưa đăng nhập vào Firebase Auth");
        }

        // Lưu thời gian truy cập cuối cùng
        saveLastAccessTimestamp();

        loadExistingNotifications();
        listenForNotifications();

        return view;
    }

    private void saveLastAccessTimestamp() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        editor.putString(LAST_ACCESS_TIMESTAMP_KEY, currentTimestamp);
        editor.apply();
        Log.d("OrderNotificationFragment", "Đã lưu thời gian truy cập cuối cùng: " + currentTimestamp);
    }

    private String getLastAccessTimestamp() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String lastAccessTimestamp = preferences.getString(LAST_ACCESS_TIMESTAMP_KEY, null);
        Log.d("OrderNotificationFragment", "Thời gian truy cập cuối cùng: " + lastAccessTimestamp);
        return lastAccessTimestamp;
    }

    private void loadExistingNotifications() {
        if (Utils.user_current == null || Utils.user_current.getId() <= 0) {
            Log.e("OrderNotificationFragment", "Người dùng chưa đăng nhập hoặc ID không hợp lệ: " + (Utils.user_current == null ? "null" : Utils.user_current.getId()));
            recyclerViewNotifications.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText("Vui lòng đăng nhập để xem thông báo");
            return;
        }

        int userId = Utils.user_current.getId();
        Log.d("OrderNotificationFragment", "Đang tải thông báo cho userId: " + userId);

        Query query = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        String lastAccessTimestamp = getLastAccessTimestamp();
        if (lastAccessTimestamp != null) {
            query = query.whereGreaterThan("timestamp", lastAccessTimestamp);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    displayedNotifications.clear(); // Xóa danh sách đã hiển thị
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        NotificationModel notification = doc.toObject(NotificationModel.class);
                        notification.setId(doc.getId());

                        // Tạo key duy nhất cho thông báo
                        String notificationKey = notification.getOrderId() + "|" + notification.getTitle() + "|" + notification.getBody();

                        // Kiểm tra trùng lặp
                        if (!displayedNotifications.contains(notificationKey)) {
                            displayedNotifications.add(notificationKey);
                            notificationList.add(notification);
                            Log.d("OrderNotificationFragment", "Tải thông báo: " + notification.getTitle() + ", userId: " + notification.getUserId());
                        }
                    }

                    if (notificationList.isEmpty() && lastAccessTimestamp != null) {
                        db.collection("notifications")
                                .whereEqualTo("userId", userId)
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener(allNotifications -> {
                                    for (DocumentSnapshot doc : allNotifications) {
                                        NotificationModel notification = doc.toObject(NotificationModel.class);
                                        notification.setId(doc.getId());

                                        String notificationKey = notification.getOrderId() + "|" + notification.getTitle() + "|" + notification.getBody();
                                        if (!displayedNotifications.contains(notificationKey)) {
                                            displayedNotifications.add(notificationKey);
                                            notificationList.add(notification);
                                            Log.d("OrderNotificationFragment", "Tải tất cả thông báo: " + notification.getTitle() + ", userId: " + notification.getUserId());
                                        }
                                    }
                                    notificationAdapter.notifyDataSetChanged();
                                    if (notificationList.isEmpty()) {
                                        recyclerViewNotifications.setVisibility(View.GONE);
                                        emptyTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        recyclerViewNotifications.setVisibility(View.VISIBLE);
                                        emptyTextView.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("OrderNotificationFragment", "Error loading all notifications: " + e.getMessage());
                                    recyclerViewNotifications.setVisibility(View.GONE);
                                    emptyTextView.setVisibility(View.VISIBLE);
                                    emptyTextView.setText("Lỗi khi tải thông báo: " + e.getMessage());
                                });
                    } else {
                        notificationAdapter.notifyDataSetChanged();
                        if (notificationList.isEmpty()) {
                            recyclerViewNotifications.setVisibility(View.GONE);
                            emptyTextView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerViewNotifications.setVisibility(View.VISIBLE);
                            emptyTextView.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderNotificationFragment", "Error loading existing notifications: " + e.getMessage());
                    recyclerViewNotifications.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    emptyTextView.setText("Lỗi khi tải thông báo: " + e.getMessage());
                });
    }

    private void listenForNotifications() {
        if (Utils.user_current == null || Utils.user_current.getId() <= 0) {
            Log.e("OrderNotificationFragment", "Người dùng chưa đăng nhập hoặc ID không hợp lệ: " + (Utils.user_current == null ? "null" : Utils.user_current.getId()));
            recyclerViewNotifications.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText("Vui lòng đăng nhập để xem thông báo");
            return;
        }

        int userId = Utils.user_current.getId();
        Log.d("OrderNotificationFragment", "Đang lắng nghe thông báo cho userId: " + userId);

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("OrderNotificationFragment", "Error listening for notifications: " + error.getMessage());
                        recyclerViewNotifications.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText("Lỗi khi lắng nghe thông báo: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    NotificationModel notification = dc.getDocument().toObject(NotificationModel.class);
                                    notification.setId(dc.getDocument().getId());

                                    // Tạo một key duy nhất cho thông báo dựa trên orderId, title và body
                                    String notificationKey = notification.getOrderId() + "|" + notification.getTitle() + "|" + notification.getBody();

                                    // Kiểm tra xem thông báo đã được hiển thị chưa
                                    if (!displayedNotifications.contains(notificationKey)) {
                                        displayedNotifications.add(notificationKey);
                                        notificationList.add(0, notification);
                                        notificationAdapter.notifyItemInserted(0);
                                        recyclerViewNotifications.scrollToPosition(0);
                                        Log.d("OrderNotificationFragment", "Thông báo mới: " + notification.getTitle() + ", userId: " + notification.getUserId());
                                    } else {
                                        Log.d("OrderNotificationFragment", "Thông báo trùng lặp, không thêm: " + notification.getTitle());
                                    }

                                    if (notificationList.isEmpty()) {
                                        recyclerViewNotifications.setVisibility(View.GONE);
                                        emptyTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        recyclerViewNotifications.setVisibility(View.VISIBLE);
                                        emptyTextView.setVisibility(View.GONE);
                                    }
                                    break;
                                case MODIFIED:
                                    break;
                                case REMOVED:
                                    break;
                            }
                        }
                    }
                });
    }
}