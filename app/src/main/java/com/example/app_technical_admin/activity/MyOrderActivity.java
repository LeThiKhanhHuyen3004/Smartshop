package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.OrderPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MyOrderActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_order);

        // Ánh xạ view
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        toolbar = findViewById(R.id.toolbar);

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Tắt tiêu đề mặc định
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị biểu tượng điều hướng
        }


        // Cài đặt adapter cho ViewPager2
        OrderPagerAdapter adapter = new OrderPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Liên kết TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("All orders");
                            break;
                        case 1:
                            tab.setText("Pending");
                            break;
                        case 2:
                            tab.setText("Accepted");
                            break;
                        case 3:
                            tab.setText("Delivering");
                            break;
                        case 4:
                            tab.setText("Delivered successfully");
                            break;
                        case 5:
                            tab.setText("Canceled");
                            break;
                    }
                }).attach();

        // Nhận vị trí tab từ Intent và đặt tab hiện tại
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("tab_position")) {
            int tabPosition = intent.getIntExtra("tab_position", 0); // Mặc định là 0 nếu không có giá trị
            viewPager.setCurrentItem(tabPosition); // Đặt tab tương ứng
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Xử lý khi nhấn vào biểu tượng điều hướng trên Toolbar
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_tab", R.id.nav_account); // Truyền ID của tab AccountFragment
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Kết thúc MyOrderActivity
        return true;
    }
}