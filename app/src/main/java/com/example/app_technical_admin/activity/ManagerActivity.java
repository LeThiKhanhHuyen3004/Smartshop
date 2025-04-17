package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager; // Thêm import này
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.NewProductAdapter;
import com.example.app_technical_admin.model.EventBus.EditDeleteEvent;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.NewProductModel;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ManagerActivity extends AppCompatActivity {
    ImageView img_add;
    RecyclerView recyclerView;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiSale apiSale;
    List<NewProduct> list;
    NewProductAdapter adapter;
    NewProduct productEditDelete;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager);

        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        initView();
        initControl();
        getNewProduct();
    }

    private void initControl() {
        img_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddProductActivity.class);
                startActivity(intent);
            }
        });
    }

    private void getNewProduct() {
        compositeDisposable.add(apiSale.getNewProduct()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        newProductModel -> {
                            if (newProductModel.isSuccess()) {
                                list = newProductModel.getResult();
                                adapter = new NewProductAdapter(getApplicationContext(), list);
                                recyclerView.setAdapter(adapter);
                            }
                        },
                        throwable -> {
                            Toast.makeText(getApplicationContext(), "Cannot connect to server" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                ));
    }

    private void initView() {
        img_add = findViewById(R.id.img_add);
        recyclerView = findViewById(R.id.recycleview_manager);
        // Sử dụng GridLayoutManager với 2 cột
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2); // 2 là số cột
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút "Back"
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white); // Icon tùy chỉnh (nếu cần)
        }

        // Xử lý sự kiện bấm vào nút "Back" trên Toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Kết thúc ManagerActivity để quay về MainActivity
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals("Edit")) {
            editProduct();
        } else if (item.getTitle().equals("Delete")) {
            deleteProduct();
        }
        return super.onContextItemSelected(item);
    }

    private void deleteProduct() {
        compositeDisposable.add(apiSale.deleteProduct(productEditDelete.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            if (messageModel.isSuccess()) {
                                Toast.makeText(getApplicationContext(), "Xóa thành công!", Toast.LENGTH_LONG).show();
                                getNewProduct();
                            } else {
                                Toast.makeText(getApplicationContext(), "Lỗi: " + messageModel.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        },
                        throwable -> {
                            Log.e("DELETE_ERROR", "Lỗi: " + throwable.getMessage());
                            Toast.makeText(getApplicationContext(), "Không thể xóa sản phẩm", Toast.LENGTH_LONG).show();
                        }
                ));
    }

    private void editProduct() {
        Intent intent = new Intent(getApplicationContext(), AddProductActivity.class);
        intent.putExtra("Edit", productEditDelete);
        startActivity(intent);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void evenEditDelete(EditDeleteEvent event) {
        if (event != null) {
            productEditDelete = event.getNewProduct();
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}