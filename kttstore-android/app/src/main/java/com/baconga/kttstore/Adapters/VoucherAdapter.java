package com.baconga.kttstore.Adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baconga.kttstore.Models.Voucher;
import com.baconga.kttstore.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private List<Voucher> vouchers;
    private OnVoucherClickListener listener;

    public interface OnVoucherClickListener {
        void onVoucherClick(Voucher voucher);
    }

    public VoucherAdapter(List<Voucher> vouchers, OnVoucherClickListener listener) {
        this.vouchers = vouchers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        if (vouchers != null && position < vouchers.size()) {
            Voucher voucher = vouchers.get(position);
            holder.bind(voucher);
        }
    }

    @Override
    public int getItemCount() {
        return vouchers != null ? vouchers.size() : 0;
    }

    public void updateData(List<Voucher> newVouchers) {
        this.vouchers = newVouchers;
        notifyDataSetChanged();
    }

    class VoucherViewHolder extends RecyclerView.ViewHolder {
        private TextView txtDiscountValue;
        private TextView txtDiscountType;
        private TextView txtDescription;
        private TextView txtMinOrderValue;
        private TextView txtMaxDiscount;
        private TextView txtExpiry;
        private TextView txtUsageLimit;
        private TextView txtCode;
        private TextView btnCopy;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDiscountValue = itemView.findViewById(R.id.txtDiscountValue);
            txtDiscountType = itemView.findViewById(R.id.txtDiscountType);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtMinOrderValue = itemView.findViewById(R.id.txtMinOrderValue);
            txtMaxDiscount = itemView.findViewById(R.id.txtMaxDiscount);
            txtExpiry = itemView.findViewById(R.id.txtExpiry);
            txtUsageLimit = itemView.findViewById(R.id.txtUsageLimit);
            txtCode = itemView.findViewById(R.id.txtCode);
            btnCopy = itemView.findViewById(R.id.btnCopy);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onVoucherClick(vouchers.get(position));
                }
            });
        }

        public void bind(Voucher voucher) {
            // Hiển thị giá trị giảm giá
            if (voucher.getDiscountType().equals("percentage")) {
                txtDiscountValue.setText(voucher.getDiscountValue() + "%");
                txtDiscountType.setText("GIẢM GIÁ");
            } else {
                // Format số tiền theo định dạng VND
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                String formattedAmount = formatter.format(voucher.getDiscountValue())
                        .replace("₫", "")
                        .trim();
                txtDiscountValue.setText(formattedAmount + "đ");
                txtDiscountType.setText("GIẢM TRỰC TIẾP");
            }

            // Hiển thị mô tả
            txtDescription.setText(voucher.getDescription());

            // Hiển thị giá trị đơn hàng tối thiểu
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String minOrder = formatter.format(voucher.getMinOrderValue())
                    .replace("₫", "")
                    .trim();
            txtMinOrderValue.setText("Đơn tối thiểu " + minOrder + "đ");

            // Hiển thị giảm tối đa
            String maxDiscount = formatter.format(voucher.getMaxDiscountAmount())
                    .replace("₫", "")
                    .trim();
            txtMaxDiscount.setText("Giảm tối đa " + maxDiscount + "đ");

            // Hiển thị thời gian hết hạn theo giờ VN
            txtExpiry.setText("HSD: " + voucher.getFormattedExpiry());

            // Hiển thị thời gian bắt đầu theo giờ VN (nếu cần)
//             txtStartDate.setText("Bắt đầu: " + voucher.getFormattedStartDate());

            // Hiển thị số lần sử dụng còn lại
            txtUsageLimit.setText(String.format("Còn %d lượt sử dụng", voucher.getUsageLeft()));

            // Hiển thị mã voucher
            txtCode.setText("Mã: " + voucher.getCode());

            // Xử lý sự kiện sao chép
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager)
                    itemView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Voucher Code", voucher.getCode());
                clipboard.setPrimaryClip(clip);

                // Hiển thị thông báo
                Toast.makeText(itemView.getContext(),
                    "Đã sao chép mã: " + voucher.getCode(),
                    Toast.LENGTH_SHORT).show();
            });
        }
    }
}