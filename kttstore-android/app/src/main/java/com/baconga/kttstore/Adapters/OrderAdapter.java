package com.baconga.kttstore.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.R;
import com.baconga.kttstore.Models.MOrder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    
    private Context context;
    private List<MOrder> orderList;
    private boolean isLoading = false;

    // Thêm interface cho sự kiện click
    public interface OnOrderClickListener {
        void onOrderClick(String orderId);
    }

    private OnOrderClickListener listener;

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public OrderAdapter(Context context, List<MOrder> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OrderViewHolder) {
            MOrder order = orderList.get(position);
            OrderViewHolder orderHolder = (OrderViewHolder) holder;
            
            orderHolder.orderIdTextView.setText("Đơn hàng #" + order.getOrderID());
            orderHolder.orderDateTextView.setText(formatDate(order.getCreatedAt()));
            orderHolder.fullnameTextView.setText("Người nhận: " + order.getFullname());
            orderHolder.phoneTextView.setText("SĐT: " + order.getPhone());
            orderHolder.addressTextView.setText("Địa chỉ: " + order.getAddress());
            orderHolder.totalPriceTextView.setText(String.format(Locale.US, "Tổng tiền: %,.0f₫", order.getPaymentPrice()));
            orderHolder.statusTextView.setText("Trạng thái đơn hàng: " + getOrderStatus(order.getOrderStatus()));
            orderHolder.shippingStatusTextView.setText("Trạng thái giao hàng: " + getShippingStatus(order.getShippingStatus()));
            orderHolder.paymentStatusTextView.setText("Thanh toán: " + (order.isPayed() ? "Đã thanh toán" : "Chưa thanh toán"));
            orderHolder.paymentStatusTextView.setTextColor(order.isPayed() ? Color.GREEN : Color.RED);

            // Thêm sự kiện click
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(String.valueOf(order.getOrderID()));
                }
            });
        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingHolder = (LoadingViewHolder) holder;
            loadingHolder.progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return orderList == null ? 0 : orderList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return orderList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            orderList.add(null);
            notifyItemInserted(orderList.size() - 1);
        } else {
            int loadingIndex = orderList.indexOf(null);
            if (loadingIndex != -1) {
                orderList.remove(loadingIndex);
                notifyItemRemoved(loadingIndex);
            }
        }
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdTextView, orderDateTextView, fullnameTextView, phoneTextView, 
                addressTextView, totalPriceTextView, statusTextView, 
                shippingStatusTextView, paymentStatusTextView;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdTextView = itemView.findViewById(R.id.orderIdTextView);
            orderDateTextView = itemView.findViewById(R.id.orderDateTextView);
            fullnameTextView = itemView.findViewById(R.id.fullnameTextView);
            phoneTextView = itemView.findViewById(R.id.phoneTextView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            totalPriceTextView = itemView.findViewById(R.id.totalPriceTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            shippingStatusTextView = itemView.findViewById(R.id.shippingStatusTextView);
            paymentStatusTextView = itemView.findViewById(R.id.paymentStatusTextView);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getOrderStatus(String status) {
        switch (status) {
            case "pending": return "Đang xử lý";
            case "confirmed": return "Đã xác nhận";
            case "shipped": return "Đã giao hàng";
            case "cancelled": return "Đã hủy";
            default: return "Không xác định";
        }
    }

    private String getShippingStatus(String status) {
        switch (status) {
            case "preparing": return "Đang chuẩn bị";
            case "shipped": return "Đang giao hàng";
            case "delivered": return "Đã nhận hàng";
            default: return "Không xác định";
        }
    }
}
