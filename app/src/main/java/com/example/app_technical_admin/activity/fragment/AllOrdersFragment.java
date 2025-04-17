package com.example.app_technical_admin.activity.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_technical_admin.Interface.ItemClickDeleteListener;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.OrderUserAdapter;
import com.example.app_technical_admin.model.Order;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AllOrdersFragment extends Fragment {

    private RecyclerView recyclerViewAllOrders;
    private ApiSale apiSale;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private List<Order> orderList;
    private OrderUserAdapter orderUserAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_orders, container, false);

        // Ánh xạ view
        recyclerViewAllOrders = view.findViewById(R.id.recyclerViewAllOrders);

        // Khởi tạo API
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        // Cấu hình RecyclerView
        recyclerViewAllOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderList = new ArrayList<>();
        orderUserAdapter = new OrderUserAdapter(getContext(), orderList, new ItemClickDeleteListener() {
            @Override
            public void onClickDelete(int id_order) {
                showDeleteOrder(id_order);
            }
        });
        recyclerViewAllOrders.setAdapter(orderUserAdapter);

        // Lấy dữ liệu đơn hàng
        getAllOrders();

        return view;
    }

    private void getAllOrders() {
        // Lấy ID người dùng hiện tại
        int userId = Utils.user_current != null ? Utils.user_current.getId() : 0;
        if (userId <= 0) {
            Toast.makeText(getContext(), "Không thể xác định người dùng, vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi API để lấy đơn hàng của người dùng hiện tại
        compositeDisposable.add(apiSale.viewOrder(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orderModel -> {
                            if (orderModel.isSuccess()) {
                                orderList.clear();
                                orderList.addAll(orderModel.getResult());
                                orderUserAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getContext(), "Không thể lấy danh sách đơn hàng!", Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> {
                            Log.e("AllOrdersFragment", "Lỗi API: " + throwable.getMessage());
                            Toast.makeText(getContext(), "Lỗi khi lấy đơn hàng!", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void showDeleteOrder(int idOrder) {
        // Hiển thị popup menu để xác nhận xóa
        View view = recyclerViewAllOrders.findViewById(R.id.statusOrder);
        if (view != null) {
            PopupMenu popupMenu = new PopupMenu(getContext(), view);
            popupMenu.inflate(R.menu.menu_delete);
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.deleteOrder) {
                    deleteOrder(idOrder);
                }
                return false;
            });
            popupMenu.show();
        }
    }

    private void deleteOrder(int idOrder) {
        // Gọi API để xóa đơn hàng
        compositeDisposable.add(apiSale.deleteOrder(idOrder)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        messageModel -> {
                            if (messageModel.isSuccess()) {
                                Toast.makeText(getContext(), "Xóa đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                                getAllOrders(); // Làm mới danh sách đơn hàng sau khi xóa
                            } else {
                                Toast.makeText(getContext(), "Xóa đơn hàng thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> {
                            Log.e("AllOrdersFragment", "Lỗi xóa đơn hàng: " + throwable.getMessage());
                            Toast.makeText(getContext(), "Lỗi khi xóa đơn hàng!", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear(); // Giải phóng tài nguyên
    }
}