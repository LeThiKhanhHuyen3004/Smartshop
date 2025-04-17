package com.example.app_technical_admin.activity.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app_technical_admin.activity.ContactActivity;
import com.example.app_technical_admin.activity.InformationActivity;
import com.example.app_technical_admin.activity.PaymentExActivity;
import com.example.app_technical_admin.activity.UserActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.activity.AdvertisingActivity;
import com.example.app_technical_admin.activity.CartActivity;
import com.example.app_technical_admin.activity.ChatActivity;
import com.example.app_technical_admin.activity.JoinActivity;
import com.example.app_technical_admin.activity.LoginActivity;
import com.example.app_technical_admin.activity.ManagerActivity;
import com.example.app_technical_admin.activity.MessageAIActivity;
import com.example.app_technical_admin.activity.PhoneActivity;
import com.example.app_technical_admin.activity.PromotionActivity;
import com.example.app_technical_admin.activity.SearchActivity;
import com.example.app_technical_admin.activity.StatisticActivity;
import com.example.app_technical_admin.activity.ViewOrderActivity;
import com.example.app_technical_admin.adapter.CategoryAdapter;
import com.example.app_technical_admin.adapter.NewProductAdapter;
import com.example.app_technical_admin.adapter.PromotionProductAdapter;
import com.example.app_technical_admin.adapter.TypeOfProductAdapter;
import com.example.app_technical_admin.model.CategoryModel;
import com.example.app_technical_admin.model.Cart;
import com.example.app_technical_admin.model.NewProduct;
import com.example.app_technical_admin.model.Promotion;
import com.example.app_technical_admin.model.TypeOfProduct;
import com.example.app_technical_admin.model.User;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;

