package com.baconga.kttstore.MenuFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.OrderDetailAdapter;
import com.baconga.kttstore.Models.MOrderDetail;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailFragment extends Fragment {
    private String orderId;
    private RequestQueue requestQueue;
    private RecyclerView rvOrderDetails;
    private OrderDetailAdapter adapter;
    private List<MOrderDetail> orderDetails;

    // Views
    private TextView tvOrderId, tvOrderDate, tvOrderStatus, tvShippingStatus, tvPaymentStatus;
    private TextView tvReceiverName, tvReceiverPhone, tvReceiverAddress;
    private TextView tvTotalProductPrice, tvTotalPrice, tvDiscount, tvFinalPrice;
    private ImageView btnBack;

    public OrderDetailFragment(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(requireContext());
        orderDetails = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_detail, container, false);
        initViews(view);
        setupRecyclerView();
        loadOrderDetails();
        return view;
    }

    private void initViews(View view) {
        // Khởi tạo các view
        tvOrderId = view.findViewById(R.id.tvOrderId);
        tvOrderDate = view.findViewById(R.id.tvOrderDate);
        tvOrderStatus = view.findViewById(R.id.tvOrderStatus);
        tvShippingStatus = view.findViewById(R.id.tvShippingStatus);
        tvPaymentStatus = view.findViewById(R.id.tvPaymentStatus);
        tvReceiverName = view.findViewById(R.id.tvReceiverName);
        tvReceiverPhone = view.findViewById(R.id.tvReceiverPhone);
        tvReceiverAddress = view.findViewById(R.id.tvReceiverAddress);
        tvTotalProductPrice = view.findViewById(R.id.tvTotalProductPrice);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        tvDiscount = view.findViewById(R.id.tvDiscount);
        tvFinalPrice = view.findViewById(R.id.tvFinalPrice);
        rvOrderDetails = view.findViewById(R.id.rvOrderDetails);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new OrderDetailAdapter(requireContext(), orderDetails);
        rvOrderDetails.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrderDetails.setAdapter(adapter);
    }

    private void loadOrderDetails() {
        String url = SERVER.get_order_by_id.replace(":orderId", orderId);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    // Parse thông tin đơn hàng
                    tvOrderId.setText("Chi tiết đơn hàng #" + response.getInt("orderID"));
                    tvOrderDate.setText("Ngày đặt: " + formatDate(response.getString("createdAt")));
                    tvOrderStatus.setText("Trạng thái đơn hàng: " + getOrderStatus(response.getString("orderStatus")));
                    tvShippingStatus.setText("Trạng thái giao hàng: " + getShippingStatus(response.getString("shippingStatus")));
                    tvPaymentStatus.setText("Thanh toán: " + (response.getBoolean("isPayed") ? "Đã thanh toán" : "Chưa thanh toán"));

                    // Parse thông tin người nhận
                    tvReceiverName.setText("Họ tên: " + response.getString("fullname"));
                    tvReceiverPhone.setText("Số điện thoại: " + response.getString("phone"));
                    tvReceiverAddress.setText("Địa chỉ: " + response.getString("address"));

                    // Parse thông tin thanh toán
                    double totalProductPrice = response.getDouble("totalProductPrice");
                    double totalPrice = response.getDouble("totalPrice");
                    double paymentPrice = response.getDouble("paymentPrice");
                    double discount = totalPrice - paymentPrice;

                    tvTotalProductPrice.setText(String.format(Locale.US, "%,.0f₫", totalProductPrice));
                    tvTotalPrice.setText(String.format(Locale.US, "%,.0f₫", totalPrice));
                    tvDiscount.setText(String.format(Locale.US, "-%,.0f₫", discount));
                    tvFinalPrice.setText(String.format(Locale.US, "%,.0f₫", paymentPrice));

                    // Parse chi tiết đơn hàng
                    JSONArray detailsArray = response.getJSONArray("orderDetails");
                    orderDetails.clear();
                    for (int i = 0; i < detailsArray.length(); i++) {
                        JSONObject detailObj = detailsArray.getJSONObject(i);
                        JSONObject productObj = detailObj.getJSONObject("product");

                        MOrderDetail.MProduct product = new MOrderDetail.MProduct(
                            productObj.getInt("productID"),
                            productObj.getString("name"),
                            productObj.getDouble("price"),
                            productObj.getString("colorName"),
                            productObj.getString("image")
                        );

                        MOrderDetail orderDetail = new MOrderDetail(
                            detailObj.getInt("orderDetailID"),
                            detailObj.getInt("quantity"),
                            detailObj.getString("SKU"),
                            detailObj.getString("size"),
                            detailObj.getInt("stock"),
                            product
                        );

                        orderDetails.add(orderDetail);
                    }
                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(requireContext(), "Lỗi khi tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(requireContext(), "Không thể tải thông tin đơn hàng", Toast.LENGTH_SHORT).show()
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
            case "completed": return "Hoàn thành";
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

    private String getToken() {
        SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
        return prefs.getString("token", "");
    }
} 