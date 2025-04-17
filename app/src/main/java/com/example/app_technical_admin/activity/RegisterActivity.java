package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {
    EditText email, password, repassword, phoneNumber, userName;
    AppCompatButton button;
    ApiSale apiSale;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.d("RegisterActivity", "onCreate() called");

        initView();
        initControl();
    }

    private void initView() {
        firebaseAuth = FirebaseAuth.getInstance();
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        repassword = findViewById(R.id.repassword);
        phoneNumber = findViewById(R.id.phoneNumber);
        userName = findViewById(R.id.userName);
        button = findViewById(R.id.btnRegister);
    }

    private void initControl() {
        button.setOnClickListener(view -> register());
    }

    private void register() {
        Log.d("RegisterActivity", "register() method called");

        String str_userName = userName.getText().toString().trim();
        String str_email = email.getText().toString().trim();
        String str_password = password.getText().toString().trim();
        String str_repassword = repassword.getText().toString().trim();
        String str_phoneNumber = phoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(str_userName)) {
            showToast("You haven't input User Name");
            return;
        }
        if (TextUtils.isEmpty(str_password)) {
            showToast("You haven't input Password");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(str_email).matches()) {
            showToast("Invalid email format");
            return;
        }
        if (str_password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return;
        }
        if (!str_password.equals(str_repassword)) {
            showToast("Passwords do not match");
            return;
        }
        if (!Patterns.PHONE.matcher(str_phoneNumber).matches()) {
            showToast("Invalid phone number");
            return;
        }

        Log.d("RegisterActivity", "All input validation passed");

        firebaseAuth.createUserWithEmailAndPassword(str_email, str_password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("RegisterActivity", "Firebase user created successfully");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            postData(str_email, str_password, str_userName, str_phoneNumber, user.getUid());
                        }
                    } else {
                        Log.e("RegisterActivity", "FirebaseAuth error: " + task.getException().getMessage());
                        showToast("Email already exists or registration failed");
                    }
                });
    }


    private void postData(String str_email, String str_password, String str_userName, String str_phoneNumber, String uid) {
        Log.d("RegisterActivity", "Posting data to server: " + str_email);
        compositeDisposable.add(apiSale.register(str_email, str_password, str_userName, str_phoneNumber, uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userModel -> {
                            if (userModel.isSuccess()) {
                                Log.d("RegisterActivity", "User registration successful on backend");
                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e("RegisterActivity", "Backend error: " + userModel.getMessage());
                                showToast("Backend error: " + userModel.getMessage());
                            }
                        },
                        throwable -> {
                            Log.e("API_ERROR", "Error occurred", throwable);
                            Log.e("RegisterActivity", "Backend API error: " + throwable.getMessage());
                            showToast("Backend API error: " + throwable.getMessage());
                        }
                )
        );
    }

    private String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(10));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}
