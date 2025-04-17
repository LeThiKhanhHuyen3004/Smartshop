package com.example.app_technical_admin.activity;

import static org.greenrobot.eventbus.EventBus.TAG;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.ImageUrlAdapter;
import com.example.app_technical_admin.databinding.ActivityAddProductBinding;
import com.example.app_technical_admin.model.MessageModel;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {
    Spinner spinner_category;
    int category = 0;
    ActivityAddProductBinding binding;
    ApiSale apiSale;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    List<String> imageUrls = new ArrayList<>(); // Danh sách URL hình ảnh
    ImageUrlAdapter imageUrlAdapter;
    NewProduct productEdit;
    boolean flag = false;

    // ActivityResultLauncher để chọn nhiều hình ảnh
    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        // Chọn nhiều hình ảnh
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            uploadImage(imageUri);
                        }
                    } else if (data.getData() != null) {
                        // Chọn một hình ảnh
                        Uri imageUri = data.getData();
                        uploadImage(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        initView();
        initData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        productEdit = (NewProduct) intent.getSerializableExtra("Edit");
        if (productEdit == null) {
            // Thêm sản phẩm mới
            flag = false;
        } else {
            // Sửa sản phẩm
            flag = true;
            binding.btnAdd.setText("Edit Product");
            binding.des.setText(productEdit.getDescription());
            binding.price.setText(productEdit.getPrice());
            binding.name.setText(productEdit.getProductName());
            binding.countProduct.setText(String.valueOf(productEdit.getCountStock()));
            binding.spinnerCategory.setSelection(productEdit.getCategory());
            binding.linkVideo.setText(productEdit.getLinkVideo()); // Hiển thị linkVideo khi chỉnh sửa
            // Hiển thị danh sách hình ảnh trong RecyclerView
            if (productEdit.getImage() != null && !productEdit.getImage().isEmpty()) {
                imageUrls.addAll(productEdit.getImage());
                imageUrlAdapter.notifyDataSetChanged();
            }
        }
    }

    private void initView() {
        spinner_category = findViewById(R.id.spinner_category);
        // Khởi tạo RecyclerView và Adapter
        binding.imageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageUrlAdapter = new ImageUrlAdapter(this, imageUrls, position -> {
            // Xử lý xóa URL hình ảnh
            imageUrls.remove(position);
            imageUrlAdapter.notifyItemRemoved(position);
        });
        binding.imageRecyclerView.setAdapter(imageUrlAdapter);
    }

    private void initData() {
        // Cập nhật danh sách category lên 6 loại
        List<String> stringList = new ArrayList<>();
        stringList.add("Please choose category"); // Vị trí 0
        stringList.add("Phone");                  // Vị trí 1
        stringList.add("Laptop");                 // Vị trí 2
        stringList.add("Accessory");              // Vị trí 3
        stringList.add("Smart Watch");            // Vị trí 4
        stringList.add("Tablet");                 // Vị trí 5
        stringList.add("PC, Printer");            // Vị trí 6

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stringList);
        spinner_category.setAdapter(adapter);

        spinner_category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                category = i; // category sẽ là 0 nếu chọn "Please choose category", 1-6 cho các loại còn lại
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Không làm gì khi không chọn
            }
        });

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag == false) {
                    addProduct();
                } else {
                    editProduct();
                }
            }
        });

        binding.imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Mở intent để chọn nhiều hình ảnh
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                pickImagesLauncher.launch(intent);
            }
        });
    }

    private void editProduct() {
        String str_productName = binding.name.getText().toString().trim();
        String str_price = binding.price.getText().toString().trim();
        String str_description = binding.des.getText().toString().trim();
        String str_countStock = binding.countProduct.getText().toString().trim();
        String str_linkVideo = binding.linkVideo.getText().toString().trim(); // Lấy giá trị linkVideo

        if (TextUtils.isEmpty(str_countStock) || TextUtils.isEmpty(str_productName) || TextUtils.isEmpty(str_price) || TextUtils.isEmpty(str_description) || category == 0 || imageUrls.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please fill all required fields and upload at least one image", Toast.LENGTH_LONG).show();
        } else {
            // Chuyển danh sách imageUrls thành chuỗi JSON
            String str_image = new Gson().toJson(imageUrls);
            // Nếu linkVideo rỗng, gửi null
            String linkVideoToSend = str_linkVideo.isEmpty() ? null : str_linkVideo;

            Log.d("DEBUG", "Editing product: " + str_productName + ", Price: " + str_price + ", Image: " + str_image + ", Desc: " + str_description + ", Category: " + category + ", LinkVideo: " + linkVideoToSend + ", ID: " + productEdit.getId());
            compositeDisposable.add(apiSale.updateProduct(
                            str_productName,
                            str_price,
                            str_image,
                            str_description,
                            category,
                            Integer.parseInt(str_countStock),
                            productEdit.getId(),
                            linkVideoToSend // Thêm linkVideo vào API call
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            messageModel -> {
                                if (messageModel.isSuccess()) {
                                    Toast.makeText(getApplicationContext(), messageModel.getMessage(), Toast.LENGTH_LONG).show();
                                    finish(); // Đóng activity sau khi sửa thành công
                                } else {
                                    Toast.makeText(getApplicationContext(), messageModel.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            },
                            throwable -> {
                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                            }
                    ));
        }
    }

    private void addProduct() {
        String str_productName = binding.name.getText().toString().trim();
        String str_price = binding.price.getText().toString().trim();
        String str_description = binding.des.getText().toString().trim();
        String str_count = binding.countProduct.getText().toString().trim();
        String str_linkVideo = binding.linkVideo.getText().toString().trim(); // Lấy giá trị linkVideo

        if (TextUtils.isEmpty(str_count) || TextUtils.isEmpty(str_productName) || TextUtils.isEmpty(str_price) || TextUtils.isEmpty(str_description) || category == 0 || imageUrls.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please fill all required fields and upload at least one image", Toast.LENGTH_LONG).show();
        } else {
            // Chuyển danh sách imageUrls thành chuỗi JSON
            String str_image = new Gson().toJson(imageUrls);
            // Nếu linkVideo rỗng, gửi null
            String linkVideoToSend = str_linkVideo.isEmpty() ? null : str_linkVideo;

            compositeDisposable.add(apiSale.insertProduct(
                            str_productName,
                            str_price,
                            str_image,
                            str_description,
                            category,
                            Integer.parseInt(str_count),
                            linkVideoToSend // Thêm linkVideo vào API call
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            messageModel -> {
                                if (messageModel.isSuccess()) {
                                    Toast.makeText(getApplicationContext(), messageModel.getMessage(), Toast.LENGTH_LONG).show();
                                    binding.name.setText("");
                                    binding.price.setText("");
                                    binding.des.setText("");
                                    binding.countProduct.setText("");
                                    binding.linkVideo.setText(""); // Xóa trường linkVideo
                                    imageUrls.clear();
                                    imageUrlAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(getApplicationContext(), messageModel.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            },
                            throwable -> {
                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                            }
                    ));
        }
    }

    private void uploadImage(Uri uri) {
        File file = new File(getPath(uri));
        Log.d(TAG, "Path: " + file.getAbsolutePath());

        RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        Call<MessageModel> call = apiSale.uploadfile(fileToUpload);
        call.enqueue(new Callback<MessageModel>() {
            @Override
            public void onResponse(Call<MessageModel> call, Response<MessageModel> response) {
                MessageModel serverResponse = response.body();
                if (serverResponse != null) {
                    if (serverResponse.isSuccess()) {
                        // Thêm URL hình ảnh vào danh sách
                        String imageUrl = Utils.BASE_URL + "images/" + serverResponse.getName();
                        imageUrls.add(imageUrl);
                        imageUrlAdapter.notifyItemInserted(imageUrls.size() - 1);
                    } else {
                        Toast.makeText(getApplicationContext(), serverResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.v("Response", serverResponse.toString());
                }
            }

            @Override
            public void onFailure(Call<MessageModel> call, Throwable t) {
                Log.d("log", t.toString());
                Toast.makeText(getApplicationContext(), "Upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getPath(Uri uri) {
        String result;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(index);
            cursor.close();
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}