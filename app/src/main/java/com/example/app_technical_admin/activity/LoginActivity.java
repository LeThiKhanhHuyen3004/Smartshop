package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.paperdb.Paper;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    TextView txtRegister, txtResetPassword;
    EditText email, password;
    AppCompatButton btnLogin;
    ApiSale apiSale;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    private FirebaseAuth firebaseAuth;
    boolean isLogin = false;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Paper.init(this);
        Utils.initUser(this); // Khôi phục user_current từ PaperDB khi khởi động

        initView();

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        boolean isLoginSaved = Paper.book().read("isLogin", false);

        if (currentUser != null && isLoginSaved && currentUser.isEmailVerified()) {
            String savedEmail = Paper.book().read("email");
            String savedPassword = Paper.book().read("password");
            if (savedEmail != null && savedPassword != null) {
                fetchUserDataFromServer(savedEmail, savedPassword);
            } else {
                initControl();
            }
        } else {
            initControl();
        }
    }

//    private void fetchUserDataFromServer(String email, String password) {
//        compositeDisposable.add(apiSale.login(email, password)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        userModel -> {
//                            if (userModel.isSuccess() && !userModel.getResult().isEmpty()) {
//                                Utils.user_current = userModel.getResult().get(0);
//                                Paper.book().write("isLogin", true);
//                                Paper.book().write("email", email);
//                                Paper.book().write("password", password);
//                                Utils.saveUser(LoginActivity.this); // Lưu user_current vào PaperDB
//
//                                // Lưu userId vào SharedPreferences
//                                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
//                                SharedPreferences.Editor editor = preferences.edit();
//                                editor.putString("userId", String.valueOf(Utils.user_current.getId()));
//                                editor.apply();
//                                Log.d(TAG, "Đã lưu userId vào SharedPreferences: " + Utils.user_current.getId());
//
//                                Log.d(TAG, "Cập nhật Utils.user_current: " + Utils.user_current.getEmail() + ", ID: " + Utils.user_current.getId());
//                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
//                                finish();
//                            } else {
//                                // Nếu API login thất bại, có thể mật khẩu trong MySQL không khớp
//                                updatePasswordInMySQL(email, password);
//                            }
//                        },
//                        throwable -> {
//                            Log.e(TAG, "Lỗi lấy dữ liệu người dùng: " + throwable.getMessage());
//                            Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
//                        }
//                ));
//    }

    private void fetchUserDataFromServer(String email, String password) {
        compositeDisposable.add(apiSale.login(email, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userModel -> {
                            if (userModel.isSuccess() && !userModel.getResult().isEmpty()) {
                                Utils.user_current = userModel.getResult().get(0);
                                Paper.book().write("isLogin", true);
                                Paper.book().write("email", email);
                                Paper.book().write("password", password);
                                Utils.saveUser(LoginActivity.this); // Lưu user_current vào PaperDB

                                // Lưu userId vào SharedPreferences
                                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("userId", String.valueOf(Utils.user_current.getId()));
                                editor.apply();
                                Log.d(TAG, "Đã lưu userId vào SharedPreferences: " + Utils.user_current.getId());

                                Log.d(TAG, "Cập nhật Utils.user_current: " + Utils.user_current.getEmail() + ", ID: " + Utils.user_current.getId());
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();
                            } else {
                                // Nếu API login thất bại, có thể mật khẩu trong MySQL không khớp
                                // Đồng bộ mật khẩu từ Firebase vào MySQL và thử lại
                                updatePasswordInMySQL(email, password);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Lỗi lấy dữ liệu người dùng: " + throwable.getMessage());
                            Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void initControl() {
        txtRegister.setOnClickListener(view -> {
            Log.d(TAG, "Chuyển đến trang đăng ký");
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
        });

        txtResetPassword.setOnClickListener(view -> {
            Log.d(TAG, "Chuyển đến trang đặt lại mật khẩu");
            startActivity(new Intent(getApplicationContext(), ResetPasswordActivity.class));
        });

        btnLogin.setOnClickListener(view -> {
            String str_email = email.getText().toString().trim();
            String str_password = password.getText().toString().trim();

            Log.d(TAG, "Nhấn nút login với email: " + str_email + ", password: " + str_password);

            if (TextUtils.isEmpty(str_email)) {
                Log.e(TAG, "Email rỗng");
                Toast.makeText(view.getContext(), "Bạn chưa nhập tài khoản!", Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(str_password)) {
                Log.e(TAG, "Mật khẩu rỗng");
                Toast.makeText(view.getContext(), "Bạn chưa nhập mật khẩu!", Toast.LENGTH_SHORT).show();
                return;
            } else if (str_password.length() < 6) {
                Log.e(TAG, "Mật khẩu quá ngắn");
                Toast.makeText(view.getContext(), "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            Paper.book().write("email", str_email);
            Paper.book().write("password", str_password);

            // Đăng xuất trước khi đăng nhập để tránh xung đột
            firebaseAuth.signOut();
            Log.d(TAG, "Đã đăng xuất khỏi Firebase");

            Log.d(TAG, "Thực hiện đăng nhập Firebase với email: " + str_email);
            firebaseAuth.signInWithEmailAndPassword(str_email, str_password)
                    .addOnCompleteListener(LoginActivity.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Đăng nhập Firebase thành công");
                            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                            if (!currentUser.isEmailVerified()) {
                                currentUser.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(LoginActivity.this, "Vui lòng xác minh email trước khi đăng nhập!", Toast.LENGTH_LONG).show();
                                            }
                                        });
                            } else {
                                // Lấy thông tin đầy đủ từ API sau khi đăng nhập Firebase thành công
                                fetchUserDataFromServer(str_email, str_password);
                            }
                        } else {
                            Log.e(TAG, "Lỗi đăng nhập Firebase: " + task.getException().getMessage());
                            Toast.makeText(LoginActivity.this, "Lỗi đăng nhập: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void initView() {
        try {
            apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
            Log.d(TAG, "initView: Đã tạo ApiSale instance");

            txtRegister = findViewById(R.id.txtRegister);
            txtResetPassword = findViewById(R.id.txtResetPassword);
            email = findViewById(R.id.email);
            password = findViewById(R.id.password);
            btnLogin = findViewById(R.id.btnLogin);

            firebaseAuth = FirebaseAuth.getInstance();
            user = firebaseAuth.getCurrentUser();
            Log.d(TAG, "initView: FirebaseAuth và User đã được lấy, User: " + (user != null ? user.getEmail() : "null"));

            if (Paper.book().read("email") != null && Paper.book().read("password") != null) {
                email.setText(Paper.book().read("email"));
                password.setText(Paper.book().read("password"));
                Log.d(TAG, "initView: Điền tự động email: " + Paper.book().read("email"));
            }
            Log.d(TAG, "initView: Đọc dữ liệu đăng nhập từ PaperDB thành công");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong initView(): ", e);
        }
    }

    private void updatePasswordInMySQL(String email, String password) {
        Call<ResponseBody> call = apiSale.updatePassword(email, password);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseString = response.body().string();
                        Log.d(TAG, "Đồng bộ mật khẩu với MySQL thành công, response: " + responseString);
                        // Sau khi đồng bộ thành công, thử đăng nhập lại
                        fetchUserDataFromServer(email, password);
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi đọc response: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Lỗi đọc response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Lỗi đồng bộ mật khẩu với MySQL, code: " + response.code() + ", message: " + response.message());
                    Toast.makeText(LoginActivity.this, "Lỗi đồng bộ mật khẩu với MySQL: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối khi đồng bộ mật khẩu: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}