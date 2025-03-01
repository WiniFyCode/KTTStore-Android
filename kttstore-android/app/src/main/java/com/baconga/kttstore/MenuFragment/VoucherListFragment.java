package com.baconga.kttstore.MenuFragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Adapters.VoucherAdapter;
import com.baconga.kttstore.Models.Voucher;
import com.baconga.kttstore.R;

import java.util.ArrayList;
import java.util.List;

public class VoucherListFragment extends Fragment {
    private static final String TAG = "VoucherListFragment";
    private static final String ARG_VOUCHERS = "vouchers";
    
    private List<Voucher> vouchers = new ArrayList<>();
    private VoucherAdapter.OnVoucherClickListener listener;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private VoucherAdapter adapter;

    public static VoucherListFragment newInstance(List<Voucher> vouchers, 
                                                VoucherAdapter.OnVoucherClickListener listener) {
        VoucherListFragment fragment = new VoucherListFragment();
        fragment.vouchers = vouchers;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voucher_list, container, false);

        // Ánh xạ views
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);

        // Setup RecyclerView với LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Khởi tạo adapter với danh sách voucher
        adapter = new VoucherAdapter(vouchers != null ? vouchers : new ArrayList<>(), listener);
        recyclerView.setAdapter(adapter);

        // Kiểm tra và hiển thị trạng thái trống
        updateEmptyState();

        return view;
    }

    private void updateEmptyState() {
        if (vouchers == null || vouchers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    public void updateVouchers(List<Voucher> newVouchers) {
        this.vouchers = newVouchers;
        if (adapter != null) {
            adapter.updateData(newVouchers);
            updateEmptyState();
            
            // Log trạng thái voucher
            if (newVouchers != null) {
                int usable = 0, expired = 0;
                for (Voucher voucher : newVouchers) {
                    if (voucher.isUsable()) usable++;
                    if (voucher.isExpired()) expired++;
                }
                Log.d(TAG, String.format("Cập nhật danh sách voucher: Tổng=%d, Có thể dùng=%d, Hết hạn=%d",
                    newVouchers.size(), usable, expired));
            }
        }
    }
} 