import com.nex3z.notificationbadge.NotificationBadge;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.paperdb.Paper;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainFragment extends Fragment {

    private Toolbar toolbar;
    private RecyclerView recyclerViewMainPage, recyclerViewCategory, recyclerViewPromotion;
    private LinearLayout promotionTitleLayout; // Ánh xạ LinearLayout chứa txtPromotionTitle
    private TextView txtPromotionTitle;
    private NavigationView navigationView;
    private ListView listViewMainPage;
    private DrawerLayout drawerLayout;
    private TypeOfProductAdapter typeOfProductAdapter;
    private List<TypeOfProduct> typeOfProductArray;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ApiSale apiSale;
    private List<NewProduct> arrayNewProduct;
    private List<NewProduct> promotionProductList; // Danh sách sản phẩm khuyến mãi
    private String promotionTitle; // Tên chương trình khuyến mãi
    private NewProductAdapter newProductAdapter;
    private PromotionProductAdapter promotionProductAdapter;
    private NotificationBadge badge;
    private FrameLayout frameCart;
    private ImageView imgSearch;
    private LottieAnimationView chatBubble;
    private ImageSlider imageSlider;
    private TextView textView;
    private FrameLayout floatingBubbleContainer;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private static final int PAYMENT_REQUEST_CODE = 100;


    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        Paper.init(requireContext());
        if (Paper.book().read("user") != null) {
            User user = Paper.book().read("user");
            Utils.user_current = user;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_main, container, false);

        // Mapping views
        mapping(view);
        setupCategoryList(view);
        getToken();
        actionBar();



        // Thiết lập RecyclerView cho sản phẩm khuyến mãi (lướt ngang)
        LinearLayoutManager layoutManagerPromotion = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewPromotion.setLayoutManager(layoutManagerPromotion);
        recyclerViewPromotion.setHasFixedSize(true);

        setupFloatingBubble(view);

        if (isConnected(requireContext())) {
            actionViewFlipper();
            getTypeOfProduct();
            getNewProduct();
            getEventClick();
        } else {
            Toast.makeText(requireContext(), "No Internet, please connect", Toast.LENGTH_LONG).show();
        }

        // Thiết lập sự kiện click cho promotionTitleLayout
        promotionTitleLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PromotionActivity.class);
            intent.putExtra("promotionList", new ArrayList<>(promotionProductList));
            intent.putExtra("promotionTitle", promotionTitle);
            startActivity(intent);
        });

        return view;
    }



    private void setupFloatingBubble(View view) {
        floatingBubbleContainer = view.findViewById(R.id.floating_bubble_container);
        chatBubble = view.findViewById(R.id.chat_bubble);

        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;

        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        floatingBubbleContainer.post(() -> {
            floatingBubbleContainer.setX(screenWidth - floatingBubbleContainer.getWidth() - margin);
            floatingBubbleContainer.setY(screenHeight - floatingBubbleContainer.getHeight() - getStatusBarHeight() - getNavigationBarHeight() - margin);
        });

        floatingBubbleContainer.setOnTouchListener(new View.OnTouchListener() {
            private float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - lastX;
                        float deltaY = event.getRawY() - lastY;

                        float newX = floatingBubbleContainer.getX() + deltaX;
                        float newY = floatingBubbleContainer.getY() + deltaY;

                        newX = Math.max(0, Math.min(newX, screenWidth - floatingBubbleContainer.getWidth()));
                        newY = Math.max(0, Math.min(newY, screenHeight - floatingBubbleContainer.getHeight() - getStatusBarHeight() - getNavigationBarHeight()));

                        floatingBubbleContainer.setX(newX);
                        floatingBubbleContainer.setY(newY);

                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        float finalDeltaX = event.getRawX() - initialTouchX;
                        float finalDeltaY = event.getRawY() - initialTouchY;
                        if (Math.abs(finalDeltaX) < 5 && Math.abs(finalDeltaY) < 5) {
                            Intent intent = new Intent(requireContext(), MessageAIActivity.class);
                            startActivity(intent);
                        } else {
                            float currentX = floatingBubbleContainer.getX();
                            if (currentX < screenWidth / 2) {
                                floatingBubbleContainer.animate().x(0).setDuration(200).start();
                            } else {
                                floatingBubbleContainer.animate().x(screenWidth - floatingBubbleContainer.getWidth()).setDuration(200).start();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private int getNavigationBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        if (!TextUtils.isEmpty(s)) {

                            compositeDisposable.add(apiSale.updateToken(Utils.user_current.getId(), s)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            messageModel -> Log.d("TOKEN_UPDATE", "Token updated successfully"),
                                            throwable -> Log.e("TOKEN_UPDATE_ERROR", "Error updating token: " + throwable.getMessage())
                                    ));
                        }
                    }
                });
        compositeDisposable.add(apiSale.getToken(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userModel -> {
                            if (userModel.isSuccess()) {
                                Utils.ID_RECEIVED = String.valueOf(userModel.getResult().get(0).getId());
                            }
                        },
                        throwable -> Log.e("TOKEN_UPDATE_ERROR", "Error getting token: " + throwable.getMessage())
                ));
    }

    private void getEventClick() {
        listViewMainPage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TypeOfProduct selectedItem = typeOfProductArray.get(position);
                String itemName = selectedItem.getProductName();
                int userStatus = Utils.user_current != null ? Utils.user_current.getStatus() : 0;

                switch (itemName) {
                    case "Home page":

                    case "Information":
                        startActivity(new Intent(requireContext(), InformationActivity.class));
                        requireActivity().finish();
                        break;
                    case "Contact":
                        startActivity(new Intent(requireContext(), ContactActivity.class));
                        requireActivity().finish();
                        // Handle accordingly, e.g., replace with another fragment
                        break;
                    case "Store orders":
                        if (userStatus == 1) {
                            startActivity(new Intent(requireContext(), ViewOrderActivity.class));
                        } else {
                            Toast.makeText(requireContext(), "No permission", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "Product Management":
                        if (userStatus == 1) {
                            startActivity(new Intent(requireContext(), ManagerActivity.class));
                        } else {
                            Toast.makeText(requireContext(), "No permission", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "Messages":
                        if (userStatus == 1) {
                            startActivity(new Intent(requireContext(), UserActivity.class));
                        } else {
                            Toast.makeText(requireContext(), "No permission", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "Financial statistics":
                        if (userStatus == 1) {
                            startActivity(new Intent(requireContext(), StatisticActivity.class));
                        } else {
                            Toast.makeText(requireContext(), "No permission", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "Log out":
                        Paper.book().delete("user");
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finish();
                        break;
                    default:
                        Toast.makeText(requireContext(), "Feature not implemented", Toast.LENGTH_SHORT).show();
                        break;
                }
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
                                arrayNewProduct = newProductModel.getResult();
                                Log.d("MainFragment", "Received products: " + arrayNewProduct.size());
                                for (NewProduct product : arrayNewProduct) {
                                    Log.d("MainFragment", "Product: " + product.getProductName() + ", Images: " + product.getImage());
                                }

                                newProductAdapter = new NewProductAdapter(requireContext(), arrayNewProduct);
                                recyclerViewMainPage.setAdapter(newProductAdapter);

                                // Lọc sản phẩm có khuyến mãi
                                filterPromotionProducts();
                            } else {
                                Log.e("MainFragment", "Failed to fetch products: " + newProductModel.getMessage());
                                Toast.makeText(requireContext(), "No new products available: " + newProductModel.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        },
                        throwable -> {
                            Log.e("MainFragment", "Error fetching new products: ", throwable);
                            Toast.makeText(requireContext(), "Cannot connect to server: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                ));
    }

    private void filterPromotionProducts() {
        promotionProductList.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate = new Date();
        promotionTitle = "Ongoing Promotions"; // Tiêu đề cố định

        for (NewProduct product : arrayNewProduct) {
            if (product.getPromotions() != null && !product.getPromotions().isEmpty()) {
                for (Promotion promotion : product.getPromotions()) {
                    try {
                        Date startDate = dateFormat.parse(promotion.getStartDate());
                        Date endDate = dateFormat.parse(promotion.getEndDate());
                        if (currentDate.after(startDate) && currentDate.before(endDate)) {
                            promotionProductList.add(product);
                            // Không cập nhật promotionTitle nữa
                            break; // Chỉ lấy khuyến mãi đầu tiên hợp lệ
                        }
                    } catch (ParseException e) {
                        Log.e("MainFragment", "Error parsing promotion dates: " + e.getMessage());
                    }
                }
            }
        }

        // Hiển thị danh sách khuyến mãi nếu có sản phẩm
        if (!promotionProductList.isEmpty()) {
            txtPromotionTitle.setText(promotionTitle); // Hiển thị "Ongoing Promotions"
            promotionTitleLayout.setVisibility(View.VISIBLE);
            recyclerViewPromotion.setVisibility(View.VISIBLE);
            promotionProductAdapter = new PromotionProductAdapter(requireContext(), promotionProductList);
            recyclerViewPromotion.setAdapter(promotionProductAdapter);
        } else {
            txtPromotionTitle.setText(promotionTitle); // Hiển thị "Ongoing Promotions" ngay cả khi không có sản phẩm
            promotionTitleLayout.setVisibility(View.VISIBLE); // Vẫn hiển thị tiêu đề
            recyclerViewPromotion.setVisibility(View.GONE); // Ẩn danh sách nếu không có sản phẩm
        }
    }
    private void getTypeOfProduct() {
        compositeDisposable.add(apiSale.getTypeOfProduct()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        typeOfProductModel -> {
                            if (typeOfProductModel.isSuccess()) {
                                typeOfProductArray = typeOfProductModel.getResult();
                                int userStatus = Utils.user_current != null ? Utils.user_current.getStatus() : 0;
                                if (userStatus == 1) {
                                    typeOfProductArray.add(new TypeOfProduct("Store orders", "https://cdn1.iconfinder.com/data/icons/unicons-line-vol-5/24/package-512.png"));
                                    typeOfProductArray.add(new TypeOfProduct("Product Management", "https://cdn1.iconfinder.com/data/icons/unicons-line-vol-3/24/file-edit-alt-1024.png"));
                                    typeOfProductArray.add(new TypeOfProduct("Messages", "https://cdn1.iconfinder.com/data/icons/unicons-line-vol-2/24/comment-alt-message-512.png"));
                                    typeOfProductArray.add(new TypeOfProduct("Financial statistics", "https://cdn1.iconfinder.com/data/icons/unicons-line-vol-5/24/signal-alt-3-1024.png"));
                                }
                                typeOfProductArray.add(new TypeOfProduct("Log out", "https://cdn1.iconfinder.com/data/icons/unicons-line-vol-3/24/export-1024.png"));
                                typeOfProductAdapter = new TypeOfProductAdapter(requireContext(), typeOfProductArray);
                                listViewMainPage.setAdapter(typeOfProductAdapter);
                            }
                        },
                        throwable -> Log.e("API_ERROR", "Error: " + throwable.getMessage())
                ));
    }

    private void actionViewFlipper() {
        List<SlideModel> imageList = new ArrayList<>();
        compositeDisposable.add(apiSale.getAdvertising()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        advetisingModel -> {
                            if (advetisingModel.isSuccess()) {
                                for (int i = 0; i < advetisingModel.getResult().size(); i++) {
                                    imageList.add(new SlideModel(advetisingModel.getResult().get(i).getUrl(), null));
                                }
                                imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP);
                                imageSlider.setItemClickListener(new ItemClickListener() {
                                    @Override
                                    public void onItemSelected(int i) {
                                        Intent advertising = new Intent(requireContext(), AdvertisingActivity.class);
                                        advertising.putExtra("information", advetisingModel.getResult().get(i).getInformation());
                                        advertising.putExtra("url", advetisingModel.getResult().get(i).getUrl());
                                        startActivity(advertising);
                                    }

                                    @Override
                                    public void doubleClick(int i) {
                                    }
                                });
                            } else {
                                Toast.makeText(requireContext(), "No advertising available", Toast.LENGTH_LONG).show();
                            }
                        },
                        throwable -> Log.d("log", throwable.getMessage())
                ));
    }

    private void actionBar() {
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void mapping(View view) {
        imgSearch = view.findViewById(R.id.imageView);
        toolbar = view.findViewById(R.id.toolbarmainpage);
        imageSlider = view.findViewById(R.id.image_slider);
        recyclerViewMainPage = view.findViewById(R.id.recycleview);
        recyclerViewCategory = view.findViewById(R.id.recyclerViewCategory);
        recyclerViewPromotion = view.findViewById(R.id.recyclerViewPromotion);
        promotionTitleLayout = view.findViewById(R.id.promotionTitleLayout); // Ánh xạ LinearLayout
        txtPromotionTitle = view.findViewById(R.id.txtPromotionTitle);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerViewMainPage.setLayoutManager(layoutManager);
        recyclerViewMainPage.setHasFixedSize(true);
        listViewMainPage = view.findViewById(R.id.listviewmainpage);
        navigationView = view.findViewById(R.id.navigationview);
        drawerLayout = view.findViewById(R.id.drawerlayout);
        badge = view.findViewById(R.id.menu_count);
        frameCart = view.findViewById(R.id.frameCart);
        textView = view.findViewById(R.id.edtSearch);

        typeOfProductArray = new ArrayList<>();
        arrayNewProduct = new ArrayList<>();
        promotionProductList = new ArrayList<>();
        if (Paper.book().read("cart") != null) {
            Utils.arrayCart = Paper.book().read("cart");
        }
        if (Utils.arrayCart == null) {
            Utils.arrayCart = new ArrayList<>();
        } else {
            int totalItem = 0;
            for (Cart item : Utils.arrayCart) {
                totalItem += item.getCount();
            }
            badge.setText(String.valueOf(totalItem));
        }

        frameCart.setOnClickListener(v -> startActivity(new Intent(requireContext(), CartActivity.class)));
        imgSearch.setOnClickListener(v -> startActivity(new Intent(requireContext(), SearchActivity.class)));
        textView.setOnClickListener(v -> startActivity(new Intent(requireContext(), SearchActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        int totalItems = 0;
        for (Cart item : Utils.arrayCart) {
            totalItems += item.getCount();
        }
        badge.setText(String.valueOf(totalItems));
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    private void setupCategoryList(View view) {
        List<CategoryModel> categoryList = new ArrayList<>();
        categoryList.add(new CategoryModel("Phone", R.drawable.phone_ic));
        categoryList.add(new CategoryModel("Laptop", R.drawable.laptop_ic));
        categoryList.add(new CategoryModel("Gadget", R.drawable.accessory_ic));
        categoryList.add(new CategoryModel("Smart watch", R.drawable.smartwatch_ic));
        categoryList.add(new CategoryModel("Tablet", R.drawable.tab_ic));
        categoryList.add(new CategoryModel("PC, Printer", R.drawable.pc_ic));

        recyclerViewCategory.setLayoutManager(new GridLayoutManager(requireContext(), 6));

        CategoryAdapter adapter = new CategoryAdapter(requireContext(), categoryList, position -> {
            Intent phonePage = new Intent(requireContext(), PhoneActivity.class);
            switch (position) {
                case 0: phonePage.putExtra("category", 2); break;
                case 1: phonePage.putExtra("category", 1); break;
                case 2: phonePage.putExtra("category", 3); break;
                case 3: phonePage.putExtra("category", 4); break;
                case 4: phonePage.putExtra("category", 5); break;
                case 5: phonePage.putExtra("category", 6); break;
            }
            startActivity(phonePage);
        });

        recyclerViewCategory.setAdapter(adapter);
    }
}