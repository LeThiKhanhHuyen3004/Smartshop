package com.example.app_technical_admin.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.R;

public class AdvertisingActivity extends AppCompatActivity {
    TextView content;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_advertising);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initView();
        initData();
    }

    private void initData() {
        String ct = getIntent().getStringExtra("information");
        String url = getIntent().getStringExtra("url");
        content.setText(ct);
        Glide.with(this).load(url).into(imageView);
    }

    private void initView() {
        content = findViewById(R.id.advetising_content);
        imageView = findViewById(R.id.advetising_image);
    }


}