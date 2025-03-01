package com.baconga.kttstore.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Models.MCategory;
import com.bumptech.glide.Glide;
import com.baconga.kttstore.R;
import java.util.List;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private Context context;
    private List<MCategory> categories;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MCategory category);
    }

    public CategoryAdapter(Context context, List<MCategory> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void updateData(List<MCategory> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        MCategory category = categories.get(position);
        
        // Load ảnh category với Glide, sử dụng getImageURL thay vì getThumbnail
        Glide.with(context)
            .load(category.getImageURL())
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .transform(new CenterCrop())
            .into(holder.ivCategory);

        holder.tvCategoryName.setText(category.getName());
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivCategory;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategory = itemView.findViewById(R.id.ivCategory);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}