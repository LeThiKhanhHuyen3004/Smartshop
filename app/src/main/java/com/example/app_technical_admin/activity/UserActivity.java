package com.example.app_technical_admin.activity;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.UserAdapter;
import com.example.app_technical_admin.model.User;
import com.example.app_technical_admin.utils.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView recyclerView;
    UserAdapter userAdapter;
    List<User> userList;
    Map<Integer, Boolean> newMessageStatus; // Map để lưu trạng thái tin nhắn mới

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initView();
        initToolbar();
        getUserFromFile();
    }

    private void getUserFromFile() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            userList = new ArrayList<>();
                            newMessageStatus = new HashMap<>(); // Khởi tạo Map
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                User user = new User();
                                user.setId(documentSnapshot.getLong("id").intValue());
                                user.setUserName(documentSnapshot.getString("userName"));
                                userList.add(user);
                                newMessageStatus.put(user.getId(), false); // Khởi tạo trạng thái không có tin nhắn mới
                            }

                            if (userList.size() > 0) {
                                userAdapter = new UserAdapter(getApplicationContext(), userList, newMessageStatus);
                                recyclerView.setAdapter(userAdapter);
                                listenForNewMessages(); // Bắt đầu lắng nghe tin nhắn mới
                            }
                        }
                    }
                });
    }

    private void listenForNewMessages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (User user : userList) {
            // Lắng nghe tin nhắn từ user này gửi đến admin
            db.collection(Utils.PATH_CHAT)
                    .whereEqualTo(Utils.SENDID, String.valueOf(user.getId()))
                    .whereEqualTo(Utils.RECEIVEDID, String.valueOf(Utils.user_current.getId()))
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(QuerySnapshot value, com.google.firebase.firestore.FirebaseFirestoreException error) {
                            if (error != null) {
                                return;
                            }
                            if (value != null) {
                                for (DocumentChange documentChange : value.getDocumentChanges()) {
                                    if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                        // Có tin nhắn mới từ user này
                                        newMessageStatus.put(user.getId(), true);
                                        int position = userList.indexOf(user);
                                        if (position != -1) {
                                            userAdapter.notifyItemChanged(position);
                                        }
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycleview_user);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
    }
}