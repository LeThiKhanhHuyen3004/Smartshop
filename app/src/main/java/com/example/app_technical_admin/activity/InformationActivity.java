package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.app_technical_admin.R;

public class InformationActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView txtPrivacyPolicy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        // Ánh xạ Toolbar và TextView
        toolbar = findViewById(R.id.toolbar);
        txtPrivacyPolicy = findViewById(R.id.txtPrivacyPolicy);

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Information");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Xử lý sự kiện click vào Privacy Policy
        txtPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút "Back"
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white); // Icon tùy chỉnh (nếu cần)
        }

        // Xử lý sự kiện bấm vào nút "Back" trên Toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InformationActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Kết thúc ManagerActivity để quay về MainActivity
            }
        });
    }

    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.smartshop.com/privacy-policy"));
        startActivity(intent);
    }
}