package com.baconga.kttstore.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.baconga.kttstore.MenuFragment.VoucherListFragment;
import com.baconga.kttstore.Models.Voucher;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class VoucherPagerAdapter extends FragmentStateAdapter {
    private List<Voucher> usableVouchers;
    private List<Voucher> usedVouchers;
    private List<Voucher> expiredVouchers;
    private VoucherAdapter.OnVoucherClickListener listener;

    private Map<Integer, VoucherListFragment> fragments = new HashMap<>();

    public VoucherPagerAdapter(FragmentActivity activity,
                              List<Voucher> usableVouchers,
                              List<Voucher> usedVouchers,
                              List<Voucher> expiredVouchers,
                              VoucherAdapter.OnVoucherClickListener listener) {
        super(activity);
        this.usableVouchers = usableVouchers;
        this.usedVouchers = usedVouchers;
        this.expiredVouchers = expiredVouchers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        VoucherListFragment fragment;
        switch (position) {
            case 0:
                fragment = VoucherListFragment.newInstance(usableVouchers, listener);
                break;
            case 1:
                fragment = VoucherListFragment.newInstance(usedVouchers, listener);
                break;
            case 2:
                fragment = VoucherListFragment.newInstance(expiredVouchers, listener);
                break;
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
        fragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public void updateData(List<Voucher> usableVouchers,
                         List<Voucher> usedVouchers,
                         List<Voucher> expiredVouchers) {
        this.usableVouchers = usableVouchers;
        this.usedVouchers = usedVouchers;
        this.expiredVouchers = expiredVouchers;

        // Cập nhật từng fragment
        for (Map.Entry<Integer, VoucherListFragment> entry : fragments.entrySet()) {
            VoucherListFragment fragment = entry.getValue();
            if (fragment != null) {
                switch (entry.getKey()) {
                    case 0:
                        fragment.updateVouchers(usableVouchers);
                        break;
                    case 1:
                        fragment.updateVouchers(usedVouchers);
                        break;
                    case 2:
                        fragment.updateVouchers(expiredVouchers);
                        break;
                }
            }
        }
    }
} 