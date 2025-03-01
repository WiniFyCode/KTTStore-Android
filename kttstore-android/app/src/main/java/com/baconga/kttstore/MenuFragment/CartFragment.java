package com.baconga.kttstore.MenuFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.CartAdapter;
import com.baconga.kttstore.Activity.VoucherActivity;
import com.baconga.kttstore.Models.CartItem;
import com.baconga.kttstore.Models.MCart;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.FragmentCartBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment implements CartAdapter.CartItemListener {
    private FragmentCartBinding binding;
    private CartAdapter adapter;
    private RequestQueue requestQueue;
    private List<CartItem> cartItems = new ArrayList<>();
    private double totalAmount = 0;
    private String defaultAddress = "";
    private String userFullname = "";
    private String userPhone = "";
    private String userEmail = "";
    private String appliedCouponCode = "";
    private double discountAmount = 0;
    private int userCouponsID = 0;
    private String discountType = "";
    private double discountValue = 0;
    private double maxDiscountAmount = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        setupViews();
        loadUserInfo();
        return binding.getRoot();
    }

    private void setupViews() {
        setupRecyclerView();
        setupListeners();
        loadUserInfo();
    }

    private void setupRecyclerView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(this, requireContext());
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnCheckout.setOnClickListener(v -> {
            processSelectedItems();
        });

        binding.btnContinueShopping.setOnClickListener(v -> {
            HomeFragment homeFragment = new HomeFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, homeFragment)
                .commit();
        });

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadCart);

        binding.btnApplyCoupon.setOnClickListener(v -> {
            String couponCode = binding.edtCouponCode.getText().toString().trim();
            if (!couponCode.isEmpty()) {
                applyCoupon(couponCode);
            } else {
                showError("Vui lòng nhập mã giảm giá");
            }
        });

        // Thêm listener cho nút xóa coupon
        binding.btnRemoveCoupon.setOnClickListener(v -> {
            resetCouponState();
            Toast.makeText(requireContext(), "Đã hủy mã giảm giá", Toast.LENGTH_SHORT).show();
        });

        // Thêm listener đơn giản cho selectVoucherText
        binding.selectVoucherText.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VoucherActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestQueue = Volley.newRequestQueue(requireContext());
        
        // Thêm xử lý dropdown cho địa chỉ
        view.findViewById(R.id.addressHeader).setOnClickListener(v -> {
            LinearLayout addressSection = view.findViewById(R.id.addressSection);
            ImageView imgArrow = view.findViewById(R.id.imgAddressArrow);
            
            // Toggle visibility
            if (addressSection.getVisibility() == View.VISIBLE) {
                addressSection.setVisibility(View.GONE);
                imgArrow.setRotation(0f);
            } else {
                addressSection.setVisibility(View.VISIBLE);
                imgArrow.setRotation(180f);
            }
        });

        // Thêm xử lý dropdown cho thông tin người dùng
        view.findViewById(R.id.userInfoHeader).setOnClickListener(v -> {
            LinearLayout userInfoLayout = view.findViewById(R.id.userInfoLayout);
            ImageView imgArrow = view.findViewById(R.id.imgUserInfoArrow);
            
            // Toggle visibility
            if (userInfoLayout.getVisibility() == View.VISIBLE) {
                userInfoLayout.setVisibility(View.GONE);
                imgArrow.setRotation(0f);
            } else {
                userInfoLayout.setVisibility(View.VISIBLE);
                imgArrow.setRotation(180f);
            }
        });

        loadDefaultAddress();
        loadCart();
    }

    private void loadCart() {
        if (!isAdded() || binding == null) return;

        // Lưu lại trạng thái chọn của các sản phẩm hiện tại
        Map<String, Boolean> selectedStates = new HashMap<>();
        for (CartItem item : cartItems) {
            selectedStates.put(item.getCart().getCartID(), item.isSelected());
        }

        String url = SERVER.get_cart;
        Log.d("CartFragment", "Loading cart from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded() || binding == null) return;
                    
                    try {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        JSONArray items = response.getJSONArray("items");
                        totalAmount = response.getDouble("totalAmount");

                        cartItems.clear();
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            
                            // Log để kiểm tra response
                            Log.d("CartFragment", "Cart Item: " + item.toString());
                            
                            try {
                                // Lấy thông tin cơ bản
                                String productName = item.getString("name");
                                double originalPrice = item.getDouble("price");
                                String thumbnail = item.getString("thumbnail");
                                String size = item.getString("size");
                                String colorName = item.getString("colorName");
                                int quantity = item.getInt("quantity");
                                int stock = item.getInt("stock");
                                
                                // Lấy các ID - Thêm kiểm tra null và giá trị mặc định
                                String cartId = String.valueOf(item.optInt("cartID", 0));
                                String productId = String.valueOf(item.optInt("productID", 0));
                                
                                // Parse SKU để lấy colorID nếu có
                                String sku = item.optString("SKU", "");
                                String colorId = "0";
                                String sizeId = "0";
                                String sizestockId = "0";
                                
                                if (!sku.isEmpty()) {
                                    String[] skuParts = sku.split("_");
                                    if (skuParts.length >= 4) {
                                        // SKU format: productID_colorID_sizeID_sizestockID
                                        colorId = skuParts[1];
                                        sizeId = skuParts[2];
                                        sizestockId = skuParts[3];
                                    }
                                }
                                
                                // Lấy giá khuyến mãi nếu có
                                Double discountPrice = null;
                                if (!item.isNull("promotion")) {
                                    JSONObject promotion = item.getJSONObject("promotion");
                                    discountPrice = promotion.getDouble("finalPrice");
                                }
                                
                                // Tạo đối tượng MCart với đầy đủ thông tin
                                MCart cart = new MCart(
                                    cartId,      // cartID
                                    productId,   // productId
                                    colorId,     // colorId
                                    sizeId,      // sizeId
                                    sizestockId, // sizestockId
                                    size,        // size
                                    quantity     // quantity
                                );
                                
                                CartItem cartItem = new CartItem(
                                    cart, 
                                    productName, 
                                    colorName, 
                                    thumbnail, 
                                    originalPrice,
                                    discountPrice,
                                    stock
                                );
                                cartItems.add(cartItem);
                                
                            } catch (JSONException e) {
                                Log.e("CartFragment", "Error parsing cart item: " + e.getMessage());
                                // Tiếp tục với item tiếp theo nếu có lỗi
                                continue;
                            }
                        }

                        // Khôi phục trạng thái chọn cho các sản phẩm
                        for (CartItem item : cartItems) {
                            String cartId = item.getCart().getCartID();
                            if (selectedStates.containsKey(cartId)) {
                                item.setSelected(selectedStates.get(cartId));
                            }
                        }

                        updateUI();

                    } catch (JSONException e) {
                        if (isAdded() && binding != null) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                            showError("Có lỗi xảy ra khi đọc dữ liệu: " + e.getMessage());
                        }
                    }
                },
                error -> {
                    if (isAdded() && binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        showError("Không thể tải giỏ hàng");
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

    private void updateUI() {
        if (adapter != null) {
            adapter.setItems(new ArrayList<>(cartItems));
        } else {
            setupRecyclerView();
            if (adapter != null) {
                adapter.setItems(new ArrayList<>(cartItems));
            }
        }
        
        // Cập nhật trạng thái empty state
        updateEmptyState(cartItems.isEmpty());

        // Nếu có sản phẩm, cập nhật các thông tin khác
        if (!cartItems.isEmpty()) {
            // Tính toán tổng tiền cho các sản phẩm được chọn
            double selectedTotal = 0;
            boolean hasSelectedItems = false;
            
            for (CartItem item : cartItems) {
                if (item.isSelected()) {
                    double itemPrice = (item.getDiscountPrice() != null) ? 
                                     item.getDiscountPrice() : 
                                     item.getOriginalPrice();
                    selectedTotal += (itemPrice * item.getCart().getQuantity());
                    hasSelectedItems = true;
                }
            }

            // Xử lý hiển thị giảm giá nếu có coupon
            if (!appliedCouponCode.isEmpty()) {
                binding.layoutDiscount.setVisibility(View.VISIBLE);
                
                String discountInfo = appliedCouponCode + " - Giảm " + 
                    (discountType.equals("percentage") ? 
                        String.format("%.0f%%", discountValue) : 
                        String.format("%,.0f₫", discountValue)) +
                    String.format(": -%,.0f₫", discountAmount);
                    
                binding.txtDiscountAmount.setText(discountInfo);
                
                if (selectedTotal > 0) {
                    selectedTotal = Math.max(0, selectedTotal - discountAmount);
                }
            } else {
                binding.layoutDiscount.setVisibility(View.GONE);
            }

            // Cập nhật hiển thị tổng tiền
            binding.txtTotalAmount.setText(String.format("Tổng cộng: %,.0f₫", selectedTotal));
            
            // Cập nhật trạng thái nút thanh toán
            binding.btnCheckout.setEnabled(hasSelectedItems);
            binding.btnCheckout.setAlpha(hasSelectedItems ? 1f : 0.5f);
        }
        
        // Thông báo cho adapter cập nhật giao diện
        adapter.notifyDataSetChanged();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (binding != null) {
            binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.swipeRefreshLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.bottomLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.layoutDiscount.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.selectVoucherText.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.edtCouponCode.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.btnApplyCoupon.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        // Nếu có coupon đã áp dụng, reset coupon khi thay đổi số lượng
        if (!appliedCouponCode.isEmpty()) {
            resetCouponState();
            Toast.makeText(requireContext(), "Đã hủy mã giảm giá do thay đổi số lượng", Toast.LENGTH_SHORT).show();
        }

        String url = SERVER.update_cart.replace(":id", item.getCart().getCartID());

        JSONObject params = new JSONObject();
        try {
            params.put("quantity", newQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, params,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        String message = response.getString("message");
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        
                        // Cập nhật số lượng trong item hiện tại
                        item.getCart().setQuantity(newQuantity);
                        
                        // Load lại toàn bộ cart để cập nhật thông tin mới nhất
                        loadCart();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        String errorMessage;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorResponse = new String(error.networkResponse.data);
                            JSONObject errorJson = new JSONObject(errorResponse);
                            
                            if (error.networkResponse.statusCode == 400) {
                                int maxQuantity = errorJson.getInt("maxQuantity");
                                errorMessage = errorJson.getString("message");
                                adapter.updateItemQuantity(item.getCart().getCartID(), maxQuantity);
                            } else {
                                errorMessage = errorJson.getString("message");
                            }
                        } else {
                            errorMessage = "Không thể cập nhật số lượng";
                        }
                        showError(errorMessage);
                    } catch (Exception e) {
                        showError("Có lỗi xảy ra khi cập nhật số lượng");
                        e.printStackTrace();
                    }
                    // Load lại cart ngay cả khi có lỗi để đảm bảo dữ liệu đồng bộ
                    loadCart();
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
    public void onItemDeleted(CartItem item) {
        String url = SERVER.delete_cart.replace(":id", item.getCart().getCartID());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> loadCart(),
                error -> showError("Không thể xóa sản phẩm")
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
    public void onItemSelected(CartItem item, boolean isSelected) {
        // Nếu có coupon đã áp dụng và bỏ chọn sản phẩm, reset coupon
        if (!appliedCouponCode.isEmpty() && !isSelected) {
            resetCouponState();
            Toast.makeText(requireContext(), "Đã hủy mã giảm giá do thay đổi sản phẩm", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }

    private void showError(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private String getToken() {
        SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
        binding = null;
    }

    private void processSelectedItems() {
        // Kiểm tra các điều kiện trước khi thanh toán
        List<CartItem> selectedItems = adapter.getSelectedItems();
        if (selectedItems.isEmpty()) {
            showError("Vui lòng chọn sản phẩm để thanh toán");
            return;
        }

        if (defaultAddress.isEmpty() || defaultAddress.equals("Bạn chưa có địa chỉ mặc định")) {
            showError("Vui lòng thêm địa chỉ giao hàng trước khi thanh toán");
            return;
        }

        // Lấy thông tin user từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
        String fullname = prefs.getString("fullname", "");
        String phone = prefs.getString("phone", "");
        String email = prefs.getString("email", "");

        // Kiểm tra thông tin user
        if (fullname.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            showError("Vui lòng cập nhật đầy đủ thông tin cá nhân");
            return;
        }

        // Tạo request body
        JSONObject requestBody = new JSONObject();
        try {
            // Thông tin cơ bản
            requestBody.put("fullname", fullname);
            requestBody.put("phone", phone);
            requestBody.put("email", email);
            requestBody.put("address", defaultAddress);
            
            

            // Thông tin coupon nếu có
            if (!appliedCouponCode.isEmpty()) {
                requestBody.put("userCouponsID", userCouponsID);
                
            }

            // Danh sách sản phẩm
            JSONArray items = new JSONArray();
            for (CartItem item : selectedItems) {
                JSONObject cartItem = new JSONObject();
                String sku = String.format("%s_%s_%s_%s",
                    item.getCart().getProductId(),
                    item.getCart().getColorId(),
                    item.getCart().getSizeId(),
                    item.getCart().getSizestockId()
                );
                cartItem.put("SKU", sku);
                cartItem.put("quantity", item.getCart().getQuantity());
                items.put(cartItem);
            }
            requestBody.put("items", items);

            // Gọi API tạo order
            createOrder(requestBody);

        } catch (JSONException e) {
            e.printStackTrace();
            showError("Có lỗi xảy ra khi tạo đơn hàng");
        }
    }

    private void createOrder(JSONObject requestBody) {
        String url = SERVER.create_order;
        
        // Hiển thị loading và disable nút thanh toán
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnCheckout.setEnabled(false);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    // Ẩn loading và enable nút thanh toán
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCheckout.setEnabled(true);
                    
                    try {
                        // Lấy thông tin đơn hàng
                        JSONObject order = response.getJSONObject("order");
                        int orderID = order.getInt("orderID");
                        
                        // Hiển thị thông báo thành công
                        Toast.makeText(requireContext(), 
                            "Đặt hàng thành công!", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Reset trạng thái giỏ hàng
                        resetCouponState();
                        loadCart();
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Có lỗi xảy ra khi xử lý đơn hàng");
                    }
                },
                error -> {
                    // Ẩn loading và enable nút thanh toán
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCheckout.setEnabled(true);
                    
                    try {
                        String errorMessage;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorResponse = new String(error.networkResponse.data);
                            JSONObject errorJson = new JSONObject(errorResponse);
                            errorMessage = errorJson.getString("message");
                        } else {
                            errorMessage = "Không thể tạo đơn hàng";
                        }
                        showError(errorMessage);
                    } catch (Exception e) {
                        showError("Có lỗi xảy ra khi tạo đơn hàng");
                        e.printStackTrace();
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

    private void loadDefaultAddress() {
        String url = SERVER.get_addresses;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    defaultAddress = "Bạn chưa có địa chỉ mặc định";
                    // Tìm địa chỉ mặc định
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject address = response.getJSONObject(i);
                        if (address.getBoolean("isDefault")) {
                            defaultAddress = address.getString("address");
                            break;
                        }
                    }
                    updateAddressUI();
                } catch (JSONException e) {
                    e.printStackTrace();
                    defaultAddress = "Không thể tải địa chỉ";
                    updateAddressUI();
                }
            },
            error -> {
                defaultAddress = "Không thể tải địa chỉ";
                updateAddressUI();
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

    private void updateAddressUI() {
        if (binding != null) {
            binding.txtDefaultAddress.setText(defaultAddress);
            
            boolean hasNoAddress = defaultAddress.equals("Bạn chưa có địa chỉ mặc định");
            binding.imgWarningAddress.setVisibility(hasNoAddress ? View.VISIBLE : View.GONE);
            
            // Sử dụng màu từ resource colors.xml
            binding.txtDefaultAddress.setTextColor(
                requireContext().getColor(hasNoAddress ? android.R.color.holo_red_dark : android.R.color.darker_gray)
            );
        }
    }

    private void loadUserInfo() {
        SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
        userFullname = prefs.getString("fullname", "");
        userPhone = prefs.getString("phone", "");
        userEmail = prefs.getString("email", "");
        updateUserInfoUI();
    }

    private void updateUserInfoUI() {
        if (binding != null) {
            binding.txtUserFullname.setText(userFullname.isEmpty() ? "Chưa cập nhật" : userFullname);
            binding.txtUserPhone.setText(userPhone.isEmpty() ? "Chưa cập nhật" : userPhone);
            binding.txtUserEmail.setText(userEmail.isEmpty() ? "Chưa cập nhật" : userEmail);

            // Set màu text tùy thuộc vào trạng thái
            int textColor = requireContext().getColor(
                userFullname.isEmpty() || userPhone.isEmpty() || userEmail.isEmpty() 
                ? android.R.color.holo_red_dark 
                : android.R.color.darker_gray
            );

            binding.txtUserFullname.setTextColor(textColor);
            binding.txtUserPhone.setTextColor(textColor);
            binding.txtUserEmail.setTextColor(textColor);
        }
    }

    private void applyCoupon(String couponCode) {
        // Kiểm tra xem có sản phẩm nào được chọn không
        if (adapter.getSelectedItems().isEmpty()) {
            showError("Vui lòng chọn sản phẩm trước");
            return;
        }

        String url = SERVER.apply_coupon;
        
        // Tính tổng giá trị đơn hàng từ các sản phẩm đã chọn
        double orderValue = 0;
        for (CartItem item : adapter.getSelectedItems()) {
            orderValue += item.getSubtotal();
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnApplyCoupon.setEnabled(false);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("code", couponCode);
            requestBody.put("orderValue", orderValue);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnApplyCoupon.setEnabled(true);
                    
                    try {
                        String message = response.getString("message");
                        
                        // Lưu thông tin cơ bản
                        discountAmount = response.getDouble("discountAmount");
                        userCouponsID = response.getInt("userCouponsID");
                        appliedCouponCode = couponCode;
                        
                        // Lưu thêm thông tin chi tiết về coupon
                        JSONObject coupon = response.getJSONObject("coupon");
                        discountType = coupon.getString("discountType");
                        discountValue = coupon.getDouble("discountValue");
                        maxDiscountAmount = coupon.getDouble("maxDiscountAmount");
                        
                       
                        Toast.makeText(requireContext(),"Áp dụng mã thành công!", Toast.LENGTH_LONG).show();
                        
                        // Xóa text trong ô input
                        binding.edtCouponCode.setText("");
                        
                        // Chỉ cập nhật UI, không load lại cart
                        updateUI();
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Có lỗi xảy ra khi áp dụng mã giảm giá");
                        resetCouponState();
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnApplyCoupon.setEnabled(true);
                    
                    try {
                        String errorMessage;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorResponse = new String(error.networkResponse.data);
                            JSONObject errorJson = new JSONObject(errorResponse);
                            errorMessage = errorJson.getString("message");
                        } else {
                            errorMessage = "Không thể áp dụng mã giảm giá";
                        }
                        showError(errorMessage);
                        resetCouponState();
                    } catch (Exception e) {
                        showError("Có lỗi xảy ra khi áp dụng mã giảm giá");
                        e.printStackTrace();
                        resetCouponState();
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

    // Thêm phương thức mới để reset trạng thái coupon
    private void resetCouponState() {
        appliedCouponCode = "";
        discountAmount = 0;
        userCouponsID = 0;
        discountType = "";
        discountValue = 0;
        maxDiscountAmount = 0;
        updateUI();
    }
}