package com.example.app_technical_admin.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.ChatAdapter;
import com.example.app_technical_admin.model.ChatMessage;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener; // Sửa import

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    RecyclerView recyclerViewChat;
    ImageView imgSend;
    EditText edtMess;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ChatAdapter adapter;
    List<ChatMessage> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initView();
        initControl();
        listenMess();
        insertUser();
    }

    private void insertUser() {
        HashMap<String, Object> user = new HashMap<>();
        user.put("id", Utils.user_current.getId());
        user.put("userName", Utils.user_current.getUserName());
        db.collection("users").document(String.valueOf(Utils.user_current.getId())).set(user);
    }
    private void initControl() {
        imgSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessToFire();
            }
        });
    }

    private void sendMessToFire() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }

        String str_mess = edtMess.getText().toString().trim();
        if (TextUtils.isEmpty(str_mess)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Utils.SENDID, String.valueOf(Utils.user_current.getId()));
            message.put(Utils.RECEIVEDID, Utils.ID_RECEIVED);
            message.put(Utils.MESSAGE, str_mess);
            message.put(Utils.DATETIME, new Date());
            db.collection(Utils.PATH_CHAT).add(message);
            edtMess.setText("");

        }
    }

    private void listenMess() {
        db.collection(Utils.PATH_CHAT)
                .whereEqualTo(Utils.SENDID, String.valueOf(Utils.user_current.getId()))
                .whereEqualTo(Utils.RECEIVEDID, Utils.ID_RECEIVED)
                .addSnapshotListener(eventListener);
        db.collection(Utils.PATH_CHAT)
                .whereEqualTo(Utils.SENDID, Utils.ID_RECEIVED)
                .whereEqualTo(Utils.RECEIVEDID, String.valueOf(Utils.user_current.getId()))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = list.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.sendId = documentChange.getDocument().getString(Utils.SENDID);
                    chatMessage.receivedId = documentChange.getDocument().getString(Utils.RECEIVEDID);
                    chatMessage.message = documentChange.getDocument().getString(Utils.MESSAGE);
                    chatMessage.dateObj = documentChange.getDocument().getDate(Utils.DATETIME);
                    chatMessage.datetime = format_date(documentChange.getDocument().getDate(Utils.DATETIME));
                    list.add(chatMessage);
                }
            }

            Collections.sort(list, (obj1, obj2) -> obj1.dateObj.compareTo(obj2.dateObj));
            if (count == 0) {
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyItemRangeInserted(list.size(), list.size());
                recyclerViewChat.smoothScrollToPosition(list.size() - 1);
            }
        }
    };

    private String format_date(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy- hh:mm a", Locale.getDefault()).format(date);
    }

    private void initView() {
        list = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        recyclerViewChat = findViewById(R.id.recycleview_chat);
        imgSend = findViewById(R.id.imageChat);
        edtMess = findViewById(R.id.edtInputText);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setHasFixedSize(true);
        adapter = new ChatAdapter(getApplicationContext(), list, String.valueOf(Utils.user_current.getId()));
        recyclerViewChat.setAdapter(adapter);
    }
}