package com.example.app_technical_admin.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager; // Thay đổi từ LinearLayoutManager sang GridLayoutManager
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.PromotionProductAdapter;
import com.example.app_technical_admin.model.NewProduct;

import java.util.ArrayList;
import java.util.List;

public class PromotionActivity extends AppCompatActivity {

    private RecyclerView recyclerViewAllPromotions;
    private Toolbar toolbar;
    private PromotionProductAdapter promotionAdapter;
    private List<NewProduct> promotionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion);

        // Khởi tạo view
        recyclerViewAllPromotions = findViewById(R.id.recyclerViewAllPromotions);
        toolbar = findViewById(R.id.toolbar);

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Nhận dữ liệu từ Intent
        promotionList = (ArrayList<NewProduct>) getIntent().getSerializableExtra("promotionList");
        String promotionTitle = getIntent().getStringExtra("promotionTitle");

        // Thiết lập tiêu đề Toolbar
        if (promotionTitle != null && !promotionTitle.isEmpty()) {
            getSupportActionBar().setTitle(promotionTitle);
        } else {
            getSupportActionBar().setTitle("Ongoing Promotions");
        }

        // Thiết lập RecyclerView với GridLayoutManager (2 cột)
        recyclerViewAllPromotions.setLayoutManager(new GridLayoutManager(this, 2)); // Số 2 là số cột
        promotionAdapter = new PromotionProductAdapter(this, promotionList);
        recyclerViewAllPromotions.setAdapter(promotionAdapter);
    }
}