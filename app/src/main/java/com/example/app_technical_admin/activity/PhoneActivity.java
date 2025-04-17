package com.example.app_technical_admin.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.PhoneAdapter;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.NewProductModel;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class PhoneActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PhoneAdapter phoneAdapter;
    private List<NewProduct> productList;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ApiSale apiSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        // Khởi tạo API
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        // Ánh xạ RecyclerView
        recyclerView = findViewById(R.id.recycleview_phone);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setHasFixedSize(true);

        productList = new ArrayList<>();
        phoneAdapter = new PhoneAdapter(this, productList);
        recyclerView.setAdapter(phoneAdapter);

        // Lấy category từ Intent
        int category = getIntent().getIntExtra("category", -1);
        if (category != -1) {
            getProductsByCategory(category);
        } else {
            Toast.makeText(this, "Không tìm thấy danh mục", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void getProductsByCategory(int category) {
        compositeDisposable.add(apiSale.getNewProductByCategory(category) // Gọi API với tham số category
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        newProductModel -> {
                            if (newProductModel.isSuccess()) {
                                productList.clear();
                                productList.addAll(newProductModel.getResult());
                                phoneAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(this, "Không có sản phẩm trong danh mục này", Toast.LENGTH_LONG).show();
                            }
                        },
                        throwable -> {
                            Log.e("PhoneActivity", "Error fetching products: " + throwable.getMessage());
                            Toast.makeText(this, "Không thể kết nối đến server: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                ));
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}