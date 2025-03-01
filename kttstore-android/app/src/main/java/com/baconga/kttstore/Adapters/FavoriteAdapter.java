package com.baconga.kttstore.Adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Models.FavoriteItem;
import com.baconga.kttstore.R;
import com.baconga.kttstore.databinding.ItemFavoriteBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    private List<FavoriteItem> items = new ArrayList<>();
    private final FavoriteItemListener listener;

    public interface FavoriteItemListener {
        void onRemoveClicked(int favoriteId);
        void onNoteChanged(int favoriteId, String note);
        void onItemClicked(FavoriteItem item);
    }

    public FavoriteAdapter(FavoriteItemListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFavoriteBinding binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        );
        return new FavoriteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<FavoriteItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavoriteBinding binding;
        private String currentNote;

        FavoriteViewHolder(ItemFavoriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FavoriteItem item) {
            try {
                // Xử lý ảnh sản phẩm
                if (item.getProduct().getThumbnail() != null && !item.getProduct().getThumbnail().isEmpty()) {
                    Glide.with(binding.getRoot().getContext())
                        .load(item.getProduct().getThumbnail())
                        .placeholder(R.drawable.ic_home)
                        .error(R.drawable.ic_home)
                        .into(binding.imgProduct);
                } else {
                    binding.imgProduct.setImageResource(R.drawable.ic_home);
                }

                // Thiết lập thông tin cơ bản
                binding.txtProductName.setText(item.getProduct().getName());
                
                // Xử lý giá và giá khuyến mãi
                double originalPrice = item.getProduct().getPrice();
                double finalPrice = item.getProduct().getFinalPrice();
                
                // Hiển thị giá theo điều kiện promotion
                if (item.getProduct().hasPromotion()) {
                    // Có khuyến mãi: Hiển thị cả giá gốc và giá giảm
                    binding.txtOriginalPrice.setText(String.format("%,.0f₫", originalPrice));
                    binding.txtOriginalPrice.setPaintFlags(binding.txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    binding.txtOriginalPrice.setVisibility(View.VISIBLE);
                    
                    binding.txtFinalPrice.setText(String.format("%,.0f₫", finalPrice));
                    binding.txtFinalPrice.setTextColor(Color.RED);
                } else {
                    // Không có khuyến mãi
                    binding.txtOriginalPrice.setVisibility(View.GONE);
                    binding.txtFinalPrice.setText(String.format("%,.0f₫", originalPrice));
                    binding.txtFinalPrice.setTextColor(Color.BLACK);
                }

                // Lưu note hiện tại
                currentNote = item.getNote();
                binding.edtNote.setText(currentNote);

                // Xử lý nút lưu ghi chú
                binding.btnSaveNote.setOnClickListener(v -> {
                    String newNote = binding.edtNote.getText().toString();
                    if (!newNote.equals(currentNote)) {
                        if (listener != null) {
                            listener.onNoteChanged(item.getFavoriteID(), newNote);
                        }
                        currentNote = newNote;
                    }
                });

                // Xử lý các sự kiện click
                binding.btnRemove.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveClicked(item.getFavoriteID());
                    }
                });

                binding.getRoot().setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClicked(item);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 