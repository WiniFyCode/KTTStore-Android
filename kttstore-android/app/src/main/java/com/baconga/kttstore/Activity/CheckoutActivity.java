package com.baconga.kttstore.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.ActivityCheckoutBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {
    private ActivityCheckoutBinding binding;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private double totalAmount = 0;
    private String selectedVoucherCode = null;
    private double discountAmount = 0;
    private String defaultAddressId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);

        // Lấy tổng tiền từ intent
        totalAmount = getIntent().getDoubleExtra("totalAmount", 0);

        setupViews();
        setupListeners();
        updateSummary();
        loadDefaultAddress();
    }

    private void setupViews() {
        binding.txtSubtotal.setText(String.format("%,.0f₫", totalAmount));
        binding.txtDiscount.setText(String.format("-%,.0f₫", discountAmount));
        binding.txtTotal.setText(String.format("%,.0f₫", totalAmount - discountAmount));
        binding.txtFinalTotal.setText(String.format("%,.0f₫", totalAmount - discountAmount));

        // Hiển thị mã voucher nếu đã chọn
        if (selectedVoucherCode != null) {
            binding.btnSelectVoucher.setText(selectedVoucherCode);
            binding.btnSelectVoucher.setTextColor(Color.BLACK);
        } else {
            binding.btnSelectVoucher.setText("Chọn voucher");
            binding.btnSelectVoucher.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSelectVoucher.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoucherActivity.class);
            intent.putExtra("orderValue", totalAmount);
            startActivityForResult(intent, 100);
        });

        binding.btnCheckout.setOnClickListener(v -> processCheckout());

        binding.btnApplyVoucher.setOnClickListener(v -> {
            String code = binding.edtVoucherCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
                return;
            }
            applyVoucherByCode(code);
        });
    }

    private void processCheckout() {
        if (defaultAddressId == null) {
            showError("Vui lòng chọn địa chỉ giao hàng");
            return;
        }

        String url = SERVER.create_order;
        JSONObject params = new JSONObject();
        
        try {
            params.put("totalAmount", totalAmount);
            params.put("discountAmount", discountAmount);
            if (selectedVoucherCode != null) {
                params.put("voucherCode", selectedVoucherCode);
            }
            params.put("addressId", defaultAddressId);
            // Thêm các thông tin khác cần thiết
            
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
            response -> {
                try {
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Có lỗi xảy ra khi xử lý đơn hàng");
                }
            },
            error -> showError("Không thể tạo đơn hàng")
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void loadDefaultAddress() {
        String url = SERVER.get_addresses;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    // Tìm địa chỉ mặc định
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject address = response.getJSONObject(i);
                        if (address.getBoolean("isDefault")) {
                            defaultAddressId = String.valueOf(address.getInt("addressID"));
                            String fullAddress = address.getString("address");
                            binding.txtAddress.setText(fullAddress);
                            
                            // Thêm click listener để cho phép thay đổi địa chỉ
                            binding.addressSection.setOnClickListener(v -> {
                                Intent intent = new Intent(this, AddressListActivity.class);
                                startActivityForResult(intent, 200);
                            });
                            break;
                        }
                    }

                    if (defaultAddressId == null) {
                        binding.txtAddress.setText("Vui lòng thêm địa chỉ giao hàng");
                        binding.addressSection.setOnClickListener(v -> {
                            Intent intent = new Intent(this, AddressListActivity.class);
                            startActivityForResult(intent, 200);
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    showError("Có lỗi xảy ra khi tải địa chỉ");
                }
            },
            error -> showError("Không thể tải địa chỉ")
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void applyVoucherByCode(String code) {
        String url = SERVER.use_user_coupon;
        JSONObject params = new JSONObject();
        try {
            params.put("code", code);
            params.put("orderValue", totalAmount);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
            response -> {
                try {
                    // Kiểm tra điều kiện sử dụng voucher
                    JSONObject couponInfo = response.getJSONObject("couponInfo");
                    
                    double minOrderValue = couponInfo.getDouble("minOrderValue");
                    if (totalAmount < minOrderValue) {
                        showError(String.format("Đơn hàng tối thiểu %,.0f₫ để sử dụng voucher này", minOrderValue));
                        return;
                    }

                    // Kiểm tra số lượt sử dụng còn lại
                    int usageLeft = response.getInt("usageLeft");
                    if (usageLeft <= 0) {
                        showError("Voucher đã hết lượt sử dụng");
                        return;
                    }

                    // Kiểm tra thời hạn sử dụng
                    String endDate = couponInfo.getString("endDate");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    Date expiryDate = dateFormat.parse(endDate);
                    if (new Date().after(expiryDate)) {
                        showError("Voucher đã hết hạn sử dụng");
                        return;
                    }

                    // Tính toán số tiền giảm giá
                    double discountValue = couponInfo.getDouble("discountValue");
                    double maxDiscountAmount = couponInfo.getDouble("maxDiscountAmount");
                    
                    if (couponInfo.getString("discountType").equals("percentage")) {
                        discountAmount = Math.min(
                            totalAmount * (discountValue / 100),
                            maxDiscountAmount
                        );
                    } else {
                        discountAmount = Math.min(discountValue, maxDiscountAmount);
                    }

                    // Nếu tất cả điều kiện đều thỏa mãn
                    selectedVoucherCode = code;
                    updateSummary();
                    Toast.makeText(this, 
                        String.format("Áp dụng voucher thành công: -%,.0f₫", discountAmount), 
                        Toast.LENGTH_SHORT).show();
                    binding.edtVoucherCode.setText("");

                    // Cập nhật UI hiển thị voucher đã chọn
                    binding.btnSelectVoucher.setText(code);
                    binding.btnSelectVoucher.setTextColor(Color.BLACK);

                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                    showError("Có lỗi xảy ra khi áp dụng voucher");
                }
            },
            error -> {
                try {
                    String errorMessage;
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        String errorResponse = new String(error.networkResponse.data, "UTF-8");
                        JSONObject errorJson = new JSONObject(errorResponse);
                        errorMessage = errorJson.getString("message");
                    } else {
                        errorMessage = "Không thể áp dụng voucher";
                    }
                    showError(errorMessage);
                } catch (Exception e) {
                    showError("Có lỗi xảy ra");
                }
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Xử lý kết quả từ VoucherActivity
            selectedVoucherCode = data.getStringExtra("voucherCode");
            discountAmount = data.getDoubleExtra("discountAmount", 0);
            updateSummary();
            Toast.makeText(this, 
                String.format("Áp dụng voucher thành công: -%,.0f₫", discountAmount), 
                Toast.LENGTH_SHORT).show();
        } else if (requestCode == 200 && resultCode == RESULT_OK) {
            // Reload địa chỉ sau khi chọn/thêm địa chỉ mới
            loadDefaultAddress();
        }
    }

    private void updateSummary() {
        binding.txtSubtotal.setText(String.format("%,.0f₫", totalAmount));
        binding.txtDiscount.setText(String.format("-%,.0f₫", discountAmount));
        double finalTotal = totalAmount - discountAmount;
        binding.txtTotal.setText(String.format("%,.0f₫", finalTotal));
        binding.txtFinalTotal.setText(String.format("%,.0f₫", finalTotal));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String getToken() {
        return sharedPreferences.getString("token", "");
    }
} 