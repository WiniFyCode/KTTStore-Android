package com.baconga.kttstore.Adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.CheckBox;

import com.baconga.kttstore.Models.MCart;
import com.bumptech.glide.Glide;
import com.baconga.kttstore.Models.CartItem;
import com.baconga.kttstore.R;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.graphics.Color;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    public interface CartItemListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onItemDeleted(CartItem item);
        void onItemSelected(CartItem item, boolean isSelected);
    }

    private final CartItemListener listener;
    private final Context context;
    private List<CartItem> items;

    public CartAdapter(CartItemListener listener, Context context) {
        this.listener = listener;
        this.context = context;
        this.items = new ArrayList<>();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void updateItemQuantity(String cartId, int newQuantity) {
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            if (item.getCart().getCartID().equals(cartId)) {
                item.getCart().setQuantity(newQuantity);
                notifyItemChanged(i);
                break;
            }
        }
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgProduct;
        private TextView txtProductName, txtColor, txtSize;
        private TextView txtPrice, txtOriginalPrice;
        private TextView txtQuantity;
        private ImageButton btnDelete;
        private TextView btnIncrease, btnDecrease;
        private CheckBox checkboxSelect;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ views
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtColor = itemView.findViewById(R.id.txtColor);
            txtSize = itemView.findViewById(R.id.txtSize);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtOriginalPrice = itemView.findViewById(R.id.txtOriginalPrice);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
        }

        void bind(CartItem item) {
            try {
                MCart cart = item.getCart();
                
                // Hiển thị thông tin sản phẩm
                txtProductName.setText(item.getProductName());
                txtColor.setText("Màu: " + item.getColorName());
                txtSize.setText("Size: " + cart.getSize());
                
                // Hiển thị giá theo điều kiện promotion
                if (item.getDiscountPrice() != null) {
                    // Có khuyến mãi: Hiển thị cả giá gốc và giá giảm
                    txtOriginalPrice.setText(String.format("%,.0f₫", item.getOriginalPrice()));
                    txtOriginalPrice.setPaintFlags(txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    txtOriginalPrice.setVisibility(View.VISIBLE);
                    
                    txtPrice.setText(String.format("%,.0f₫", item.getFinalPrice()));
                    txtPrice.setTextColor(Color.RED);
                } else {
                    txtOriginalPrice.setVisibility(View.GONE);
                    txtPrice.setText(String.format("%,.0f₫", item.getOriginalPrice()));
                    txtPrice.setTextColor(Color.BLACK);
                }
                
                txtQuantity.setText(String.valueOf(cart.getQuantity()));

                // Load ảnh với Glide
                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.ic_home)
                            .error(R.drawable.ic_home)
                            .into(imgProduct);
                } else {
                    imgProduct.setImageResource(R.drawable.ic_home);
                }

                // Xử lý sự kiện
                btnIncrease.setOnClickListener(v -> {
                    int newQty = cart.getQuantity() + 1;
                    if (newQty <= item.getStock()) {
                        listener.onQuantityChanged(item, newQty);
                    } else {
                        Toast.makeText(v.getContext(), "Đã đạt số lượng tối đa", Toast.LENGTH_SHORT).show();
                    }
                });

                btnDecrease.setOnClickListener(v -> {
                    int newQty = cart.getQuantity() - 1;
                    if (newQty >= 1) {
                        listener.onQuantityChanged(item, newQty);
                    }
                });

                btnDelete.setOnClickListener(v -> {
                    listener.onItemDeleted(item);
                });

                checkboxSelect.setChecked(item.isSelected());

                // Thêm listener cho checkbox
                checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        item.setSelected(isChecked);
                        listener.onItemSelected(item, isChecked);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Thêm method để chọn/bỏ chọn tất cả
    public void selectAll(boolean select) {
        for (CartItem item : items) {
            item.setSelected(select);
        }
        notifyDataSetChanged();
    }

    // Lấy danh sách item đã chọn
    public List<CartItem> getSelectedItems() {
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : items) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }
} 