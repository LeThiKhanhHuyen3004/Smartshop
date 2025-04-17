package com.example.app_technical_admin.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.app_technical_admin.activity.fragment.AcceptedFragment;
import com.example.app_technical_admin.activity.fragment.AllOrdersFragment;
import com.example.app_technical_admin.activity.fragment.CanceledFragment;
import com.example.app_technical_admin.activity.fragment.DeliveredSuccessfullyFragment;
import com.example.app_technical_admin.activity.fragment.DeliveringFragment;
import com.example.app_technical_admin.activity.fragment.PendingFragment;

public class OrderPagerAdapter extends FragmentStateAdapter {

    public OrderPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AllOrdersFragment();
            case 1:
                return new PendingFragment();
            case 2:
                return new AcceptedFragment();
            case 3:
                return new DeliveringFragment();
            case 4:
                return new DeliveredSuccessfullyFragment();
            case 5:
                return new CanceledFragment();
            default:
                return new AllOrdersFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 6; // Số lượng tab
    }
}