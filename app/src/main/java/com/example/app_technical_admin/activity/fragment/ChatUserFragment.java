package com.example.app_technical_admin.activity.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.MainActivity;
import com.example.app_technical_admin.adapter.ChatAdapter;
import com.example.app_technical_admin.model.ChatMessage;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatUserFragment extends Fragment {

    private static final Logger log = LoggerFactory.getLogger(ChatUserFragment.class);
    private RecyclerView recyclerViewChat;
    private ImageView imgSend;
    private EditText edtMess;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ChatAdapter adapter;
    private List<ChatMessage> list;
    private boolean isChatVisible = false;

    public ChatUserFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        initView(view);
        initControl();
        listenMess();
        insertUser();
        return view;
    }

    private void insertUser() {
        if (auth.getCurrentUser() == null) {
            Log.w("ChatUserFragment", "Cannot insert user: User is not logged in");
            Toast.makeText(requireContext(), "Please log in to proceed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Utils.user_current == null || Utils.user_current.getId() <= 0) {
            Log.w("ChatUserFragment", "Cannot insert user: user_current is invalid or ID is not set");
            Toast.makeText(requireContext(), "User data is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ChatUserFragment", "Firebase UID: " + auth.getCurrentUser().getUid());
        Log.d("ChatUserFragment", "Utils.user_current.getId(): " + Utils.user_current.getId());

        HashMap<String, Object> user = new HashMap<>();
        user.put("id", Utils.user_current.getId());
        user.put("userName", Utils.user_current.getUserName());
        db.collection("users").document(String.valueOf(Utils.user_current.getId())).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatUserFragment", "User inserted successfully: ID=" + Utils.user_current.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatUserFragment", "Failed to insert user: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to insert user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
            Toast.makeText(requireContext(), "Please log in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }

        String str_mess = edtMess.getText().toString().trim();
        if (TextUtils.isEmpty(str_mess)) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
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

                    // Kiểm tra nếu tin nhắn đến từ người khác và fragment không hiển thị
                    if (!chatMessage.sendId.equals(String.valueOf(Utils.user_current.getId())) && !isChatVisible) {
                        notifyNewMessage();
                    }
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

    private void notifyNewMessage() {
        // Gọi onNewMessageReceived() từ MainActivity
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).onNewMessageReceived();
        }
    }

    private String format_date(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy- hh:mm a", Locale.getDefault()).format(date);
    }

    private void initView(View view) {
        list = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        recyclerViewChat = view.findViewById(R.id.recycleview_chat);
        imgSend = view.findViewById(R.id.imageChat);
        edtMess = view.findViewById(R.id.edtInputText);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setHasFixedSize(true);
        adapter = new ChatAdapter(requireContext(), list, String.valueOf(Utils.user_current.getId()));
        recyclerViewChat.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        isChatVisible = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isChatVisible = false;
    }
}