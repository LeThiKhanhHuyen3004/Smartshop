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

public class DeliveredSuccessfullyFragment extends Fragment {

    private RecyclerView recyclerViewDeliveredSuccessfullyOrders;
    private ApiSale apiSale;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private List<Order> deliveredSuccessfullyOrderList;
    private OrderUserAdapter orderUserAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivered_successfully, container, false);

        // Ánh xạ view
        recyclerViewDeliveredSuccessfullyOrders = view.findViewById(R.id.recyclerViewDeliveredSuccessfullyOrders);

        // Khởi tạo API
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);

        // Cấu hình RecyclerView
        recyclerViewDeliveredSuccessfullyOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        deliveredSuccessfullyOrderList = new ArrayList<>();
        orderUserAdapter = new OrderUserAdapter(getContext(), deliveredSuccessfullyOrderList, new ItemClickDeleteListener() {
            @Override
            public void onClickDelete(int id_order) {
                showDeleteOrder(id_order);
            }
        });
        recyclerViewDeliveredSuccessfullyOrders.setAdapter(orderUserAdapter);

        // Lấy dữ liệu đơn hàng có trạng thái Delivered successfully
        getDeliveredSuccessfullyOrders();

        return view;
    }

    private void getDeliveredSuccessfullyOrders() {
        // Kiểm tra userId của người dùng hiện tại
        if (Utils.user_current == null || Utils.user_current.getId() <= 0) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = Utils.user_current.getId();
        Log.d("DeliveredSuccessfullyFragment", "Lấy đơn hàng với userId: " + userId);

        // Gọi API để lấy đơn hàng của người dùng hiện tại, sau đó lọc các đơn hàng có status = 3 (Delivered successfully)
        compositeDisposable.add(apiSale.viewOrder(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orderModel -> {
                            if (orderModel.isSuccess()) {
                                deliveredSuccessfullyOrderList.clear();
                                // Lọc các đơn hàng có trạng thái Delivered successfully (status = 3)
                                for (Order order : orderModel.getResult()) {
                                    if (order.getStatus() == 3) {
                                        deliveredSuccessfullyOrderList.add(order);
                                    }
                                }
                                orderUserAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getContext(), "Không thể lấy danh sách đơn hàng Delivered successfully!", Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> {
                            Log.e("DeliveredSuccessfullyFragment", "Lỗi API: " + throwable.getMessage());
                            Toast.makeText(getContext(), "Lỗi khi lấy đơn hàng Delivered successfully!", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void showDeleteOrder(int idOrder) {
        // Hiển thị popup menu để xác nhận xóa
        View view = recyclerViewDeliveredSuccessfullyOrders.findViewById(R.id.statusOrder);
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
                                getDeliveredSuccessfullyOrders(); // Làm mới danh sách đơn hàng sau khi xóa
                            } else {
                                Toast.makeText(getContext(), "Xóa đơn hàng thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        },
                        throwable -> {
                            Log.e("DeliveredSuccessfullyFragment", "Lỗi xóa đơn hàng: " + throwable.getMessage());
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