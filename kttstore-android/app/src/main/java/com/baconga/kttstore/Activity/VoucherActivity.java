package com.baconga.kttstore.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.VoucherAdapter;
import com.baconga.kttstore.Adapters.VoucherPagerAdapter;
import com.baconga.kttstore.Models.Voucher;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class VoucherActivity extends AppCompatActivity implements VoucherAdapter.OnVoucherClickListener {

    private static final String TAG = "VoucherActivity";
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ImageButton btnBack;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private List<Voucher> allVouchers = new ArrayList<>();
    private List<Voucher> usableVouchers = new ArrayList<>();
    private List<Voucher> usedVouchers = new ArrayList<>();
    private List<Voucher> expiredVouchers = new ArrayList<>();
    private VoucherPagerAdapter pagerAdapter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher);

        // Khởi tạo
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);

        // Ánh xạ views
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnBack = findViewById(R.id.btnBack);

        // Setup adapters
        setupAdapters();

        // Setup ViewPager
        setupViewPager();

        // Setup listeners
        setupListeners();

        // Load dữ liệu
        loadVouchers();
    }

    private void setupAdapters() {
        // Tạo pager adapter
        pagerAdapter = new VoucherPagerAdapter(this, usableVouchers, usedVouchers, expiredVouchers, this);
    }

    private void setupViewPager() {
        // Tạo pager adapter
        pagerAdapter = new VoucherPagerAdapter(this, usableVouchers, usedVouchers, expiredVouchers, this);
        viewPager.setAdapter(pagerAdapter);

        // Setup tab layout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Có thể dùng");
                    break;
                case 1:
                    tab.setText("Đã sử dụng");
                    break;
                case 2:
                    tab.setText("Hết hạn");
                    break;
            }
        }).attach();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadVouchers() {
        Log.d(TAG, "Bắt đầu tải danh sách voucher");
        String url = SERVER.get_user_coupons;
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    // Lấy mảng userCoupons từ response object
                    JSONArray userCouponsArray = response.getJSONArray("userCoupons");
                    Log.d(TAG, "Tải voucher thành công, số lượng: " + userCouponsArray.length());
                    parseVouchers(userCouponsArray);
                    categorizeVouchers();
                    updateAdapters();
                } catch (JSONException | ParseException e) {
                    Log.e(TAG, "Lỗi khi xử lý dữ liệu voucher: " + e.getMessage());
                    showError("Có lỗi xảy ra khi tải danh sách voucher");
                }
            },
            error -> {
                Log.e(TAG, "Lỗi khi tải voucher: " + error.getMessage());
                showError("Không thể tải danh sách voucher");
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void parseVouchers(JSONArray userCouponsArray) throws JSONException, ParseException {
        allVouchers.clear();
        
        // Định dạng thời gian đầu vào từ server (UTC)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (int i = 0; i < userCouponsArray.length(); i++) {
            JSONObject userCoupon = userCouponsArray.getJSONObject(i);
            JSONObject couponInfo = userCoupon.getJSONObject("couponInfo");
            
            // Parse thời gian từ UTC sang local time
            Date startDate = inputFormat.parse(couponInfo.getString("startDate"));
            Date endDate = inputFormat.parse(couponInfo.getString("endDate"));
            
            Voucher voucher = new Voucher(
                couponInfo.getString("code"),
                couponInfo.getString("description"),
                couponInfo.getString("discountType"),
                couponInfo.getDouble("discountValue"),
                couponInfo.getDouble("minOrderValue"),
                couponInfo.getDouble("maxDiscountAmount"),
                startDate,
                endDate,
                userCoupon.getInt("usageLeft"),
                couponInfo.getBoolean("isActive")
            );
            
            allVouchers.add(voucher);
        }
        Log.d(TAG, "Parse voucher hoàn tất, số lượng: " + allVouchers.size());
    }

    private void categorizeVouchers() {
        usableVouchers.clear();
        usedVouchers.clear();
        expiredVouchers.clear();

        for (Voucher voucher : allVouchers) {
            if (voucher.isUsable()) {
                usableVouchers.add(voucher);
            } else if (voucher.isExpired()) {
                expiredVouchers.add(voucher);
            } else {
                usedVouchers.add(voucher);
            }
        }

        Log.d(TAG, String.format("Phân loại voucher: Có thể dùng=%d, Đã dùng=%d, Hết hạn=%d",
            usableVouchers.size(), usedVouchers.size(), expiredVouchers.size()));
    }

    private void updateAdapters() {
        runOnUiThread(() -> {
            Log.d(TAG, "Cập nhật adapter với dữ liệu mới");
            pagerAdapter.updateData(usableVouchers, usedVouchers, expiredVouchers);
        });
    }

    @Override
    public void onVoucherClick(Voucher voucher) {
        if (voucher.isUsable()) {
            // Lấy giá trị đơn hàng
            double orderValue = getIntent().getDoubleExtra("orderValue", 0);
            
            // Kiểm tra điều kiện đơn tối thiểu
            if (orderValue < voucher.getMinOrderValue()) {
                Toast.makeText(this, 
                    String.format("Đơn hàng tối thiểu %,.0f₫ để sử dụng voucher này", 
                    voucher.getMinOrderValue()), 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            // Tính toán số tiền giảm giá
            double discountAmount;
            if (voucher.getDiscountType().equals("percentage")) {
                discountAmount = Math.min(
                    orderValue * (voucher.getDiscountValue() / 100),
                    voucher.getMaxDiscountAmount()
                );
            } else {
                discountAmount = Math.min(
                    voucher.getDiscountValue(),
                    voucher.getMaxDiscountAmount()
                );
            }

            // Trả về kết quả cho CheckoutActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("voucherCode", voucher.getCode());
            resultIntent.putExtra("discountAmount", discountAmount);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Voucher không khả dụng", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 