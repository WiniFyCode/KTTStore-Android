package com.baconga.kttstore.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Models.Address;
import com.baconga.kttstore.R;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
    private List<Address> addresses;
    private OnAddressClickListener listener;

    public AddressAdapter(List<Address> addresses, OnAddressClickListener listener) {
        this.addresses = addresses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Address address = addresses.get(position);
        
        // Hiển thị địa chỉ
        holder.txtAddress.setText(address.getAddress());
        
        // Hiển thị/ẩn tag mặc định và nút đặt mặc định
        holder.txtDefault.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
        holder.btnSetDefault.setVisibility(address.isDefault() ? View.GONE : View.VISIBLE);
        
        // Set up listeners
        holder.btnSetDefault.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefaultClick(address);
            }
        });
        
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(address);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addresses != null ? addresses.size() : 0;
    }

    public void updateData(List<Address> newAddresses) {
        this.addresses = newAddresses;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAddress;
        TextView txtDefault;
        TextView btnSetDefault;
        TextView btnEdit;
        TextView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtDefault = itemView.findViewById(R.id.txtDefault);
            btnSetDefault = itemView.findViewById(R.id.btnSetDefault);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public interface OnAddressClickListener {
        void onSetDefaultClick(Address address);
        void onEditClick(Address address);
        void onDeleteClick(Address address);
    }
} 