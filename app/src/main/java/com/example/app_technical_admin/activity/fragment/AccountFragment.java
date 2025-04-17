package com.example.app_technical_admin.activity.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.MyOrderActivity;
import com.example.app_technical_admin.activity.UserInforActivity;
import com.example.app_technical_admin.adapter.NewProductAdapter;
import com.example.app_technical_admin.model.ChatAIHistoryResponse;
import com.example.app_technical_admin.model.ChatAIMessage;
import com.example.app_technical_admin.model.MessageAI;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private TextView txtUserName, txtUserTag;
    private ImageView imgAvatar, imgInfor;
    private LinearLayout layoutUserInfo, layoutOrders;
    private LinearLayout layoutPending, layoutAccepted, layoutDelivering, layoutDelivered, layoutCanceled;
    private RecyclerView recyclerViewAccount;
    private NewProductAdapter newProductAdapter;
    private List<NewProduct> recommendedProducts;
    private ApiSale apiSale;
    private TextView tvRecommendedProductsTitle;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo API
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_account, container, false);

        // Ánh xạ
        mapping(view);
        displayUserInfo();

        // Thiết lập RecyclerView
        setupRecyclerView(view);

        // Lấy danh sách sản phẩm được đề xuất
        fetchRecommendedProducts();

        // Sự kiện click
        setupClickEvents();

        return view;
    }

    private void setupClickEvents() {
        // Sự kiện click vào layoutUserInfo
        layoutUserInfo.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), UserInforActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting UserInforActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào imgInfor
        imgInfor.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), MyOrderActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutPending (Tab Pending - vị trí 1)
        layoutPending.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), MyOrderActivity.class);
                intent.putExtra("tab_position", 1); // Tab Pending
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutAccepted (Tab Accepted - vị trí 2)
        layoutAccepted.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), MyOrderActivity.class);
                intent.putExtra("tab_position", 2); // Tab Accepted
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutDelivering (Tab Delivering - vị trí 3)
        layoutDelivering.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), MyOrderActivity.class);
                intent.putExtra("tab_position", 3); // Tab Delivering
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutDelivered (Tab Delivered successfully - vị trí 4)
        layoutDelivered.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), MyOrderActivity.class);
                intent.putExtra("tab_position", 4); // Tab Delivered successfully
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });

        // Sự kiện click vào layoutCanceled (Tab Canceled - vị trí 5)
        layoutCanceled.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(), MyOrderActivity.class);
                intent.putExtra("tab_position", 5); // Tab Canceled
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MyOrderActivity: " + e.getMessage());
            }
        });
    }

    private void mapping(View view) {
        txtUserName = view.findViewById(R.id.txtUserName);
        txtUserTag = view.findViewById(R.id.txtUserTag);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        layoutUserInfo = view.findViewById(R.id.layoutUserInfo);
        layoutOrders = view.findViewById(R.id.layoutOrders);
        imgInfor = view.findViewById(R.id.imgInfor);
        layoutPending = view.findViewById(R.id.layoutPending);
        layoutAccepted = view.findViewById(R.id.layoutAccepted);
        layoutDelivering = view.findViewById(R.id.layoutDelivering);
        layoutDelivered = view.findViewById(R.id.layoutDelivered);
        layoutCanceled = view.findViewById(R.id.layoutCanceled);
        recyclerViewAccount = view.findViewById(R.id.recyclerViewAccount);
        tvRecommendedProductsTitle = view.findViewById(R.id.tvRecommendedProductsTitle);
//        tvNoRecommendedProducts = view.findViewById(R.id.tvNoRecommendedProducts);
    }

    private void displayUserInfo() {
        if (Utils.user_current != null) {
            if (Utils.user_current.getUserName() != null && !Utils.user_current.getUserName().isEmpty()) {
                txtUserName.setText(Utils.user_current.getUserName());
            } else {
                txtUserName.setText("Account Name");
            }

            if (Utils.user_current.getAvatar() != null && !Utils.user_current.getAvatar().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(Utils.user_current.getAvatar(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgAvatar.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding avatar: " + e.getMessage());
                    imgAvatar.setImageResource(R.drawable.ic_username);
                }
            } else {
                imgAvatar.setImageResource(R.drawable.ic_username);
            }
        } else {
            Log.w(TAG, "Utils.user_current is null");
            txtUserName.setText("Account Name");
            imgAvatar.setImageResource(R.drawable.ic_username);
        }
    }

    private void setupRecyclerView(View view) {
        recyclerViewAccount = view.findViewById(R.id.recyclerViewAccount);
        // Sử dụng GridLayoutManager với 2 cột
        recyclerViewAccount.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recommendedProducts = new ArrayList<>();
        newProductAdapter = new NewProductAdapter(requireContext(), recommendedProducts);
        recyclerViewAccount.setAdapter(newProductAdapter);
    }
    private void fetchProductList(Set<Integer> productIds) {
        Log.d(TAG, "Gọi API getNewProduct với product_ids: " + productIds.toString());
        compositeDisposable.add(apiSale.getNewProduct()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        newProductModel -> {
                            Log.d(TAG, "API getNewProduct response: " + new Gson().toJson(newProductModel));
                            if (newProductModel.isSuccess()) {
                                List<NewProduct> allProducts = newProductModel.getResult();
                                Log.d(TAG, "Số lượng sản phẩm từ API: " + (allProducts != null ? allProducts.size() : 0));
                                if (allProducts != null) {
                                    // Lọc các sản phẩm có ID nằm trong productIds
                                    recommendedProducts.clear();
                                    for (NewProduct product : allProducts) {
                                        if (productIds.contains(product.getId())) {
                                            recommendedProducts.add(product);
                                        }
                                    }
                                    Log.d(TAG, "Số lượng sản phẩm được lọc: " + recommendedProducts.size());
                                    newProductAdapter.notifyDataSetChanged();
                                    Log.d(TAG, "Đã tải " + recommendedProducts.size() + " sản phẩm được đề xuất");

                                    // Hiển thị hoặc ẩn tiêu đề và RecyclerView
                                    if (recommendedProducts.isEmpty()) {
                                        tvRecommendedProductsTitle.setVisibility(View.GONE);
                                        recyclerViewAccount.setVisibility(View.GONE);
//                                        tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                                    } else {
                                        tvRecommendedProductsTitle.setVisibility(View.VISIBLE);
                                        recyclerViewAccount.setVisibility(View.VISIBLE);
//                                        tvNoRecommendedProducts.setVisibility(View.GONE);
                                    }
                                } else {
                                    Log.e(TAG, "Danh sách sản phẩm trống");
                                    tvRecommendedProductsTitle.setVisibility(View.GONE);
                                    recyclerViewAccount.setVisibility(View.GONE);
//                                    tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Log.e(TAG, "Không thể tải danh sách sản phẩm: " + newProductModel.getMessage());
                                tvRecommendedProductsTitle.setVisibility(View.GONE);
                                recyclerViewAccount.setVisibility(View.GONE);
//                                tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Lỗi khi tải danh sách sản phẩm: " + throwable.getMessage());
                            tvRecommendedProductsTitle.setVisibility(View.GONE);
                            recyclerViewAccount.setVisibility(View.GONE);
//                            tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                        }
                ));
    }

    private void fetchRecommendedProducts() {
        int userId = Utils.user_current.getId();
        Log.d(TAG, "Lấy danh sách sản phẩm được đề xuất cho user_id: " + userId);

        // Bước 1: Lấy lịch sử trò chuyện
        apiSale.getChatHistory(userId).enqueue(new Callback<ChatAIHistoryResponse>() {
            @Override
            public void onResponse(Call<ChatAIHistoryResponse> call, Response<ChatAIHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatAIHistoryResponse chatResponse = response.body();
                    Log.d(TAG, "API getChatHistory response: " + new Gson().toJson(chatResponse));

                    if (chatResponse.isSuccess()) {
                        List<ChatAIMessage> chatHistory = chatResponse.getChatHistory();
                        Log.d(TAG, "Số lượng tin nhắn trong lịch sử: " + (chatHistory != null ? chatHistory.size() : 0));

                        if (chatHistory != null && !chatHistory.isEmpty()) {
                            // Trích xuất product_ids từ các tin nhắn TYPE_BOT_PRODUCT
                            Set<Integer> productIds = new HashSet<>();
                            for (ChatAIMessage message : chatHistory) {
                                Log.d(TAG, "Tin nhắn: type=" + message.getMessageType() + ", product_ids=" + message.getProductIds());
                                if (message.getMessageType() == MessageAI.TYPE_BOT_PRODUCT) {
                                    String productDataJson = message.getProductIds();
                                    if (productDataJson != null && !productDataJson.isEmpty()) {
                                        try {
                                            if (productDataJson.startsWith("[") && productDataJson.contains("id")) {
                                                // Dữ liệu là danh sách sản phẩm đầy đủ
                                                List<NewProduct> products = new Gson().fromJson(productDataJson, new TypeToken<List<NewProduct>>() {}.getType());
                                                for (NewProduct product : products) {
                                                    productIds.add(product.getId());
                                                }
                                            } else {
                                                // Dữ liệu là danh sách ID
                                                List<Integer> ids = new Gson().fromJson(productDataJson, new TypeToken<List<Integer>>() {}.getType());
                                                productIds.addAll(ids);
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Lỗi khi phân tích product_ids: " + e.getMessage());
                                        }
                                    }
                                }
                            }

                            Log.d(TAG, "Danh sách product_ids: " + productIds.toString());
                            if (productIds.isEmpty()) {
                                Log.d(TAG, "Không tìm thấy sản phẩm được đề xuất trong lịch sử trò chuyện");
                                tvRecommendedProductsTitle.setVisibility(View.GONE);
                                recyclerViewAccount.setVisibility(View.GONE);
//                                tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                                return;
                            }

                            // Bước 2: Lấy danh sách sản phẩm từ API
                            fetchProductList(productIds);
                        } else {
                            Log.d(TAG, "Lịch sử trò chuyện trống");
                            tvRecommendedProductsTitle.setVisibility(View.GONE);
                            recyclerViewAccount.setVisibility(View.GONE);
//                            tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e(TAG, "Không thể lấy lịch sử trò chuyện: Response success is false");
                        tvRecommendedProductsTitle.setVisibility(View.GONE);
                        recyclerViewAccount.setVisibility(View.GONE);
//                        tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e(TAG, "Không thể lấy lịch sử trò chuyện: Response không thành công, mã: " + (response != null ? response.code() : "null"));
                    tvRecommendedProductsTitle.setVisibility(View.GONE);
                    recyclerViewAccount.setVisibility(View.GONE);
//                    tvNoRecommendedProducts.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ChatAIHistoryResponse> call, Throwable t) {
                Log.e(TAG, "Lỗi khi lấy lịch sử trò chuyện: " + t.getMessage());
                tvRecommendedProductsTitle.setVisibility(View.GONE);
                recyclerViewAccount.setVisibility(View.GONE);
//                tvNoRecommendedProducts.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear();
    }
}