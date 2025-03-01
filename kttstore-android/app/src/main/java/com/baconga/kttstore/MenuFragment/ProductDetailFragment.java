package com.baconga.kttstore.MenuFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.ProductImageAdapter;
import com.baconga.kttstore.Adapters.ProductAdapter;
import com.baconga.kttstore.Models.MProduct;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.FragmentProductDetailBinding;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDetailFragment extends Fragment {
    private FragmentProductDetailBinding binding;
    private RequestQueue requestQueue;
    private String productId;
    private ProductImageAdapter imageAdapter;
    private int selectedColorIndex = 0;
    private JSONObject currentProduct;
    private String selectedSize = null;
    private String selectedColor = null;
    private boolean isFavorite = false;
    private boolean isDescriptionExpanded = false;
    private RecyclerView rvRelatedProducts;
    private ProductAdapter relatedProductsAdapter;
    private List<MProduct> relatedProducts = new ArrayList<>();
    private String currentCategoryId;

    public static ProductDetailFragment newInstance(String productId) {
        ProductDetailFragment fragment = new ProductDetailFragment();
        Bundle args = new Bundle();
        args.putString("productId", productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup image adapter
        imageAdapter = new ProductImageAdapter(requireContext(), new ArrayList<>());
        binding.viewPagerImages.setAdapter(imageAdapter);

        // Setup back button
        binding.btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Setup add to cart button
        binding.btnAddToCart.setOnClickListener(v -> {
            if (selectedColor == null || selectedSize == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn màu sắc và kích thước", Toast.LENGTH_SHORT).show();
                return;
            }
            addToCart();
        });

        // Setup description toggle
        binding.btnToggleDescription.setOnClickListener(v -> {
            isDescriptionExpanded = !isDescriptionExpanded;
            
            if (isDescriptionExpanded) {
                // Mở rộng description
                binding.txtDescription.setMaxLines(Integer.MAX_VALUE);
                binding.txtDescription.setEllipsize(null);
                binding.btnToggleDescription.setText("Thu gọn");
            } else {
                // Thu gọn description
                binding.txtDescription.setMaxLines(4);
                binding.txtDescription.setEllipsize(TextUtils.TruncateAt.END);
                binding.btnToggleDescription.setText("Xem thêm");
            }
        });

        // Thêm xử lý cho nút yêu thích
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());

        // Kiểm tra trạng thái yêu thích khi load sản phẩm
        checkFavoriteStatus();

        // Load product details
        loadProductDetails();

        // Setup RecyclerView for related products
        rvRelatedProducts = binding.rvRelatedProducts;
        setupRelatedProductsRecyclerView();
    }

    private void loadProductDetails() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey("productId")) {
            Log.e("ProductDetail", "No productId provided");
            return;
        }

        String productId = args.getString("productId");
        if (productId == null) {
            Log.e("ProductDetail", "ProductId is null");
            return;
        }

        String url = SERVER.get_product_by_id.replace(":id", productId);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject product = response.getJSONObject("product");
                            currentProduct = product;
                            
                            // Get category ID and load related products
                            currentCategoryId = product.getString("categoryID");
                            loadRelatedProducts();

                            // Parse product details
                            String name = product.getString("name");
                            String description = product.getString("description");
                            String price = product.getString("price");

                            // Update UI
                            binding.txtProductName.setText(name);
                            binding.txtDescription.setText(description);
                            binding.txtDescription.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Log để debug
                                    Log.d("ProductDetail", "Line count: " + binding.txtDescription.getLineCount());
                                    Log.d("ProductDetail", "Text length: " + description.length());
                                    
                                    // Luôn hiển thị nút nếu text dài hơn 200 ký tự
                                    if (description.length() > 200) {
                                        binding.btnToggleDescription.setVisibility(View.VISIBLE);
                                        // Mặc định thu gọn
                                        binding.txtDescription.setMaxLines(4);
                                        binding.txtDescription.setEllipsize(TextUtils.TruncateAt.END);
                                    } else {
                                        binding.btnToggleDescription.setVisibility(View.GONE);
                                        binding.txtDescription.setMaxLines(Integer.MAX_VALUE);
                                    }
                                }
                            });

                            // Format price
                            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                            double priceValue = Double.parseDouble(price.replace(".", ""));

                            // Check if product has valid promotion
                            if (!product.isNull("promotion")) {
                                JSONObject promotion = product.getJSONObject("promotion");
                                String discountedPrice = promotion.getString("discountedPrice");
                                int discountPercent = promotion.getInt("discountPercent");

                                // Show original price with strikethrough
                                binding.txtOriginalPrice.setVisibility(View.VISIBLE);
                                binding.txtOriginalPrice.setPaintFlags(binding.txtOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                                binding.txtOriginalPrice.setText(formatter.format(priceValue));

                                // Show discounted price
                                double discountedPriceValue = Double.parseDouble(discountedPrice.replace(".", ""));
                                binding.txtPrice.setText(formatter.format(discountedPriceValue) + " (-" + discountPercent + "%)");
                            } else {
                                binding.txtOriginalPrice.setVisibility(View.GONE);
                                binding.txtPrice.setText(formatter.format(priceValue));
                            }

                            // Setup colors and initial images
                            setupColors(product.getJSONArray("colors"));

                        } else {
                            Toast.makeText(requireContext(), "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Lỗi khi tải thông tin sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(requireContext(), "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void setupColors(JSONArray colors) throws JSONException {
        binding.colorChipGroup.removeAllViews();
        binding.colorChipGroup.setSelectionRequired(true);
        binding.colorChipGroup.setSingleSelection(true);

        for (int i = 0; i < colors.length(); i++) {
            JSONObject color = colors.getJSONObject(i);
            String colorName = color.getString("colorName");

            Chip chip = new Chip(requireContext());
            chip.setText(colorName);
            chip.setCheckable(true);
            chip.setClickable(true);

            final int colorIndex = i;
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedColorIndex = colorIndex;
                    selectedColor = colorName;
                    try {
                        updateImagesAndSizes(colors.getJSONObject(colorIndex));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (binding.colorChipGroup.getCheckedChipId() == View.NO_ID) {
                    selectedColor = null;
                }
            });

            binding.colorChipGroup.addView(chip);

            if (i == 0) {
                chip.setChecked(true);
            }
        }
    }

    private void updateImagesAndSizes(JSONObject color) throws JSONException {
        // Update images
        JSONArray images = color.getJSONArray("images");
        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < images.length(); i++) {
            imageUrls.add(images.getString(i));
        }
        imageAdapter.updateImages(imageUrls);

        // Update sizes
        setupSizeChips(color.getJSONArray("sizes"));
    }

    private void setupSizeChips(JSONArray sizes) throws JSONException {
        binding.sizeChipGroup.removeAllViews();
        binding.sizeChipGroup.setSelectionRequired(true);
        binding.sizeChipGroup.setSingleSelection(true);

        selectedSize = null;

        for (int i = 0; i < sizes.length(); i++) {
            JSONObject sizeObj = sizes.getJSONObject(i);
            String size = sizeObj.getString("size");
            int stock = sizeObj.getInt("stock");
            String sku = sizeObj.getString("SKU");

            Chip chip = new Chip(requireContext());
            chip.setText(size);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setTag(sku);

            if (stock == 0) {
                chip.setEnabled(false);
                chip.setText(size + " (Hết hàng)");
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSize = size;
                    if (selectedColor != null && selectedSize != null) {
                        checkFavoriteStatus();
                    }
                } else if (binding.sizeChipGroup.getCheckedChipId() == View.NO_ID) {
                    selectedSize = null;
                }
            });

            binding.sizeChipGroup.addView(chip);
        }
    }

    private String getSelectedSKU() {
        try {
            if (currentProduct == null || selectedColor == null || selectedSize == null) {
                return null;
            }

            JSONObject selectedColorObj = currentProduct.getJSONArray("colors")
                    .getJSONObject(selectedColorIndex);

            JSONArray sizes = selectedColorObj.getJSONArray("sizes");
            for (int i = 0; i < sizes.length(); i++) {
                JSONObject sizeObj = sizes.getJSONObject(i);
                String size = sizeObj.getString("size");
                if (size.equals(selectedSize)) {
                    return sizeObj.getString("SKU");
                }
            }

            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addToCart() {
        try {
            // Lấy thông tin màu sắc đã chọn
            JSONObject selectedColorObj = currentProduct.getJSONArray("colors")
                    .getJSONObject(selectedColorIndex);

            // Tìm size object tương ứng
            JSONArray sizes = selectedColorObj.getJSONArray("sizes");
            String selectedSKU = getSelectedSKU();

            if (selectedSKU == null) {
                Toast.makeText(requireContext(), "Không tìm thấy thông tin size", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo request body
            JSONObject params = new JSONObject();
            params.put("SKU", selectedSKU);
            params.put("quantity", 1);

            // Gọi API thêm vào giỏ hàng
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    SERVER.add_to_cart,
                    params,
                    response -> {
                        try {
                            String message = response.getString("message");
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(requireContext(), "Thêm vào giỏ hàng thành công", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        String errorMessage = "Không thể thêm vào giỏ hàng";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data);
                                JSONObject errorJson = new JSONObject(errorResponse);
                                errorMessage = errorJson.getString("message");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + getToken());
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);

        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getToken() {
        SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    private void checkFavoriteStatus() {
        // Kiểm tra xem đã có token và productId chưa
        String token = getToken();
        if (token.isEmpty() || productId == null) {
            return;
        }

        // Tạo URL để kiểm tra trạng thái yêu thích
        String url = SERVER.check_favorite.replace(":productID", productId);

        // Tạo request kiểm tra trạng thái yêu thích
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        // Kiểm tra success trước
                        if (response.getBoolean("success")) {
                            // Cập nhật trạng thái yêu thích và icon
                            isFavorite = response.getBoolean("isFavorited"); // Thay đổi từ "isFavorite" thành "isFavorited"
                            updateFavoriteIcon();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Favorite", "Error checking favorite status: " + error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void toggleFavorite() {
        String token = getToken();
        if (token.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        String url;
        int method;

        if (isFavorite) {
            url = SERVER.favorites + "/product/" + productId;
            method = Request.Method.DELETE;
        } else {
            url = SERVER.add_to_favorites;
            method = Request.Method.POST;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                method,
                url,
                isFavorite ? null : createFavoriteRequestBody(),
                response -> {
                    try {
                        boolean isSuccess;
                        // Kiểm tra xem response có phải là boolean trực tiếp không
                        if (response.has("success")) {
                            isSuccess = response.getBoolean("success");
                        } else {
                            isSuccess = response.getBoolean(""); // Nếu response là boolean trực tiếp
                        }

                        if (isSuccess) {
                            isFavorite = !isFavorite;
                            updateFavoriteIcon();
                            String message = isFavorite ?
                                    "Đã thêm vào danh sách yêu thích" :
                                    "Đã xóa khỏi danh sách yêu thích";
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        } else {
                            String message = isFavorite ?
                                    "Không thể xóa khỏi danh sách yêu thích" :
                                    "Không thể thêm vào danh sách yêu thích";
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String message = isFavorite ?
                            "Không thể xóa khỏi danh sách yêu thích" :
                            "Không thể thêm vào danh sách yêu thích";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private JSONObject createFavoriteRequestBody() {
        JSONObject params = new JSONObject();
        try {
            params.put("productID", productId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

    private void updateFavoriteIcon() {
        // Cập nhật icon trái tim dựa vào trạng thái yêu thích
        binding.btnFavorite.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
    }

    private void setupRelatedProductsRecyclerView() {
        relatedProductsAdapter = new ProductAdapter(requireContext(), relatedProducts, product -> {
            // Navigate to product detail
            ProductDetailFragment fragment = ProductDetailFragment.newInstance(product.getProductID());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), 
            LinearLayoutManager.HORIZONTAL, false);
        rvRelatedProducts.setLayoutManager(layoutManager);
        rvRelatedProducts.setAdapter(relatedProductsAdapter);
    }

    private void loadRelatedProducts() {
        String url = SERVER.get_products + "?categoryID=" + currentCategoryId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    relatedProducts.clear();
                    try {
                        JSONArray productsArray = response.getJSONArray("products");
                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject json = productsArray.getJSONObject(i);
                            // Bỏ qua sản phẩm hiện tại
                            if (!json.getString("_id").equals(productId)) {
                                MProduct product = parseProduct(json);
                                if (product != null) {
                                    relatedProducts.add(product);
                                }
                            }
                        }
                        relatedProductsAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(),
                                "Lỗi tải sản phẩm liên quan: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(requireContext(),
                            "Lỗi kết nối server",
                            Toast.LENGTH_SHORT).show();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    // Reuse parseProduct method from CategoryDetailFragment
    private MProduct parseProduct(JSONObject json) throws JSONException {
        // Tạo đối tượng sản phẩm với thông tin cơ bản
        MProduct product = new MProduct(json);

        // Xử lý thông tin khuyến mãi nếu có
        if (!json.isNull("promotion")) {
            JSONObject promotion = json.getJSONObject("promotion");
            MProduct.Promotion productPromotion = new MProduct.Promotion(
                promotion.getInt("discountPercent"),
                promotion.getString("finalPrice")
            );
            product.setPromotion(productPromotion);
            
            product.setPromotionName(promotion.optString("name", ""));
            product.setDiscountPercent(promotion.getInt("discountPercent"));
            String discountedPriceStr = promotion.getString("finalPrice").replace(".", "");
            product.setDiscountedPrice(Double.parseDouble(discountedPriceStr));
            product.setPromotionEndDate(promotion.optString("endDate", ""));
        }

        return product;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
