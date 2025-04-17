package com.example.app_technical_admin.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.SliderAdapter;
import com.example.app_technical_admin.model.Cart;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.Promotion;
import com.example.app_technical_admin.utils.Utils;
import com.nex3z.notificationbadge.NotificationBadge;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.paperdb.Paper;
import me.relex.circleindicator.CircleIndicator3;

public class DetailActivity extends AppCompatActivity {
    TextView productName, price, description, txtDiscountedPrice, txtPromotion;
    Button btnAdd;
    AppCompatButton btnYoutube;
    ViewPager2 imageSlider;
    CircleIndicator3 indicator;
    Spinner spinner;
    Toolbar toolbar;
    NewProduct newProduct;
    NotificationBadge badge;
    double discountedPrice;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean isAutoSliding = true; // Biến để kiểm soát trạng thái tự động chuyển slide
    private boolean isSliderInitialized = false; // Biến để kiểm tra xem slider đã được khởi tạo chưa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initView();
        ActionToolBar();
        initData();
        initControl();
    }

    private void initControl() {
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCart();
                Paper.book().write("cart", Utils.arrayCart);
            }
        });
        btnYoutube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Kiểm tra linkVideo có tồn tại và không rỗng
                String linkVideo = newProduct.getLinkVideo();
                if (linkVideo != null && !linkVideo.trim().isEmpty()) {
                    Intent youtube = new Intent(getApplicationContext(), YoutubeActivity.class);
                    youtube.putExtra("linkVideo", linkVideo);
                    startActivity(youtube);
                } else {
                    // Hiển thị thông báo nếu không có link video
                    Toast.makeText(getApplicationContext(), "Sản phẩm này không có video để xem.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Thêm listener để dừng tự động chuyển slide khi người dùng vuốt tay
        imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    // Người dùng đang vuốt tay, dừng tự động chuyển slide
                    isAutoSliding = false;
                    sliderHandler.removeCallbacks(sliderRunnable);
                } else if (state == ViewPager2.SCROLL_STATE_IDLE && !isAutoSliding) {
                    // Khi người dùng dừng vuốt, tiếp tục tự động chuyển slide
                    isAutoSliding = true;
                    sliderHandler.postDelayed(sliderRunnable, 2000);
                }
            }
        });
    }

    private void addCart() {
        if (Utils.arrayCart == null) {
            Utils.arrayCart = new ArrayList<>();
        }

        if (Utils.arrayCart.size() > 0) {
            boolean flag = false;
            int count = Integer.parseInt(spinner.getSelectedItem().toString());
            for (int i = 0; i < Utils.arrayCart.size(); i++) {
                if (Utils.arrayCart.get(i).getId() == newProduct.getId()) {
                    Utils.arrayCart.get(i).setCount(count + Utils.arrayCart.get(i).getCount());
                    flag = true;
                }
            }

            if (!flag) {
                addNewCartItem(count);
            }
        } else {
            int count = Integer.parseInt(spinner.getSelectedItem().toString());
            addNewCartItem(count);
        }

        updateCartBadge();
    }

    private void addNewCartItem(int count) {
        long priceToUse = (long) (discountedPrice > 0 ? discountedPrice : Double.parseDouble(newProduct.getPrice()));
        Cart cart = new Cart();
        cart.setPrice(priceToUse);
        cart.setCount(count);
        cart.setId(newProduct.getId());
        cart.setProductName(newProduct.getProductName());
        cart.setProductImg(newProduct.getImage() != null && !newProduct.getImage().isEmpty() ? newProduct.getImage().get(0) : "");
        cart.setCountStock(newProduct.getCountStock());
        Utils.arrayCart.add(cart);
    }

    private void updateCartBadge() {
        if (badge != null) {
            int totalItems = 0;
            for (Cart item : Utils.arrayCart) {
                totalItems += item.getCount();
            }
            badge.setText(String.valueOf(totalItems));
        }
    }

    private void initData() {
        newProduct = (NewProduct) getIntent().getSerializableExtra("detail");
        if (newProduct != null) {
            productName.setText(newProduct.getProductName());
            description.setText(newProduct.getDescription());
            DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

            // Giá gốc
            double originalPrice = Double.parseDouble(newProduct.getPrice());
            price.setText(decimalFormat.format(originalPrice) + " VND");

            String linkVideo = newProduct.getLinkVideo();
            if (linkVideo == null || linkVideo.trim().isEmpty()) {
                btnYoutube.setVisibility(View.GONE);
            } else {
                btnYoutube.setVisibility(View.VISIBLE);
            }

            // Thiết lập slider hình ảnh
            if (newProduct.getImage() != null && !newProduct.getImage().isEmpty()) {
                SliderAdapter sliderAdapter = new SliderAdapter(this, newProduct.getImage());
                imageSlider.setAdapter(sliderAdapter);
                indicator.setViewPager(imageSlider);

                // Tự động chuyển slide
                sliderRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!isAutoSliding) return; // Không chạy nếu tự động chuyển slide bị dừng
                        int currentItem = imageSlider.getCurrentItem();
                        int totalItems = sliderAdapter.getItemCount();
                        if (totalItems > 0) {
                            int nextItem = (currentItem + 1) % totalItems;
                            Log.d("SliderDebug", "Current: " + currentItem + ", Next: " + nextItem);
                            imageSlider.setCurrentItem(nextItem, true);
                            sliderHandler.postDelayed(this, 2000);
                        }
                    }
                };

                // Chỉ bắt đầu tự động chuyển slide nếu chưa được khởi tạo
                if (!isSliderInitialized) {
                    sliderHandler.postDelayed(sliderRunnable, 2000);
                    isSliderInitialized = true;
                }
            } else {
                List<String> defaultImages = new ArrayList<>();
                defaultImages.add("https://via.placeholder.com/150");
                SliderAdapter sliderAdapter = new SliderAdapter(this, defaultImages);
                imageSlider.setAdapter(sliderAdapter);
                indicator.setViewPager(imageSlider);
            }

            // Kiểm tra khuyến mãi
            if (newProduct.getPromotions() != null && !newProduct.getPromotions().isEmpty()) {
                StringBuilder promotionInfo = new StringBuilder();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date currentDate = new Date();
                boolean hasValidPromotion = false;
                discountedPrice = originalPrice;

                for (Promotion promotion : newProduct.getPromotions()) {
                    try {
                        Date startDate = dateFormat.parse(promotion.getStartDate());
                        Date endDate = dateFormat.parse(promotion.getEndDate());
                        if (currentDate.after(startDate) && currentDate.before(endDate)) {
                            promotionInfo.append(promotion.getDiscount())
                                    .append(promotion.getDiscountType().equals("percent") ? "%" : " VNĐ")
                                    .append(" Off") // Add " off" after the discount value and unit
                                    .append(" (").append(promotion.getName()).append(")");

                            double discount = promotion.getDiscount();
                            if (promotion.getDiscountType().equals("percent")) {
                                discountedPrice = originalPrice * (1 - discount / 100);
                            } else {
                                discountedPrice = originalPrice - discount;
                            }
                            hasValidPromotion = true;
                            break;
                        }
                    } catch (ParseException e) {
                        Log.e("DetailActivity", "Error parsing promotion dates: " + e.getMessage());
                    }
                }

                if (hasValidPromotion) {
                    price.setPaintFlags(price.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    txtDiscountedPrice.setText(decimalFormat.format(discountedPrice) + " VND");
                    txtDiscountedPrice.setVisibility(View.VISIBLE);
                    txtPromotion.setText(promotionInfo.toString());
                    txtPromotion.setVisibility(View.VISIBLE);
                } else {
                    price.setPaintFlags(price.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    txtDiscountedPrice.setVisibility(View.GONE);
                    txtPromotion.setVisibility(View.GONE);
                    discountedPrice = 0;
                }
            } else {
                price.setPaintFlags(price.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                txtDiscountedPrice.setVisibility(View.GONE);
                txtPromotion.setVisibility(View.GONE);
                discountedPrice = 0;
            }

            // Spinner cho số lượng
            List<Integer> number = new ArrayList<>();
            for (int i = 1; i < newProduct.getCountStock() + 1; i++) {
                number.add(i);
            }
            ArrayAdapter<Integer> adapterspin = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, number);
            spinner.setAdapter(adapterspin);
        } else {
            productName.setText("No product data");
        }
    }

    private void initView() {
        productName = findViewById(R.id.txtProductName);
        price = findViewById(R.id.txtPrice);
        description = findViewById(R.id.txtdetailDes);
        txtDiscountedPrice = findViewById(R.id.txtDiscountedPrice);
        txtPromotion = findViewById(R.id.txtPromotion);
        btnAdd = findViewById(R.id.btnAddtocart);
        btnYoutube = findViewById(R.id.btnYoutube);
        imageSlider = findViewById(R.id.imageSlider);
        indicator = findViewById(R.id.indicator);
        spinner = findViewById(R.id.spinner);
        toolbar = findViewById(R.id.toolbar);
        badge = findViewById(R.id.menu_count);
        FrameLayout frameLayoutCart = findViewById(R.id.frameCart);
        frameLayoutCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cart = new Intent(getApplicationContext(), CartActivity.class);
                startActivity(cart);
            }

        });
        if (Utils.arrayCart != null) {
            int totalItem = 0;
            for (int i = 0; i < Utils.arrayCart.size(); i++) {
                totalItem = totalItem + Utils.arrayCart.get(i).getCount();
            }
            badge.setText(String.valueOf(totalItem));
        }
    }

    private void ActionToolBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.arrayCart != null) {
            int totalItem = 0;
            for (int i = 0; i < Utils.arrayCart.size(); i++) {
                totalItem = totalItem + Utils.arrayCart.get(i).getCount();
            }
            badge.setText(String.valueOf(totalItem));
        }
        // Chỉ tiếp tục tự động chuyển slide nếu đang ở trạng thái cho phép
        isAutoSliding = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dừng tự động chuyển slide
        isAutoSliding = false;
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dọn dẹp Handler và đặt lại trạng thái khởi tạo
        sliderHandler.removeCallbacks(sliderRunnable);
        isSliderInitialized = false;
    }
}