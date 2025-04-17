package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.Cart;
import com.example.app_technical_admin.model.CreateOrder;
import com.example.app_technical_admin.model.Address.District;
import com.example.app_technical_admin.model.MessageModel;
import com.example.app_technical_admin.model.Address.Province;
import com.example.app_technical_admin.model.Address.Ward;
import com.example.app_technical_admin.model.NotiSendData;
import com.example.app_technical_admin.retrofit.ApiPushNotification;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.retrofit.RetrofitClientNoti;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.paperdb.Paper;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vn.zalopay.sdk.Environment;
import vn.zalopay.sdk.ZaloPayError;
import vn.zalopay.sdk.ZaloPaySDK;
import vn.zalopay.sdk.listeners.PayOrderListener;

public class PaymentActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView txtTotal, txtPhoneNumber, txtEmail, tvSelectedAddress;
    ImageView ivSelectAddress;
    AppCompatButton btnOrder;
    RadioButton rbSelectedPaymentMethod;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiSale apiSale;
    long total;
    int totalItem;
    int id_order;

    private List<Province> provinceList;
    private Province selectedProvince;
    private District selectedDistrict;
    private Ward selectedWard;
    private String fullAddress;
    private String streetName;
    private String houseNumber;

    private String selectedPaymentMethod = "Cash"; // Default to Cash
    private static final String MOMO_PARTNER_CODE = "MOMOIQA420180417";
    private static final String MOMO_ACCESS_KEY = "Q8gbQHeDesB2Xs0t";
    private static final String MOMO_SECRET_KEY = "PPuDXq1KowPT1ftR8DvlQTHhC03aul17";
    private static final String MOMO_REDIRECT_URL = "com.example.app_technical_admin://callback";
    private static final String MOMO_IPN_URL = "https://test-payment.momo.vn/demo/#/payment-result";
    private static final String MOMO_REQUEST_TYPE = "captureWallet";
    private static final String MOMO_LANG = "vi";
    private static final String MOMO_ORDER_INFO_TEMPLATE = "Thanh toán sản phẩm công nghệ - Đơn hàng #%s";
    private static final int MOMO_REQUEST_CODE = 1002;
    private String currentOrderId;
    private String currentRequestId;
    private final OkHttpClient momoClient = new OkHttpClient();

    private static final int PAYMENT_METHOD_REQUEST_CODE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ZaloPaySDK.init(2554, Environment.SANDBOX);

        Utils.initUser(this);
        Log.d("PaymentActivity", "ID người dùng sau khi khôi phục từ PaperDB: " + Utils.user_current.getId());

        if (Utils.user_current.getId() <= 0) {
            Log.e("PaymentActivity", "ID người dùng không hợp lệ, chuyển về LoginActivity");
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initView();
        countItem();
        loadAddressData();
        fetchUserAddressFromServer();
        initControl();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        txtTotal = findViewById(R.id.txtTotal);
        txtPhoneNumber = findViewById(R.id.txtPhoneNumber);
        txtEmail = findViewById(R.id.txtEmail);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        ivSelectAddress = findViewById(R.id.ivSelectAddress);
        btnOrder = findViewById(R.id.btnOrder);
        rbSelectedPaymentMethod = findViewById(R.id.rbSelectedPaymentMethod);

        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
        total = getIntent().getLongExtra("total", 0);
        txtTotal.setText(decimalFormat.format(total));
        txtEmail.setText(Utils.user_current.getEmail() != null ? Utils.user_current.getEmail() : "");
        txtPhoneNumber.setText(Utils.user_current.getPhoneNumber() != null ? Utils.user_current.getPhoneNumber() : "");
    }

    private void initControl() {
        toolbar.setNavigationOnClickListener(view -> finish());

        ivSelectAddress.setOnClickListener(v -> showAddressDialog());

        rbSelectedPaymentMethod.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, PaymentMethodActivity.class);
            intent.putExtra("selectedPaymentMethod", selectedPaymentMethod);
            startActivityForResult(intent, PAYMENT_METHOD_REQUEST_CODE);
        });

        btnOrder.setOnClickListener(view -> {
            if (TextUtils.isEmpty(fullAddress)) {
                Toast.makeText(getApplicationContext(), "Bạn chưa chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            } else {
                processOrderBasedOnPaymentMethod();
            }
        });
    }

    private void processOrderBasedOnPaymentMethod() {
        String str_email = Utils.user_current.getEmail();
        String str_phoneNumber = Utils.user_current.getPhoneNumber();
        int id = Utils.user_current.getId();
        String orderDetailJson = createOrderDetailJson();

        compositeDisposable.add(apiSale.createOrder(str_email, str_phoneNumber, String.valueOf(total), id, fullAddress, totalItem, orderDetailJson)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            if (messageModel != null && messageModel.isSuccess()) {
                                String idOrder = messageModel.getId_order();
                                id_order = Integer.parseInt(idOrder);
                                pushNotiToUser();
                                saveOrderToFirestore(idOrder, str_email, str_phoneNumber, String.valueOf(total), id, fullAddress, totalItem, orderDetailJson);
                                Toast.makeText(getApplicationContext(), "Order created successfully", Toast.LENGTH_SHORT).show();
                                updateCartAndFinish(idOrder);

                                // Handle payment-specific logic
                                switch (selectedPaymentMethod) {
                                    case "Cash":
                                        Intent intent = new Intent(getApplicationContext(), PaymentSuccessActivity.class);
                                        startActivity(intent);
                                        finish();
                                        break;
                                    case "MoMo":
                                        requestPayment(idOrder);
                                        break;
                                    case "ZaloPay":
                                        requestZalo();
                                        break;
                                }
                            } else {
                                String errorMsg = messageModel != null ? messageModel.getMessage() : "messageModel is null";
                                Toast.makeText(getApplicationContext(), "Failed to create order: " + errorMsg, Toast.LENGTH_LONG).show();
                            }
                        },
                        throwable -> Toast.makeText(getApplicationContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }
    private void fetchUserAddressFromServer() {
        if (Utils.user_current.getId() <= 0) {
            Log.e("PaymentActivity", "ID người dùng không hợp lệ, không thể gọi API getUserInfo");
            return;
        }

        String userId = String.valueOf(Utils.user_current.getId());
        Log.d("PaymentActivity", "Gọi API getUserInfo với userId: " + userId);

        compositeDisposable.add(apiSale.getUserInfo(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userModel -> {
                            if (userModel.isSuccess() && !userModel.getResult().isEmpty()) {
                                Utils.user_current = userModel.getResult().get(0);
                                Utils.saveUser(this);
                                String userAddress = Utils.user_current.getAddress();
                                if (!TextUtils.isEmpty(userAddress)) {
                                    fullAddress = userAddress;
                                    tvSelectedAddress.setText(fullAddress);
                                    tvSelectedAddress.setTextColor(getResources().getColor(android.R.color.black));
                                } else {
                                    tvSelectedAddress.setText("Chọn địa chỉ giao hàng");
                                    tvSelectedAddress.setTextColor(getResources().getColor(android.R.color.darker_gray));
                                }
                            } else {
                                String errorMsg = userModel.getMessage() != null ? userModel.getMessage() : "Không tìm thấy thông tin người dùng";
                                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> Toast.makeText(this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void countItem() {
        totalItem = 0;
        for (int i = 0; i < Utils.arrayBuyProduct.size(); i++) {
            totalItem += Utils.arrayBuyProduct.get(i).getCount();
        }
    }

    private void loadAddressData() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.vietnam_address);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            inputStream.close();

            Gson gson = new Gson();
            provinceList = gson.fromJson(jsonString.toString(), new TypeToken<List<Province>>() {}.getType());

            if (provinceList == null || provinceList.isEmpty()) {
                Toast.makeText(this, "Dữ liệu địa chỉ trống", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tải dữ liệu địa chỉ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddressDialog() {
        if (provinceList == null || provinceList.isEmpty()) {
            Toast.makeText(this, "Dữ liệu địa chỉ không khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_address, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_background);
            dialog.getWindow().setDimAmount(0.5f);
        }

        Spinner spinnerProvince = dialogView.findViewById(R.id.spinnerProvince);
        Spinner spinnerDistrict = dialogView.findViewById(R.id.spinnerDistrict);
        Spinner spinnerWard = dialogView.findViewById(R.id.spinnerWard);
        EditText edtStreetName = dialogView.findViewById(R.id.edtStreetName);
        EditText edtHouseNumber = dialogView.findViewById(R.id.edtHouseNumber);
        Button btnConfirmAddress = dialogView.findViewById(R.id.btnConfirmAddress);

        ArrayAdapter<Province> provinceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, provinceList);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        String userAddress = Utils.user_current.getAddress();
        if (!TextUtils.isEmpty(userAddress)) {
            try {
                String[] addressParts = userAddress.split(", ");
                if (addressParts.length == 4) {
                    String provinceName = addressParts[3].trim();
                    String districtName = addressParts[2].trim();
                    String wardName = addressParts[1].trim();
                    String streetAndHouse = addressParts[0].trim();

                    for (int i = 0; i < provinceList.size(); i++) {
                        Province province = provinceList.get(i);
                        if (province.getName().equals(provinceName)) {
                            spinnerProvince.setSelection(i);
                            selectedProvince = province;

                            List<District> districts = province.getDistricts();
                            ArrayAdapter<District> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
                            districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerDistrict.setAdapter(districtAdapter);
                            spinnerDistrict.setEnabled(true);

                            for (int j = 0; j < districts.size(); j++) {
                                District district = districts.get(j);
                                if (district.getName().equals(districtName)) {
                                    spinnerDistrict.setSelection(j);
                                    selectedDistrict = district;

                                    List<Ward> wards = district.getWards();
                                    ArrayAdapter<Ward> wardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wards);
                                    wardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    spinnerWard.setAdapter(wardAdapter);
                                    spinnerWard.setEnabled(true);

                                    for (int k = 0; k < wards.size(); k++) {
                                        Ward ward = wards.get(k);
                                        if (ward.getName().equals(wardName)) {
                                            spinnerWard.setSelection(k);
                                            selectedWard = ward;
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    String[] streetParts = streetAndHouse.split(" ", 2);
                    if (streetParts.length == 2) {
                        houseNumber = streetParts[0].trim();
                        streetName = streetParts[1].trim();
                        edtHouseNumber.setText(houseNumber);
                        edtStreetName.setText(streetName);
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Không thể điền sẵn địa chỉ, vui lòng chọn lại", Toast.LENGTH_SHORT).show();
            }
        }

        spinnerProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedProvince = provinceList.get(position);
                List<District> districts = selectedProvince.getDistricts();
                ArrayAdapter<District> districtAdapter = new ArrayAdapter<>(PaymentActivity.this, android.R.layout.simple_spinner_item, districts);
                districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrict.setAdapter(districtAdapter);
                spinnerDistrict.setEnabled(true);
                spinnerWard.setEnabled(false);
                spinnerWard.setAdapter(null);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerDistrict.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedDistrict = selectedProvince.getDistricts().get(position);
                List<Ward> wards = selectedDistrict.getWards();
                ArrayAdapter<Ward> wardAdapter = new ArrayAdapter<>(PaymentActivity.this, android.R.layout.simple_spinner_item, wards);
                wardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerWard.setAdapter(wardAdapter);
                spinnerWard.setEnabled(true);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerWard.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedWard = selectedDistrict.getWards().get(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnConfirmAddress.setOnClickListener(v -> {
            if (selectedProvince == null || selectedDistrict == null || selectedWard == null) {
                Toast.makeText(this, "Vui lòng chọn đầy đủ tỉnh/thành, quận/huyện và xã/phường", Toast.LENGTH_SHORT).show();
                return;
            }

            streetName = edtStreetName.getText().toString().trim();
            houseNumber = edtHouseNumber.getText().toString().trim();

            if (TextUtils.isEmpty(houseNumber) || TextUtils.isEmpty(streetName)) {
                Toast.makeText(this, "Vui lòng nhập số nhà và tên đường", Toast.LENGTH_SHORT).show();
                return;
            }

            fullAddress = String.format("%s %s, %s, %s, %s",
                    houseNumber, streetName, selectedWard.getName(), selectedDistrict.getName(), selectedProvince.getName());

            String oldAddress = Utils.user_current.getAddress();
            if (oldAddress != null && !oldAddress.equals(fullAddress)) {
                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
                confirmBuilder.setTitle("Lưu địa chỉ mới");
                confirmBuilder.setMessage("Bạn có muốn lưu địa chỉ này làm địa chỉ mặc định không?");
                confirmBuilder.setPositiveButton("Có", (dialogInterface, which) -> {
                    updateUserAddress(fullAddress);
                    tvSelectedAddress.setText(fullAddress);
                    tvSelectedAddress.setTextColor(getResources().getColor(android.R.color.black));
                });
                confirmBuilder.setNegativeButton("Không", (dialogInterface, which) -> {
                    tvSelectedAddress.setText(fullAddress);
                    tvSelectedAddress.setTextColor(getResources().getColor(android.R.color.black));
                });
                confirmBuilder.setCancelable(false);
                confirmBuilder.show();
            } else {
                tvSelectedAddress.setText(fullAddress);
                tvSelectedAddress.setTextColor(getResources().getColor(android.R.color.black));
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateUserAddress(String newAddress) {
        if (Utils.user_current == null || Utils.user_current.getId() <= 0) {
            Toast.makeText(this, "Không thể cập nhật địa chỉ: ID người dùng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = Utils.user_current.getId();
        compositeDisposable.add(apiSale.updateAddress(userId, newAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            if (messageModel.isSuccess()) {
                                Utils.user_current.setAddress(newAddress);
                                Utils.saveUser(this);
                                Toast.makeText(this, "Địa chỉ đã được cập nhật", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, messageModel.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> Toast.makeText(this, "Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private String createOrderDetailJson() {
        JsonArray orderDetailJson = new JsonArray();
        for (int i = 0; i < Utils.arrayBuyProduct.size(); i++) {
            JsonObject item = new JsonObject();
            Cart cart = Utils.arrayBuyProduct.get(i);
            long priceToUse = cart.getDiscountedPrice() > 0 ? cart.getDiscountedPrice() : cart.getPrice();
            item.addProperty("id_product", cart.getId());
            item.addProperty("count", cart.getCount());
            item.addProperty("price", priceToUse);
            orderDetailJson.add(item);
        }
        return orderDetailJson.toString();
    }

    private void updateCartAndFinish(String idOrder) {
        if (idOrder == null || idOrder.isEmpty()) {
            Toast.makeText(this, "Không thể cập nhật giỏ hàng: ID đơn hàng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            id_order = Integer.parseInt(idOrder);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Không thể cập nhật giỏ hàng: ID đơn hàng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < Utils.arrayBuyProduct.size(); i++) {
            Cart cart = Utils.arrayBuyProduct.get(i);
            Utils.arrayCart.remove(cart);
        }
        Utils.arrayBuyProduct.clear();
        Paper.book().write("cart", Utils.arrayCart);
    }

    private void saveOrderToFirestore(String idOrder, String email, String phoneNumber, String total, int idUser, String address, int totalItem, String orderDetailJson) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("id", Integer.parseInt(idOrder));
        orderData.put("id_user", idUser);
        orderData.put("address", address);
        orderData.put("phoneNumber", phoneNumber);
        orderData.put("email", email);
        orderData.put("total", total);
        orderData.put("status", 0);
        orderData.put("userName", Utils.user_current.getUserName());
        orderData.put("orderDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        orderData.put("momo", "");
        orderData.put("item", new Gson().fromJson(orderDetailJson, new TypeToken<List<Map<String, Object>>>() {}.getType()));

        db.collection("orders")
                .document(idOrder)
                .set(orderData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Đơn hàng " + idOrder + " đã được thêm vào Firestore"))
                .addOnFailureListener(e -> Log.e("Firestore", "Lỗi khi thêm đơn hàng " + idOrder + ": " + e.getMessage()));
    }

    private void pushNotiToUser() {
        compositeDisposable.add(apiSale.getToken(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userModel -> {
                            if (userModel.isSuccess()) {
                                for (int i = 0; i < userModel.getResult().size(); i++) {
                                    Map<String, String> data = new HashMap<>();
                                    data.put("title", "notification");
                                    data.put("body", "Ban co don hang moi");
                                    NotiSendData notiSendData = new NotiSendData(userModel.getResult().get(i).getToken(), data);
                                    ApiPushNotification apiPushNotification = RetrofitClientNoti.getInstance().create(ApiPushNotification.class);
                                    compositeDisposable.add(apiPushNotification.sendNotification(notiSendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                    notiResponse -> Log.d("PUSH_NOTI", "Notification sent successfully"),
                                                    throwable -> Log.e("PUSH_NOTI", "Error sending notification: " + throwable.getMessage())
                                            ));
                                }
                            }
                        },
                        throwable -> Log.d("loggg", throwable.getMessage())
                ));
    }

    private void requestPayment(String id_order) {
        if (total < 1000) {
            Toast.makeText(this, "Số tiền tối thiểu là 1.000 VND!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            currentRequestId = MOMO_PARTNER_CODE + "merchant_billId_" + System.currentTimeMillis();
            currentOrderId = id_order;
            String orderInfo = String.format(MOMO_ORDER_INFO_TEMPLATE, currentOrderId);

            JSONObject extraDataJson = new JSONObject();
            extraDataJson.put("app", "Smartshop Tech");
            String extraData = Base64.encodeToString(extraDataJson.toString().getBytes(), Base64.DEFAULT);

            String rawData = "accessKey=" + MOMO_ACCESS_KEY +
                    "&amount=" + total +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + MOMO_IPN_URL +
                    "&orderId=" + currentOrderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + MOMO_PARTNER_CODE +
                    "&redirectUrl=" + MOMO_REDIRECT_URL +
                    "&requestId=" + currentRequestId +
                    "&requestType=" + MOMO_REQUEST_TYPE;

            String signature = generateHmacSHA256(rawData, MOMO_SECRET_KEY);

            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", MOMO_PARTNER_CODE);
            requestBody.put("partnerName", "Smartshop");
            requestBody.put("storeId", MOMO_PARTNER_CODE);
            requestBody.put("requestId", currentRequestId);
            requestBody.put("amount", total);
            requestBody.put("orderId", currentOrderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", MOMO_REDIRECT_URL);
            requestBody.put("ipnUrl", MOMO_IPN_URL);
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", MOMO_REQUEST_TYPE);
            requestBody.put("signature", signature);
            requestBody.put("lang", MOMO_LANG);

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toString());
            Request request = new Request.Builder()
                    .url("https://test-payment.momo.vn/v2/gateway/api/create")
                    .post(body)
                    .build();

            Log.d("MOMO_REQUEST", "Request Data: " + requestBody.toString());
            momoClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(PaymentActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String resultCode = jsonResponse.getString("resultCode");

                            Log.d("MOMO_RESPONSE", "Response: " + responseBody);

                            if ("0".equals(resultCode)) {
                                String payUrl = jsonResponse.getString("payUrl");
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                                startActivityForResult(intent, MOMO_REQUEST_CODE);
                            } else {
                                String message = jsonResponse.optString("message", "Lỗi không xác định");
                                runOnUiThread(() -> Toast.makeText(PaymentActivity.this, "Lỗi MoMo: " + message, Toast.LENGTH_LONG).show());
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(PaymentActivity.this, "Lỗi phân tích dữ liệu MoMo: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(PaymentActivity.this, "Lỗi server: " + response.message(), Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void requestZalo() {
        CreateOrder orderApi = new CreateOrder();
        try {
            String amount = String.valueOf(total);
            Log.d("ZALOPAY_RESULT", "Creating ZaloPay order with amount: " + amount);
            JSONObject data = orderApi.createOrder(amount);
            String code = data.getString("return_code");
            Log.d("ZALOPAY_RESULT", "ZaloPay createOrder response code: " + code);

            if (code.equals("1")) {
                String token = data.getString("zp_trans_token");
                Log.d("ZALOPAY_RESULT", "ZaloPay token: " + token);
                ZaloPaySDK.getInstance().payOrder(this, token, "demozpdk:app", new PayOrderListener() {
                    @Override
                    public void onPaymentSucceeded(String s, String s1, String s2) {
                        Log.d("ZALOPAY_RESULT", "ZaloPay payment succeeded: s=" + s + ", s1=" + s1 + ", s2=" + s2);
                        compositeDisposable.add(apiSale.updateMomo(id_order, s1)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        messageModel -> {
                                            Log.d("ZALOPAY_RESULT", "Update ZaloPay status: " + (messageModel.isSuccess() ? "Success" : "Failed"));
                                            if (messageModel.isSuccess()) {
                                                // Redirect to PaymentSuccessActivity after successful ZaloPay payment
                                                Intent intent = new Intent(getApplicationContext(), PaymentSuccessActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Log.e("ZALOPAY_RESULT", "Update failed: " + messageModel.getMessage());
                                                Toast.makeText(PaymentActivity.this, "Cập nhật trạng thái đơn hàng thất bại: " + messageModel.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        },
                                        throwable -> {
                                            Log.e("ZALOPAY_RESULT", "Error updating ZaloPay status: " + throwable.getMessage());
                                            Toast.makeText(PaymentActivity.this, "Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                ));
                    }

                    @Override
                    public void onPaymentCanceled(String s, String s1) {
                        Log.d("ZALOPAY_RESULT", "ZaloPay payment canceled: s=" + s + ", s1=" + s1);
                        Toast.makeText(PaymentActivity.this, "Thanh toán bị hủy", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPaymentError(ZaloPayError zaloPayError, String s, String s1) {
                        Log.d("ZALOPAY_RESULT", "ZaloPay payment error: error=" + zaloPayError.name() + ", s=" + s + ", s1=" + s1);
                        Toast.makeText(PaymentActivity.this, "Lỗi thanh toán: " + zaloPayError.name(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.d("ZALOPAY_RESULT", "ZaloPay createOrder failed: Invalid return code");
                Toast.makeText(this, "Lỗi ZaloPay: Mã trả về không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ZALOPAY_RESULT", "ZaloPay error: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }   private String generateHmacSHA256(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            Log.e("MOMO_ERROR", "Lỗi khi tạo chữ ký: " + e.getMessage());
            return "";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYMENT_METHOD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedPaymentMethod = data.getStringExtra("selectedPaymentMethod");
            rbSelectedPaymentMethod.setText(selectedPaymentMethod);
        }

        if (requestCode == MOMO_REQUEST_CODE) {
            Log.d("MOMO_RESULT", "onActivityResult triggered for MoMo, resultCode: " + resultCode);
            if (data != null) {
                Uri uri = data.getData();
                Log.d("MOMO_RESULT", "MoMo URI: " + (uri != null ? uri.toString() : "null"));
                if (uri != null) {
                    String momoResultCode = uri.getQueryParameter("resultCode");
                    String transId = uri.getQueryParameter("transId");
                    Log.d("MOMO_RESULT", "ResultCode: " + momoResultCode + ", TransId: " + transId);

                    if ("0".equals(momoResultCode)) {
                        compositeDisposable.add(apiSale.updateMomo(id_order, transId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        messageModel -> {
                                            Log.d("MOMO_RESULT", "Update MoMo status: " + (messageModel.isSuccess() ? "Success" : "Failed"));
                                            if (messageModel.isSuccess()) {
                                                // Redirect to PaymentSuccessActivity after successful MoMo payment
                                                Intent intent = new Intent(getApplicationContext(), PaymentSuccessActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Cập nhật trạng thái đơn hàng thất bại", Toast.LENGTH_SHORT).show();
                                            }
                                        },
                                        throwable -> {
                                            Log.e("MOMO_RESULT", "Error updating MoMo status: " + throwable.getMessage());
                                            Toast.makeText(this, "Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                ));
                    } else {
                        String message = uri.getQueryParameter("message");
                        Log.d("MOMO_RESULT", "MoMo payment failed: " + message);
                        Toast.makeText(this, "Giao dịch thất bại: " + (message != null ? message : "Không xác định"), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d("MOMO_RESULT", "MoMo URI is null, no response from MoMo");
                    Toast.makeText(this, "Không nhận được phản hồi từ MoMo", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.d("MOMO_RESULT", "MoMo Intent data is null");
                Toast.makeText(this, "Không nhận được dữ liệu từ MoMo", Toast.LENGTH_LONG).show();
            }
        }
    }    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("ZALOPAY_RESULT", "onNewIntent triggered with intent: " + (intent != null ? intent.toString() : "null"));
        if (intent != null) {
            Log.d("ZALOPAY_RESULT", "Intent data: " + (intent.getData() != null ? intent.getData().toString() : "null"));
        }
        ZaloPaySDK.getInstance().onResult(intent);
    }
}