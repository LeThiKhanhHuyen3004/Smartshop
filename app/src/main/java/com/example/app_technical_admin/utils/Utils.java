package com.example.app_technical_admin.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.app_technical_admin.model.Cart;
import com.example.app_technical_admin.model.User;
import com.example.app_technical_admin.retrofit.ApiSale;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.paperdb.Paper;

public class Utils {
//    public static final String BASE_URL = "http://192.168.1.6/sale/";
public static final String BASE_URL = "http://172.16.75.136/sale/";
    public static List<Cart> arrayCart = new ArrayList<>();
    public static List<Cart> arrayBuyProduct = new ArrayList<>();
    public static User user_current = new User(); // Khởi tạo mặc định

    public static String ID_RECEIVED;
    public static final String SENDID = "idsend";
    public static final String RECEIVEDID = "idreceived";
    public static final String MESSAGE = "message";
    public static final String DATETIME = "datetime";
    public static final String PATH_CHAT = "chat";

    // Khởi tạo và khôi phục user_current từ PaperDB
    public static void initUser(Context context) {
        Paper.init(context); // Khởi tạo PaperDB
        User savedUser = Paper.book().read("user_current", null);
        if (savedUser != null) {
            user_current = savedUser;
            Log.d("Utils", "Khôi phục user_current từ PaperDB: ID=" + user_current.getId() + ", Email=" + user_current.getEmail());
        } else {
            user_current = new User(); // Nếu không có dữ liệu, khởi tạo mặc định
            Log.w("Utils", "Không tìm thấy user_current trong PaperDB, khởi tạo mặc định");
        }
    }

    // Lưu user_current vào PaperDB
    public static void saveUser(Context context) {
        Paper.init(context);
        if (user_current != null && user_current.getId() > 0) {
            Paper.book().write("user_current", user_current);
            Log.d("Utils", "Đã lưu user_current vào PaperDB: ID=" + user_current.getId());
        }
    }

    // Xóa user_current khi đăng xuất
    public static void clearUser(Context context) {
        Paper.init(context);
        Paper.book().delete("user_current");
        user_current = new User();
        Log.d("Utils", "Đã xóa user_current khỏi PaperDB");
    }

    public static String statusOrder(int status) {
        String result = "";
        switch (status) {
            case 0:
                result = "Order is pending";
                break;
            case 1:
                result = "Order accepted";
                break;
            case 2:
                result = "Order is delivering";
                break;
            case 3:
                result = "Order delivery successful";
                break;
            case 4:
                result = "Order is canceled";
                break;
            default:
                result = "...";
        }
        return result;
    }

    public static boolean isAdmin() {
        return user_current != null && user_current.getStatus() == 1;
    }

    public static boolean isUser() {
        return user_current != null && user_current.getStatus() == 0;
    }

    public static String hmacSHA256(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacData = mac.doFinal(data.getBytes("UTF-8"));
            return Base64.encodeToString(hmacData, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC: " + e.getMessage());
        }
    }
}