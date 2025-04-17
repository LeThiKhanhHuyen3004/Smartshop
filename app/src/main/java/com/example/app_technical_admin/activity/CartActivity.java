package com.example.app_technical_admin.activity;

import static org.greenrobot.eventbus.ThreadMode.MAIN;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.CartAdapter;
import com.example.app_technical_admin.model.Cart;
import com.example.app_technical_admin.model.EventBus.TotalEvent;
import com.example.app_technical_admin.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    TextView emptyCart, total;
    Toolbar toolbar;
    RecyclerView recyclerView;
    AppCompatButton btnBuyProduct;
    CartAdapter adapter;
    List<Cart> cartList;
    long totalProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initView();
        initControl();
        if (Utils.arrayBuyProduct != null) {
            Utils.arrayBuyProduct.clear();
        }
        total();
    }

    private void total() {
        totalProduct = 0;
        for (int i = 0; i < Utils.arrayBuyProduct.size(); i++) {
            Cart cart = Utils.arrayBuyProduct.get(i);
            long priceToUse = cart.getDiscountedPrice() > 0 ? cart.getDiscountedPrice() : cart.getPrice();
            totalProduct += (priceToUse * cart.getCount());
        }
        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
        total.setText(decimalFormat.format(totalProduct));
    }

    private void initControl() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        if (Utils.arrayCart.size() == 0) {
            emptyCart.setVisibility(View.VISIBLE);
        } else {
            adapter = new CartAdapter(getApplicationContext(), Utils.arrayCart);
            recyclerView.setAdapter(adapter);
        }

        btnBuyProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (totalProduct == 0) {
                    // Hiển thị thông báo nếu không có sản phẩm được chọn
                    Toast.makeText(getApplicationContext(), "No products yet, please select a product", Toast.LENGTH_SHORT).show();
                } else {
                    // Chuyển sang PaymentActivity nếu có sản phẩm
                    Intent intent = new Intent(getApplicationContext(), PaymentActivity.class);
                    intent.putExtra("total", totalProduct);
                    startActivity(intent);
                }
            }
        });
    }

    private void initView() {
        emptyCart = findViewById(R.id.txtEmptyCart);
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycleviewCart);
        total = findViewById(R.id.txtTotal);
        btnBuyProduct = findViewById(R.id.btnBuyProduct);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    public void eventTotal(TotalEvent event) {
        if (event != null) {
            total();
        }
    }
}