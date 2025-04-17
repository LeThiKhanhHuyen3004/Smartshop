package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.app_technical_admin.R;
import com.google.android.material.button.MaterialButton;

public class ContactActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText edtName, edtEmail, edtMessage;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        // Ánh xạ các view
        toolbar = findViewById(R.id.toolbar);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtMessage = findViewById(R.id.edtMessage);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Contact");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Xử lý sự kiện click nút Submit
        btnSubmit.setOnClickListener(v -> submitContactForm());

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút "Back"
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white); // Icon tùy chỉnh (nếu cần)
        }

        // Xử lý sự kiện bấm vào nút "Back" trên Toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContactActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Kết thúc ManagerActivity để quay về MainActivity
            }
        });
    }

    private void submitContactForm() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String message = edtMessage.getText().toString().trim();

        // Kiểm tra dữ liệu nhập vào
        if (TextUtils.isEmpty(name)) {
            edtName.setError("Please enter your name");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Please enter your email");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Please enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            edtMessage.setError("Please enter your message");
            return;
        }

        // Xử lý gửi dữ liệu (có thể gửi lên server hoặc gửi email)
        Toast.makeText(this, "Thank you for your message, " + name + "! We will get back to you soon.", Toast.LENGTH_LONG).show();

        // Xóa form sau khi gửi
        edtName.setText("");
        edtEmail.setText("");
        edtMessage.setText("");
    }
}