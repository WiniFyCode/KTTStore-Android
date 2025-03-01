package com.baconga.kttstore.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Models.MOrderDetail;
import com.baconga.kttstore.R;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.ViewHolder> {
    private Context context;
    private List<MOrderDetail> orderDetails;

    public OrderDetailAdapter(Context context, List<MOrderDetail> orderDetails) {
        this.context = context;
        this.orderDetails = orderDetails;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MOrderDetail orderDetail = orderDetails.get(position);
        MOrderDetail.MProduct product = orderDetail.getProduct();

        // Hiển thị thông tin sản phẩm
        holder.tvProductName.setText(product.getName());
        holder.tvColorAndSize.setText(String.format("Màu: %s - Size: %s", 
            product.getColorName(), orderDetail.getSize()));
        holder.tvPrice.setText(String.format(Locale.US, "%,.0f₫", product.getPrice()));
        holder.tvQuantity.setText(String.format("x%d", orderDetail.getQuantity()));

        // Load ảnh sản phẩm
        Glide.with(context)
            .load(product.getImage())
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.imgProduct);
    }

    @Override
    public int getItemCount() {
        return orderDetails.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvProductName, tvColorAndSize, tvPrice, tvQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvColorAndSize = itemView.findViewById(R.id.tvColorAndSize);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
        }
    }
} 