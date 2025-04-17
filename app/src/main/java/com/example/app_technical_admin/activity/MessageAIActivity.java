package com.example.app_technical_admin.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.MessageAIAdapter;
import com.example.app_technical_admin.model.ChatAIHistoryResponse;
import com.example.app_technical_admin.model.ChatAIMessage;
import com.example.app_technical_admin.model.ChatAIResponse;
import com.example.app_technical_admin.model.ItemForAI;
import com.example.app_technical_admin.model.MessageAI;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.OrderForAI;
import com.example.app_technical_admin.model.OrderResponse;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;

public class MessageAIActivity extends AppCompatActivity {
    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private AppCompatButton buttonConsult, buttonAskProduct, buttonSupport;
    private ArrayList<MessageAI> messageAIs;
    private MessageAIAdapter adapter;
    private RequestQueue requestQueue;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private String imagePath;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ApiSale apiSale;
    private List<NewProduct> productList;
    private int userId;
    private boolean isWaitingForSuggestionResponse = false;
    private List<OrderForAI> orderList;
    private OrderForAI latestOrder;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MessageAIPrefs";
    private static final String KEY_LATEST_ORDER_DATE = "latestOrderDate";
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 3000;

    // Loại bỏ BroadcastReceiver vì không cần tự động gợi ý khi có đơn hàng mới
    private BroadcastReceiver orderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MessageAIActivity", "Received broadcast: NEW_ORDER_PLACED");
            // Không gọi checkForNewOrder nữa
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_ai);

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonConsult = findViewById(R.id.buttonConsult);
        buttonAskProduct = findViewById(R.id.buttonAskProduct);
        buttonSupport = findViewById(R.id.buttonSupport);
        messageAIs = new ArrayList<>();
        adapter = new MessageAIAdapter(messageAIs);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        userId = Utils.user_current.getId();
        Log.d("MessageAIActivity", "User ID: " + userId);
        if (userId <= 0) {
            Log.e("USER_ERROR", "User not logged in. Redirecting to LoginActivity.");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Thiết lập Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        fetchProductListAndThenLoadChatHistory();

        fetchOrderHistory();

        if (savedInstanceState != null) {
            ArrayList<MessageAI> savedMessages = savedInstanceState.getParcelableArrayList("messages");
            if (savedMessages != null && !savedMessages.isEmpty()) {
                messageAIs.clear();
                messageAIs.addAll(savedMessages);
                adapter.notifyDataSetChanged();
                if (messageAIs.size() > 0) {
                    recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                }
            }
        }

        // Đăng ký BroadcastReceiver nhưng không gọi checkForNewOrder tự động
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(orderReceiver, new IntentFilter("NEW_ORDER_PLACED"), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(orderReceiver, new IntentFilter("NEW_ORDER_PLACED"));
        }

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editTextMessage.getText().toString().trim().toLowerCase();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    editTextMessage.setText("");
                }
            }
        });

        buttonConsult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Hãy tư vấn cho tôi một sản phẩm công nghệ phù hợp.";
                sendMessage(message);
            }
        });

        buttonAskProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Tôi muốn biết thông tin về điện thoại/máy tính bảng/dây sạc.";
                sendMessage(message);
            }
        });

        buttonSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Tôi cần hỗ trợ kỹ thuật về sản phẩm công nghệ.";
                sendMessage(message);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message_ai, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_chat) {
            clearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearChat() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa đoạn chat")
                .setMessage("Bạn có chắc chắn muốn xóa toàn bộ đoạn chat không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    apiSale.clearChat(userId).enqueue(new Callback<ChatAIResponse>() {
                        @Override
                        public void onResponse(Call<ChatAIResponse> call, retrofit2.Response<ChatAIResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ChatAIResponse chatResponse = response.body();
                                if (chatResponse.isSuccess()) {
                                    Log.d("MessageAIActivity", "Chat history cleared successfully in database");
                                    messageAIs.clear();
                                    adapter.notifyDataSetChanged();
                                    // Gọi lại loadChatHistory để kiểm tra trạng thái chat trống và hiển thị lời chào nếu cần
                                    loadChatHistory();
                                } else {
                                    Log.e("MessageAIActivity", "Failed to clear chat history: " + chatResponse.getMessage());
                                    messageAIs.add(new MessageAI("Không thể xóa đoạn chat: " + chatResponse.getMessage(), MessageAI.TYPE_BOT));
                                    adapter.notifyItemInserted(messageAIs.size() - 1);
                                    recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                    saveMessageToDatabase("Không thể xóa đoạn chat: " + chatResponse.getMessage(), MessageAI.TYPE_BOT, null);
                                }
                            } else {
                                Log.e("MessageAIActivity", "Failed to clear chat history: Response not successful, code: " + (response != null ? response.code() : "null"));
                                messageAIs.add(new MessageAI("Không thể xóa đoạn chat: Lỗi kết nối", MessageAI.TYPE_BOT));
                                adapter.notifyItemInserted(messageAIs.size() - 1);
                                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                saveMessageToDatabase("Không thể xóa đoạn chat: Lỗi kết nối", MessageAI.TYPE_BOT, null);
                            }
                        }

                        @Override
                        public void onFailure(Call<ChatAIResponse> call, Throwable t) {
                            Log.e("MessageAIActivity", "Error clearing chat history: " + t.getMessage());
                            messageAIs.add(new MessageAI("Không thể xóa đoạn chat: " + t.getMessage(), MessageAI.TYPE_BOT));
                            adapter.notifyItemInserted(messageAIs.size() - 1);
                            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                            saveMessageToDatabase("Không thể xóa đoạn chat: " + t.getMessage(), MessageAI.TYPE_BOT, null);
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .setCancelable(true)
                .show();
    }

    private void fetchProductListAndThenLoadChatHistory() {
        Log.d("MessageAIActivity", "fetchProductListAndThenLoadChatHistory called.");
        compositeDisposable.add(apiSale.getNewProduct()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        newProductModel -> {
                            Log.d("MessageAIActivity", "API getNewProduct response received.");
                            if (newProductModel.isSuccess()) {
                                productList = newProductModel.getResult();
                                Log.d("API_SUCCESS", "Loaded " + (productList != null ? productList.size() : 0) + " products");
                                if (productList != null) {
                                    for (NewProduct product : productList) {
                                        Log.d("MessageAIActivity", "Product: " + product.getProductName() + ", ID: " + product.getId() + ", Category: " + product.getCategory());
                                    }
                                }
                                loadChatHistory();
                            } else {
                                Log.e("API_ERROR", newProductModel.getMessage());
                                messageAIs.add(new MessageAI("Không thể tải danh sách sản phẩm: " + newProductModel.getMessage(), MessageAI.TYPE_BOT));
                                adapter.notifyItemInserted(messageAIs.size() - 1);
                                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                saveMessageToDatabase("Không thể tải danh sách sản phẩm: " + newProductModel.getMessage(), MessageAI.TYPE_BOT, null);
                                loadChatHistory();
                            }
                        },
                        throwable -> {
                            Log.e("API_ERROR", "Error fetching products: " + throwable.getMessage());
                            messageAIs.add(new MessageAI("Lỗi khi tải danh sách sản phẩm: " + throwable.getMessage(), MessageAI.TYPE_BOT));
                            adapter.notifyItemInserted(messageAIs.size() - 1);
                            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                            saveMessageToDatabase("Lỗi khi tải danh sách sản phẩm: " + throwable.getMessage(), MessageAI.TYPE_BOT, null);
                            loadChatHistory();
                        }
                ));
    }

    // Loại bỏ onResume để không tự động kiểm tra đơn hàng mới
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MessageAIActivity", "onResume called.");
        // Không gọi checkForNewOrder nữa
    }

    private void checkForNewOrder(int retryCount) {
        Log.d("MessageAIActivity", "checkForNewOrder called, retry count: " + retryCount);
        String lastOrderDate = sharedPreferences.getString(KEY_LATEST_ORDER_DATE + userId, null);
        Log.d("MessageAIActivity", "Last saved order date: " + lastOrderDate);

        compositeDisposable.add(apiSale.viewOrderAI(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orderResponse -> {
                            Log.d("MessageAIActivity", "API viewOrderAI response received.");
                            if (orderResponse.isSuccess()) {
                                orderList = orderResponse.getResult();
                                Log.d("MessageAIActivity", "Loaded " + (orderList != null ? orderList.size() : 0) + " orders");
                                if (orderList == null || orderList.isEmpty()) {
                                    Log.d("MessageAIActivity", "No orders found for user_id: " + userId);
                                    if (retryCount < MAX_RETRIES) {
                                        Log.d("MessageAIActivity", "Retrying to fetch orders, attempt " + (retryCount + 1));
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> checkForNewOrder(retryCount + 1), RETRY_DELAY_MS);
                                    } else {
                                        Log.d("MessageAIActivity", "Max retries reached, no orders found.");
                                    }
                                    return;
                                }

                                latestOrder = findLatestOrder(orderList);
                                if (latestOrder == null || latestOrder.getOrderDate() == null) {
                                    Log.d("MessageAIActivity", "Latest order is null or has no date.");
                                    if (retryCount < MAX_RETRIES) {
                                        Log.d("MessageAIActivity", "Retrying to fetch orders, attempt " + (retryCount + 1));
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> checkForNewOrder(retryCount + 1), RETRY_DELAY_MS);
                                    }
                                    return;
                                }

                                String currentOrderDate = latestOrder.getOrderDate();
                                Log.d("MessageAIActivity", "Current latest order date: " + currentOrderDate);

                                if (lastOrderDate == null || !lastOrderDate.equals(currentOrderDate)) {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(KEY_LATEST_ORDER_DATE + userId, currentOrderDate);
                                    editor.apply();
                                    Log.d("MessageAIActivity", "New order detected.");
                                    // Không tự động gợi ý sản phẩm
                                } else {
                                    Log.d("MessageAIActivity", "No new order detected. Last order date matches current: " + lastOrderDate);
                                }
                            } else {
                                Log.e("ORDER_CHECK_ERROR", "Failed to check new order: " + orderResponse.getMessage());
                                if (retryCount < MAX_RETRIES) {
                                    Log.d("MessageAIActivity", "Retrying to fetch orders, attempt " + (retryCount + 1));
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> checkForNewOrder(retryCount + 1), RETRY_DELAY_MS);
                                }
                            }
                        },
                        throwable -> {
                            Log.e("ORDER_CHECK_ERROR", "Error checking new order: " + throwable.getMessage());
                            if (retryCount < MAX_RETRIES) {
                                Log.d("MessageAIActivity", "Retrying to fetch orders, attempt " + (retryCount + 1));
                                new Handler(Looper.getMainLooper()).postDelayed(() -> checkForNewOrder(retryCount + 1), RETRY_DELAY_MS);
                            } else {
                                Log.e("MessageAIActivity", "Max retries reached, failed to fetch orders: " + throwable.getMessage());
                            }
                        }
                ));
    }

    private void fetchOrderHistory() {
        Log.d("MessageAIActivity", "fetchOrderHistory called.");
        compositeDisposable.add(apiSale.viewOrderAI(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orderResponse -> {
                            Log.d("MessageAIActivity", "API viewOrderAI response received for fetchOrderHistory.");
                            if (orderResponse.isSuccess()) {
                                orderList = orderResponse.getResult();
                                Log.d("MessageAIActivity", "Loaded " + (orderList != null ? orderList.size() : 0) + " orders");
                                if (orderList == null || orderList.isEmpty()) {
                                    Log.d("MessageAIActivity", "No orders found for user_id: " + userId);
                                } else {
                                    latestOrder = findLatestOrder(orderList);
                                    if (latestOrder != null) {
                                        Log.d("MessageAIActivity", "Latest order date from fetchOrderHistory: " + latestOrder.getOrderDate());
                                    }
                                }
                            } else {
                                Log.e("ORDER_HISTORY_ERROR", "Failed to load order history: " + orderResponse.getMessage());
                            }
                        },
                        throwable -> {
                            Log.e("ORDER_HISTORY_ERROR", "Error loading order history: " + throwable.getMessage());
                        }
                ));
    }

    private OrderForAI findLatestOrder(List<OrderForAI> orders) {
        Log.d("MessageAIActivity", "findLatestOrder called.");
        if (orders == null || orders.isEmpty()) {
            Log.d("MessageAIActivity", "findLatestOrder: Orders list is null or empty.");
            return null;
        }

        OrderForAI latest = orders.get(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            for (OrderForAI order : orders) {
                if (order.getOrderDate() == null) {
                    Log.d("MessageAIActivity", "Order with null date found, skipping.");
                    continue;
                }
                Date currentDate = dateFormat.parse(order.getOrderDate());
                Date latestDate = dateFormat.parse(latest.getOrderDate() != null ? latest.getOrderDate() : "1970-01-01");
                if (currentDate != null && latestDate != null && currentDate.after(latestDate)) {
                    latest = order;
                }
            }
        } catch (ParseException e) {
            Log.e("DATE_PARSE_ERROR", "Error parsing order date: " + e.getMessage());
        }
        Log.d("MessageAIActivity", "Latest order found: " + (latest != null ? latest.getOrderDate() : "null"));
        return latest;
    }

    private void loadChatHistory() {
        Log.d("MessageAIActivity", "Loading chat history for user_id: " + userId);
        apiSale.getChatHistory(userId).enqueue(new Callback<ChatAIHistoryResponse>() {
            @Override
            public void onResponse(Call<ChatAIHistoryResponse> call, retrofit2.Response<ChatAIHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatAIHistoryResponse chatResponse = response.body();
                    Log.d("MessageAIActivity", "Chat history response: success=" + chatResponse.isSuccess());
                    if (chatResponse.isSuccess()) {
                        List<ChatAIMessage> chatHistory = chatResponse.getChatHistory();
                        Log.d("MessageAIActivity", "Chat history size: " + (chatHistory != null ? chatHistory.size() : 0));
                        messageAIs.clear(); // Xóa danh sách hiện tại trước khi tải lịch sử

                        if (chatHistory != null && !chatHistory.isEmpty()) {
                            // Nếu có lịch sử chat, tải các tin nhắn
                            for (ChatAIMessage chatAIMessage : chatHistory) {
                                int messageType = chatAIMessage.getMessageType();
                                String messageContent = chatAIMessage.getMessageContent();
                                String productDataJson = chatAIMessage.getProductIds();

                                if (messageType == MessageAI.TYPE_USER || messageType == MessageAI.TYPE_BOT) {
                                    messageAIs.add(new MessageAI(messageContent, messageType));
                                    Log.d("MessageAIActivity", "Loaded message: " + messageContent + ", type: " + messageType);
                                } else if (messageType == MessageAI.TYPE_BOT_PRODUCT) {
                                    List<NewProduct> products = new ArrayList<>();
                                    if (productDataJson != null && !productDataJson.isEmpty()) {
                                        try {
                                            if (productDataJson.startsWith("[") && productDataJson.contains("id")) {
                                                products = new Gson().fromJson(productDataJson, new TypeToken<List<NewProduct>>() {}.getType());
                                                Log.d("MessageAIActivity", "Parsed product_data: " + products.size() + " products");
                                                for (NewProduct product : products) {
                                                    Log.d("MessageAIActivity", "Restored product: " + product.getProductName() + ", ID: " + product.getId());
                                                }
                                            } else {
                                                List<Integer> productIds = new Gson().fromJson(productDataJson, new TypeToken<List<Integer>>() {}.getType());
                                                Log.d("MessageAIActivity", "Parsed product_ids: " + productIds.toString());
                                                if (productList != null) {
                                                    for (Integer productId : productIds) {
                                                        boolean found = false;
                                                        for (NewProduct product : productList) {
                                                            if (product.getId() == productId) {
                                                                products.add(product);
                                                                found = true;
                                                                Log.d("MessageAIActivity", "Found product: " + product.getProductName() + ", ID: " + productId);
                                                                break;
                                                            }
                                                        }
                                                        if (!found) {
                                                            Log.w("MessageAIActivity", "Product with ID " + productId + " not found in productList");
                                                        }
                                                    }
                                                } else {
                                                    Log.w("MessageAIActivity", "productList is null, cannot restore products for message: " + messageContent);
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e("MessageAIActivity", "Error parsing product_data: " + e.getMessage());
                                        }
                                    } else {
                                        Log.w("MessageAIActivity", "product_data is null or empty for TYPE_BOT_PRODUCT message: " + messageContent);
                                    }

                                    if (products.isEmpty() && productDataJson != null && !productDataJson.isEmpty()) {
                                        messageAIs.add(new MessageAI(messageContent + "\n(Lưu ý: Một số sản phẩm đề xuất không còn tồn tại trong danh sách hiện tại.)", messageType, products));
                                    } else {
                                        messageAIs.add(new MessageAI(messageContent, messageType, products));
                                    }
                                    Log.d("MessageAIActivity", "Loaded product message: " + messageContent + ", type: " + messageType + ", products: " + products.size());
                                }
                            }
                            adapter.notifyDataSetChanged();
                            if (messageAIs.size() > 0) {
                                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                            }
                        } else {
                            // Nếu không có lịch sử chat (trang chat trống), hiển thị lời chào mặc định
                            Log.d("MessageAIActivity", "No chat history found for user_id: " + userId + ". Displaying welcome message.");
                            String welcomeMessage = "Xin chào! Tôi là SavyAI, trợ lý bán hàng công nghệ của SmartShop. Tôi có thể giúp bạn tìm kiếm sản phẩm, tư vấn công nghệ, hoặc hỗ trợ về đơn hàng. Bạn cần tôi giúp gì hôm nay?";
                            messageAIs.add(new MessageAI(welcomeMessage, MessageAI.TYPE_BOT));
                            adapter.notifyItemInserted(messageAIs.size() - 1);
                            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                            saveMessageToDatabase(welcomeMessage, MessageAI.TYPE_BOT, null);
                        }
                    } else {
                        Log.e("CHAT_HISTORY_ERROR", "Failed to load chat history: Response success is false");
                        // Nếu không tải được lịch sử chat, vẫn hiển thị lời chào mặc định
                        String welcomeMessage = "Xin chào! Tôi là SavyAI, trợ lý bán hàng công nghệ của SmartShop. Tôi có thể giúp bạn tìm kiếm sản phẩm, tư vấn công nghệ, hoặc hỗ trợ về đơn hàng. Bạn cần tôi giúp gì hôm nay?";
                        messageAIs.add(new MessageAI(welcomeMessage, MessageAI.TYPE_BOT));
                        adapter.notifyItemInserted(messageAIs.size() - 1);
                        recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                        saveMessageToDatabase(welcomeMessage, MessageAI.TYPE_BOT, null);
                    }
                } else {
                    Log.e("CHAT_HISTORY_ERROR", "Failed to load chat history: Response not successful, code: " + (response != null ? response.code() : "null"));
                    // Nếu không tải được lịch sử chat, vẫn hiển thị lời chào mặc định
                    String welcomeMessage = "Xin chào! Tôi là SavyAI, trợ lý bán hàng công nghệ của SmartShop. Tôi có thể giúp bạn tìm kiếm sản phẩm, tư vấn công nghệ, hoặc hỗ trợ về đơn hàng. Bạn cần tôi giúp gì hôm nay?";
                    messageAIs.add(new MessageAI(welcomeMessage, MessageAI.TYPE_BOT));
                    adapter.notifyItemInserted(messageAIs.size() - 1);
                    recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                    saveMessageToDatabase(welcomeMessage, MessageAI.TYPE_BOT, null);
                }
            }

            @Override
            public void onFailure(Call<ChatAIHistoryResponse> call, Throwable t) {
                Log.e("CHAT_HISTORY_ERROR", "Error loading chat history: " + t.getMessage());
                // Nếu không tải được lịch sử chat, vẫn hiển thị lời chào mặc định
                String welcomeMessage = "Xin chào! Tôi là SavyAI, trợ lý bán hàng công nghệ của SmartShop. Tôi có thể giúp bạn tìm kiếm sản phẩm, tư vấn công nghệ, hoặc hỗ trợ về đơn hàng. Bạn cần tôi giúp gì hôm nay?";
                messageAIs.add(new MessageAI(welcomeMessage, MessageAI.TYPE_BOT));
                adapter.notifyItemInserted(messageAIs.size() - 1);
                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                saveMessageToDatabase(welcomeMessage, MessageAI.TYPE_BOT, null);
            }
        });
    }

    private void saveMessageToDatabase(String messageContent, int messageType, List<NewProduct> products) {
        String productDataJson = null;
        if (messageType == MessageAI.TYPE_BOT_PRODUCT && products != null && !products.isEmpty()) {
            productDataJson = new Gson().toJson(products);
            Log.d("MessageAIActivity", "Saving product_data: " + productDataJson);
        }

        Log.d("MessageAIActivity", "Saving message for user_id: " + userId + ", content: " + messageContent + ", type: " + messageType);
        apiSale.saveChat(userId, messageContent, messageType, productDataJson).enqueue(new Callback<ChatAIResponse>() {
            @Override
            public void onResponse(Call<ChatAIResponse> call, retrofit2.Response<ChatAIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatAIResponse chatResponse = response.body();
                    Log.d("MessageAIActivity", "Save chat response: success=" + chatResponse.isSuccess() + ", message=" + chatResponse.getMessage());
                    if (chatResponse.isSuccess()) {
                        Log.d("CHAT_SAVE", "Message saved successfully");
                    } else {
                        Log.e("CHAT_SAVE_ERROR", "Failed to save message: " + chatResponse.getMessage());
                    }
                } else {
                    Log.e("CHAT_SAVE_ERROR", "Failed to save message: Response not successful, code: " + (response != null ? response.code() : "null"));
                }
            }

            @Override
            public void onFailure(Call<ChatAIResponse> call, Throwable t) {
                Log.e("CHAT_SAVE_ERROR", "Error saving message: " + t.getMessage());
            }
        });
    }

    private void saveMessageToDatabase(String messageContent, int messageType) {
        saveMessageToDatabase(messageContent, messageType, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Log.e("CAMERA_ERROR", "Quyền CAMERA bị từ chối!");
                messageAIs.add(new MessageAI("Bạn cần cấp quyền CAMERA để chụp ảnh!", MessageAI.TYPE_BOT));
                adapter.notifyItemInserted(messageAIs.size() - 1);
                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                saveMessageToDatabase("Bạn cần cấp quyền CAMERA để chụp ảnh!", MessageAI.TYPE_BOT);
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CAMERA_ERROR", "Lỗi tạo file ảnh: " + ex.getMessage());
                messageAIs.add(new MessageAI("Lỗi khi tạo file ảnh: " + ex.getMessage(), MessageAI.TYPE_BOT));
                adapter.notifyItemInserted(messageAIs.size() - 1);
                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                saveMessageToDatabase("Lỗi khi tạo file ảnh: " + ex.getMessage(), MessageAI.TYPE_BOT);
                return;
            }
            if (photoFile != null) {
                try {
                    Uri photoURI = FileProvider.getUriForFile(this, "com.example.app_technical_admin.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (Exception e) {
                    Log.e("CAMERA_ERROR", "Lỗi FileProvider: " + e.getMessage());
                    messageAIs.add(new MessageAI("Lỗi FileProvider: " + e.getMessage(), MessageAI.TYPE_BOT));
                    adapter.notifyItemInserted(messageAIs.size() - 1);
                    recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                    saveMessageToDatabase("Lỗi FileProvider: " + e.getMessage(), MessageAI.TYPE_BOT);
                }
            }
        } else {
            Log.e("CAMERA_ERROR", "Không tìm thấy ứng dụng camera!");
            messageAIs.add(new MessageAI("Không tìm thấy ứng dụng camera!", MessageAI.TYPE_BOT));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase("Không tìm thấy ứng dụng camera!", MessageAI.TYPE_BOT);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("Không thể truy cập thư mục lưu trữ!");
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagePath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            sendImageMessage();
        }
    }

    private void sendImageMessage() {
        if (imagePath != null) {
            messageAIs.add(new MessageAI("Hình ảnh: " + imagePath, MessageAI.TYPE_USER));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase("Hình ảnh: " + imagePath, MessageAI.TYPE_USER);
        } else {
            messageAIs.add(new MessageAI("Lỗi: Không lưu được hình ảnh!", MessageAI.TYPE_BOT));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase("Lỗi: Không lưu được hình ảnh!", MessageAI.TYPE_BOT);
        }
    }

    private void sendMessage(String message) {
        messageAIs.add(new MessageAI(message, MessageAI.TYPE_USER));
        adapter.notifyItemInserted(messageAIs.size() - 1);
        recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
        saveMessageToDatabase(message, MessageAI.TYPE_USER, null);

        if (isWaitingForSuggestionResponse) {
            isWaitingForSuggestionResponse = false;
            String lowerCaseMessage = message.toLowerCase();
            if (lowerCaseMessage.contains("có") || lowerCaseMessage.contains("ok") ||
                    lowerCaseMessage.contains("được") || lowerCaseMessage.contains("muốn")) {
                suggestRelatedProducts();
                return;
            } else {
                messageAIs.add(new MessageAI("Được rồi, nếu bạn cần tư vấn thêm, hãy cho tôi biết nhé!", MessageAI.TYPE_BOT));
                adapter.notifyItemInserted(messageAIs.size() - 1);
                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                saveMessageToDatabase("Được rồi, nếu bạn cần tư vấn thêm, hãy cho tôi biết nhé!", MessageAI.TYPE_BOT, null);
            }
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" +
                getString(R.string.gemini_api_key);

        boolean isOrderRelated = message.toLowerCase().contains("đơn hàng") ||
                message.toLowerCase().contains("order") ||
                message.toLowerCase().contains("mua hàng") ||
                message.toLowerCase().contains("trạng thái") ||
                message.toLowerCase().contains("giao hàng");

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "model");
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();

            StringBuilder productListString = new StringBuilder();
            if (productList != null && !productList.isEmpty()) {
                productListString.append("Danh sách sản phẩm hiện có:\n");
                for (NewProduct product : productList) {
                    productListString.append(String.format(
                            "- %s (ID: %d): Giá %s, Số lượng tồn: %d, Danh mục: %d, Mô tả: %s\n",
                            product.getProductName() != null ? product.getProductName() : "N/A",
                            product.getId(),
                            product.getPrice() != null ? product.getPrice() : "N/A",
                            product.getCountStock(),
                            product.getCategory(),
                            product.getDescription() != null ? product.getDescription() : "Không có mô tả"
                    ));
                }
            } else {
                productListString.append("Hiện tại không có sản phẩm nào trong cơ sở dữ liệu.");
            }

            StringBuilder orderHistoryString = new StringBuilder();
            if (orderList != null && !orderList.isEmpty()) {
                orderHistoryString.append("Lịch sử đơn hàng của người dùng:\n");
                int orderLimit = Math.min(orderList.size(), 5);
                for (int i = 0; i < orderLimit; i++) {
                    OrderForAI order = orderList.get(i);
                    String statusText = getStatusText(order.getStatus());
                    orderHistoryString.append(String.format(
                            "- Ngày đặt: %s, Tổng tiền: %s VNĐ, Trạng thái: %s, Địa chỉ: %s, Số điện thoại: %s, Email: %s\n",
                            order.getOrderDate() != null ? order.getOrderDate() : "N/A",
                            order.getTotal() != null ? order.getTotal() : "0",
                            statusText,
                            order.getAddress() != null ? order.getAddress() : "N/A",
                            order.getPhoneNumber() != null ? order.getPhoneNumber() : "N/A",
                            order.getEmail() != null ? order.getEmail() : "N/A"
                    ));
                    if (order.getItem() != null && !order.getItem().isEmpty()) {
                        orderHistoryString.append("  Chi tiết đơn hàng:\n");
                        for (ItemForAI item : order.getItem()) {
                            orderHistoryString.append(String.format(
                                    "    + Sản phẩm: %s (ID: %d), Số lượng: %d, Giá: %.0f VNĐ, Mô tả: %s\n",
                                    item.getProductName() != null ? item.getProductName() : "N/A",
                                    item.getId_product(),
                                    item.getCount(),
                                    item.getPrice(),
                                    item.getDescription() != null ? item.getDescription() : "Không có mô tả"
                            ));
                        }
                    }
                }
            } else {
                orderHistoryString.append("Người dùng chưa có đơn hàng nào.");
            }

            systemPart.put("text", "Bạn là trợ lý bán hàng công nghệ chuyên nghiệp tên là SavyAI trong một ứng dụng mua sắm mang tên SmartShop. Nhiệm vụ của bạn là trả lời câu hỏi của khách hàng về sản phẩm công nghệ (điện thoại, máy tính bảng, dây sạc, v.v.), tư vấn sản phẩm, hỗ trợ các thắc mắc liên quan đến mua sắm, và cung cấp thông tin về lịch sử đơn hàng của họ. Dựa vào danh sách sản phẩm sau để tư vấn: " + productListString.toString() + "\n\nDựa vào lịch sử đơn hàng sau để trả lời các câu hỏi về đơn hàng: " + orderHistoryString.toString() + "\n\nHãy trả lời ngắn gọn, đúng trọng tâm, giọng điệu phải thân thiện, không hỏi lại người dùng quá nhiều câu hỏi cùng lúc, chỉ đề xuất sản phẩm từ danh sách đã cung cấp, và trả lời một cách thân thiện, tự nhiên. **Khi đề xuất sản phẩm, hãy bắt đầu bằng một câu giới thiệu ngắn gọn, sau đó liệt kê và tư vấn từng sản phẩm trên một dòng riêng, cần bao gồm tên sản phẩm, ID trong ngoặc, và giá, ví dụ: Samsung Galaxy S21 (ID: 123) - Giá: 20,000,000 VNĐ. Có thể cung cấp thông tin thông số chi tiết của sản phẩm như dung lượng RAM, bộ nhớ, kích thước màn hình, v.v., nhưng không được quá dài dòng. Nếu gặp câu hỏi không liên quan đến công nghệ, sản phẩm trong cửa hàng, hoặc đơn hàng, hãy trả lời: 'Xin lỗi, tôi chỉ có thể hỗ trợ các câu hỏi liên quan đến công nghệ, các sản phẩm có trong cửa hàng, và đơn hàng của bạn. Vui lòng đặt câu hỏi liên quan để tôi có thể giúp bạn tốt nhất nhé!'");
            systemParts.put(systemPart);
            systemMessage.put("parts", systemParts);
            contentsArray.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            JSONArray userParts = new JSONArray();
            JSONObject userPart = new JSONObject();
            userPart.put("text", message);
            userParts.put(userPart);
            userMessage.put("parts", userParts);
            contentsArray.put(userMessage);

            int historyLimit = Math.min(messageAIs.size(), 5);
            for (int i = Math.max(0, messageAIs.size() - historyLimit); i < messageAIs.size(); i++) {
                MessageAI msg = messageAIs.get(i);
                if (msg.getType() == MessageAI.TYPE_USER || msg.getType() == MessageAI.TYPE_BOT) {
                    JSONObject historyMessage = new JSONObject();
                    historyMessage.put("role", msg.getType() == MessageAI.TYPE_USER ? "user" : "model");
                    JSONArray historyParts = new JSONArray();
                    JSONObject historyPart = new JSONObject();
                    historyPart.put("text", msg.getContent());
                    historyParts.put(historyPart);
                    historyMessage.put("parts", historyParts);
                    contentsArray.put(historyMessage);
                }
            }

            requestBody.put("contents", contentsArray);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        try {
                            if (response == null || !response.has("candidates")) {
                                handleError("Phản hồi không có candidates hoặc null");
                                return;
                            }

                            JSONArray candidates = response.getJSONArray("candidates");
                            if (candidates.length() == 0) {
                                handleError("Candidates rỗng");
                                return;
                            }

                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() == 0) {
                                handleError("Parts rỗng");
                                return;
                            }

                            String botReply = parts.getJSONObject(0).getString("text").replaceAll("\\*+", "");
                            List<NewProduct> mentionedProducts = extractProductsFromReply(botReply);

                            // Tách phản hồi thành các dòng
                            String[] replyLines = botReply.split("\n");
                            int productIndex = 0;

                            for (String line : replyLines) {
                                if (line.trim().isEmpty()) continue;

                                // Kiểm tra xem dòng này có phải là dòng giới thiệu không (không chứa ID sản phẩm)
                                if (!line.contains("(ID: ")) {
                                    // Hiển thị dòng văn bản giới thiệu
                                    messageAIs.add(new MessageAI(line.trim(), MessageAI.TYPE_BOT));
                                    adapter.notifyItemInserted(messageAIs.size() - 1);
                                    recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                    saveMessageToDatabase(line.trim(), MessageAI.TYPE_BOT, null);
                                } else {
                                    // Đây là dòng chứa thông tin sản phẩm
                                    if (productIndex < mentionedProducts.size()) {
                                        // Hiển thị dòng văn bản tư vấn sản phẩm
                                        messageAIs.add(new MessageAI(line.trim(), MessageAI.TYPE_BOT));
                                        adapter.notifyItemInserted(messageAIs.size() - 1);
                                        recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                        saveMessageToDatabase(line.trim(), MessageAI.TYPE_BOT, null);

                                        // Hiển thị item sản phẩm ngay sau đó
                                        List<NewProduct> singleProductList = new ArrayList<>();
                                        singleProductList.add(mentionedProducts.get(productIndex));
                                        messageAIs.add(new MessageAI("", MessageAI.TYPE_BOT_PRODUCT, singleProductList));
                                        adapter.notifyItemInserted(messageAIs.size() - 1);
                                        recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                        saveMessageToDatabase("", MessageAI.TYPE_BOT_PRODUCT, singleProductList);
                                        productIndex++;
                                    } else {
                                        // Nếu không có sản phẩm tương ứng, hiển thị dòng văn bản với thông báo
                                        messageAIs.add(new MessageAI(line.trim() + " (Sản phẩm không còn tồn tại trong danh sách hiện tại.)", MessageAI.TYPE_BOT));
                                        adapter.notifyItemInserted(messageAIs.size() - 1);
                                        recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                        saveMessageToDatabase(line.trim() + " (Sản phẩm không còn tồn tại trong danh sách hiện tại.)", MessageAI.TYPE_BOT, null);
                                    }
                                }
                            }

                            // Nếu không có sản phẩm nào được đề xuất, thông báo cho người dùng
                            if (mentionedProducts.isEmpty() && botReply.contains("(ID: ")) {
                                messageAIs.add(new MessageAI("Một số sản phẩm được đề xuất không còn tồn tại trong danh sách hiện tại.", MessageAI.TYPE_BOT));
                                adapter.notifyItemInserted(messageAIs.size() - 1);
                                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                saveMessageToDatabase("Một số sản phẩm được đề xuất không còn tồn tại trong danh sách hiện tại.", MessageAI.TYPE_BOT, null);
                            }

                            if (isOrderRelated) {
                                String suggestionPrompt = "Bạn có muốn xem thêm các phụ kiện liên quan đến sản phẩm mà bạn đã đặt hàng hay không?";
                                messageAIs.add(new MessageAI(suggestionPrompt, MessageAI.TYPE_BOT));
                                adapter.notifyItemInserted(messageAIs.size() - 1);
                                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                                saveMessageToDatabase(suggestionPrompt, MessageAI.TYPE_BOT, null);
                                isWaitingForSuggestionResponse = true;
                            }
                        } catch (Exception e) {
                            handleError("Lỗi phân tích dữ liệu từ Gemini API: " + e.getMessage());
                        }
                    },
                    error -> {
                        String errorMsg = error.networkResponse != null ?
                                "HTTP " + error.networkResponse.statusCode + ": " + new String(error.networkResponse.data) :
                                error.getMessage() != null ? error.getMessage() : "Không thể kết nối đến Gemini API";
                        handleError("Lỗi gọi Gemini API: " + errorMsg);
                    });

            requestQueue.add(request);
        } catch (Exception e) {
            handleError("Lỗi tạo request: " + e.getMessage());
        }
    }

    private void handleError(String errorMessage) {
        Log.e("GEMINI_ERROR", errorMessage);
        messageAIs.add(new MessageAI("Lỗi: " + errorMessage, MessageAI.TYPE_BOT));
        adapter.notifyItemInserted(messageAIs.size() - 1);
        recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
        saveMessageToDatabase("Lỗi: " + errorMessage, MessageAI.TYPE_BOT);
    }

    private List<NewProduct> extractProductsFromReply(String reply) {
        List<NewProduct> mentionedProducts = new ArrayList<>();
        if (productList == null || productList.isEmpty()) {
            Log.w("MessageAIActivity", "productList rỗng hoặc null, không thể trích xuất sản phẩm");
            return mentionedProducts;
        }

        String normalizedReply = reply.toLowerCase().replaceAll("\\s+", " ").trim();
        Log.d("MessageAIActivity", "Phản hồi đã chuẩn hóa: " + normalizedReply);

        // Tìm sản phẩm theo ID
        for (NewProduct product : productList) {
            String idPattern = "\\(id:\\s*" + product.getId() + "\\)";
            if (normalizedReply.matches(".*" + idPattern + ".*")) {
                if (!mentionedProducts.contains(product)) { // Tránh trùng lặp
                    mentionedProducts.add(product);
                    Log.d("MessageAIActivity", "Tìm thấy sản phẩm theo ID: " + product.getProductName() + " (ID: " + product.getId() + ")");
                }
            }
        }

        // Sắp xếp sản phẩm theo thứ tự xuất hiện trong reply
        List<NewProduct> sortedProducts = new ArrayList<>();
        String[] replyLines = reply.split("\n");
        for (String line : replyLines) {
            for (NewProduct product : mentionedProducts) {
                if (line.contains("(ID: " + product.getId() + ")") && !sortedProducts.contains(product)) {
                    sortedProducts.add(product);
                }
            }
        }

        Log.d("MessageAIActivity", "Tổng số sản phẩm trích xuất: " + sortedProducts.size());
        return sortedProducts;
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Đang chờ xử lý";
            case 1: return "Đã xác nhận";
            case 2: return "Đang giao hàng";
            case 3: return "Đã giao hàng";
            case 4: return "Đã hủy";
            default: return "Không xác định";
        }
    }

    private void suggestRelatedProducts() {
        Log.d("MessageAIActivity", "suggestRelatedProducts called.");
        if (latestOrder == null || productList == null || productList.isEmpty()) {
            messageAIs.add(new MessageAI("Tôi không thể đề xuất sản phẩm vì không có thông tin đơn hàng hoặc danh sách sản phẩm.", MessageAI.TYPE_BOT));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase("Tôi không thể đề xuất sản phẩm vì không có thông tin đơn hàng hoặc danh sách sản phẩm.", MessageAI.TYPE_BOT, null);
            return;
        }

        Set<Integer> categories = new HashSet<>();
        if (latestOrder.getItem() != null) {
            for (ItemForAI item : latestOrder.getItem()) {
                categories.add(item.getCategory());
            }
        } else {
            messageAIs.add(new MessageAI("Đơn hàng mới nhất của bạn không có sản phẩm để tôi đề xuất phụ kiện liên quan.", MessageAI.TYPE_BOT));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase("Đơn hàng mới nhất của bạn không có sản phẩm để tôi đề xuất phụ kiện liên quan.", MessageAI.TYPE_BOT, null);
            return;
        }

        List<NewProduct> relatedProducts = new ArrayList<>();
        for (NewProduct product : productList) {
            if (categories.contains(product.getCategory()) || isRelatedCategory(product.getCategory(), categories)) {
                boolean alreadyPurchased = false;
                for (ItemForAI item : latestOrder.getItem()) {
                    if (item.getId_product() == product.getId()) {
                        alreadyPurchased = true;
                        break;
                    }
                }
                if (!alreadyPurchased) {
                    relatedProducts.add(product);
                }
            }
        }

        if (!relatedProducts.isEmpty()) {
            String suggestionText = "Dựa trên đơn hàng của bạn, tôi có một số gợi ý phụ kiện liên quan:";
            messageAIs.add(new MessageAI(suggestionText, MessageAI.TYPE_BOT));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase(suggestionText, MessageAI.TYPE_BOT, null);

            // Add each product as a separate TYPE_BOT_PRODUCT message
            for (NewProduct product : relatedProducts) {
                List<NewProduct> singleProductList = new ArrayList<>();
                singleProductList.add(product);
                messageAIs.add(new MessageAI("", MessageAI.TYPE_BOT_PRODUCT, singleProductList));
                adapter.notifyItemInserted(messageAIs.size() - 1);
                recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
                saveMessageToDatabase("", MessageAI.TYPE_BOT_PRODUCT, singleProductList);
            }
        } else {
            String noSuggestionText = "Hiện tại tôi không tìm thấy phụ kiện liên quan nào để gợi ý. Bạn có muốn tôi tư vấn thêm không?";
            messageAIs.add(new MessageAI(noSuggestionText, MessageAI.TYPE_BOT));
            adapter.notifyItemInserted(messageAIs.size() - 1);
            recyclerViewChat.scrollToPosition(messageAIs.size() - 1);
            saveMessageToDatabase(noSuggestionText, MessageAI.TYPE_BOT, null);
        }
    }

    // Helper method to get a short description (max 20 words)
    private String getShortDescription(String description) {
        if (description == null || description.isEmpty()) {
            return "Không có mô tả.";
        }
        String[] words = description.split("\\s+");
        StringBuilder shortDesc = new StringBuilder();
        int wordCount = 0;
        for (String word : words) {
            if (wordCount >= 20) break;
            shortDesc.append(word).append(" ");
            wordCount++;
        }
        return shortDesc.toString().trim() + (wordCount < words.length ? "..." : ".");
    }

    private boolean isRelatedCategory(int productCategory, Set<Integer> purchasedCategories) {
        Log.d("MessageAIActivity", "isRelatedCategory called. Product category: " + productCategory + ", Purchased categories: " + purchasedCategories);
        if (purchasedCategories.contains(1)) {
            return productCategory == 2 || productCategory == 3;
        }
        if (purchasedCategories.contains(2)) {
            return productCategory == 1;
        }
        if (purchasedCategories.contains(4)) {
            return productCategory == 5 || productCategory == 6;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("messages", messageAIs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(orderReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("MessageAIActivity", "Receiver not registered: " + e.getMessage());
        }
        compositeDisposable.clear();
    }
}