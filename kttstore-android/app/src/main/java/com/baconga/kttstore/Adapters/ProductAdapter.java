package com.baconga.kttstore.Adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Models.MProduct;
import com.baconga.kttstore.R;
import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private Context context;
    private List<MProduct> products;
    private OnItemClickListener listener;
    private OnProductClickListener productClickListener;
    private boolean isGridLayout;

    public ProductAdapter(Context context, List<MProduct> products, OnProductClickListener productClickListener) {
        this.context = context;
        this.products = products;
        this.productClickListener = productClickListener;
    }

    public void setGridLayout(boolean isGridLayout) {
        this.isGridLayout = isGridLayout;
        notifyDataSetChanged();
    }

    public void updateData(List<MProduct> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridLayout ? R.layout.item_product_grid : R.layout.item_product_horizontal;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        MProduct product = products.get(position);
        
        // Log thông tin giá của sản phẩm
        Log.d("ProductAdapter", "Product: " + product.getName());
        Log.d("ProductAdapter", "Original Price: " + product.getPrice());
        Log.d("ProductAdapter", "Discounted Price: " + product.getDiscountedPrice());

        // Load ảnh sản phẩm
        Glide.with(context)
            .load(product.getThumbnail())
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.ivProduct);

        // Hiển thị tên sản phẩm
        holder.tvName.setText(product.getName());
        
        // Format giá theo định dạng tiền Việt Nam
        DecimalFormat formatter = new DecimalFormat("#,###");
        
        // Xử lý hiển thị giá gốc và giá khuyến mãi
        if (product.getDiscountedPrice() > 0) {  // Kiểm tra có giá khuyến mãi không
            // Có khuyến mãi
            holder.tvPromotionPrice.setText(formatter.format(product.getDiscountedPrice()) + "đ");
            holder.tvPrice.setText(formatter.format(product.getPrice()) + "đ");
            holder.tvPrice.setPaintFlags(holder.tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvPrice.setVisibility(View.VISIBLE);
            
            // Hiển thị phần trăm giảm giá
            holder.tvDiscountPercent.setVisibility(View.VISIBLE);
            holder.tvDiscountPercent.setText("-" + product.getDiscountPercent() + "%");
        } else {
            // Không có khuyến mãi
            holder.tvPromotionPrice.setText(formatter.format(product.getPrice()) + "đ");
            holder.tvPrice.setVisibility(View.GONE);
            holder.tvDiscountPercent.setVisibility(View.GONE);
        }

        // Hiển thị trạng thái tồn kho
        if (product.getInStock()) {
            holder.tvStatus.setText("Còn hàng");
            holder.tvStatus.setTextColor(context.getColor(R.color.Green));
        } else {
            holder.tvStatus.setText("Hết hàng");
            holder.tvStatus.setTextColor(context.getColor(R.color.Red));
        }

        // Hiển thị categoryID
        holder.tvCategory.setText(product.getCategoryID());

        holder.itemView.setOnClickListener(v -> {
            if (productClickListener != null) {
                productClickListener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(MProduct product);
    }

    public interface OnProductClickListener {
        void onProductClick(MProduct product);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvPromotionPrice, tvStatus, tvCategory, tvDiscountPercent;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvPromotionPrice = itemView.findViewById(R.id.tvPromotionPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDiscountPercent = itemView.findViewById(R.id.tvDiscountPercent);
        }
    }
} 