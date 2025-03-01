package com.baconga.kttstore.MenuFragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.ProductAdapter;
import com.baconga.kttstore.Models.MProduct;
import com.baconga.kttstore.R;
import com.baconga.kttstore.databinding.FragmentProductListBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baconga.kttstore.SERVER;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.app.Dialog;
import android.widget.Spinner;
import android.widget.RadioGroup;
import android.widget.CheckBox;
import android.widget.Button;
import java.util.Arrays;
import android.widget.ArrayAdapter;

public class ProductListFragment extends Fragment {
    private FragmentProductListBinding binding;
    private ProductAdapter productAdapter;
    private RequestQueue requestQueue;
    private List<MProduct> products;
    private static final String ARG_TARGET_ID = "target_id";
    private static final String ARG_TITLE = "title";
    private Integer targetId;
    private String title;
    private RecyclerView rvProducts;
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int ITEMS_PER_PAGE = 6;
    private ImageButton btnPrevPage;
    private ImageButton btnNextPage;
    private TextView tvPageInfo;
    private int minPrice = 0;
    private int maxPrice = 0;
    private Boolean inStock = null;
    private boolean hasPromotion = false;
    private String sort = "newest";
    private boolean isActivated = true;

    // Constructor với targetId và title ( để lấy sản phẩm Nam - Nữ )
    public static ProductListFragment newInstance(Integer targetId, String title) {
        ProductListFragment fragment = new ProductListFragment();
        Bundle args = new Bundle();
        if (targetId != null) {
            args.putInt(ARG_TARGET_ID, targetId);
        }
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    // Constructor với title ( để lấy tất cả sản phẩm )
    public static ProductListFragment newInstance() {
        ProductListFragment fragment = new ProductListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_TARGET_ID)) {
                targetId = getArguments().getInt(ARG_TARGET_ID);
            }
            title = getArguments().getString(ARG_TITLE);
        }
        products = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Khởi tạo binding
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Ánh xạ views từ binding thay vì findViewById
        btnPrevPage = binding.btnPrevPage;
        btnNextPage = binding.btnNextPage;
        tvPageInfo = binding.tvPageInfo;
        
        // Thiết lập RecyclerView
        setupRecyclerView();

        // Thiết lập sự kiện cho nút phân trang
        setupPaginationControls();

        // Xử lý nút back
        binding.btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Xử lý tìm kiếm
        binding.edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.edtSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchProducts(query);
                }
                // Ẩn bàn phím
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.edtSearch.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Xử lý nút filter
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        // Load dữ liệu
        loadProducts();

        return root;
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.rvProducts.setLayoutManager(layoutManager);
        productAdapter = new ProductAdapter(requireContext(), products, product -> {
            ProductDetailFragment fragment = ProductDetailFragment.newInstance(product.getProductID());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void setupPaginationControls() {
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadProducts();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadProducts();
            }
        });

        updatePaginationControls();
    }

    private void updatePaginationControls() {
        tvPageInfo.setText(String.format("Trang %d/%d", currentPage, totalPages));
        btnPrevPage.setEnabled(currentPage > 1);
        btnNextPage.setEnabled(currentPage < totalPages);
        btnPrevPage.setAlpha(currentPage > 1 ? 1f : 0.5f);
        btnNextPage.setAlpha(currentPage < totalPages ? 1f : 0.5f);
    }

    private void loadProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.paginationLayout.setVisibility(View.GONE);

        // Xây dựng URL với các tham số
        StringBuilder urlBuilder = new StringBuilder(SERVER.products)
            .append("?page=").append(currentPage)
            .append("&limit=").append(ITEMS_PER_PAGE)
            .append("&sort=").append(sort);

        // Thêm targetId nếu có
        if (targetId != null) {
            urlBuilder.append("&targetID=").append(targetId);
        }

        // Thêm các tham số filter khác
        if (minPrice > 0) urlBuilder.append("&minPrice=").append(minPrice);
        if (maxPrice > 0) urlBuilder.append("&maxPrice=").append(maxPrice);
        if (inStock != null) urlBuilder.append("&inStock=").append(inStock);
        if (hasPromotion) urlBuilder.append("&hasPromotion=true");
        urlBuilder.append("&isActivated=").append(isActivated);

        String url = urlBuilder.toString();
        Log.d("ProductListFragment", "Loading URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.paginationLayout.setVisibility(View.VISIBLE);
                    try {
                        JSONArray productsArray = response.getJSONArray("products");
                        totalPages = response.getInt("totalPages");
                        currentPage = response.getInt("currentPage");
                        
                        products.clear();
                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject json = productsArray.getJSONObject(i);
                            MProduct product = parseProduct(json);
                            if (product != null) {
                                products.add(product);
                            }
                        }

                        productAdapter.notifyDataSetChanged();
                        updatePaginationControls();
                        updateEmptyState();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Lỗi xử lý dữ liệu: " + e.getMessage());
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.paginationLayout.setVisibility(View.GONE);
                    showError("Lỗi kết nối server");
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void searchProducts(String query) {
        // Reset all filters when searching
        currentPage = 1;
        sort = "newest";
        minPrice = 0;
        maxPrice = 0;
        inStock = null;
        hasPromotion = false;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        String url = SERVER.products + "?sort=" + sort
                    + "&search=" + query 
                    + "&page=" + currentPage 
                    + "&limit=" + ITEMS_PER_PAGE;
        
        Log.d("ProductListFragment", "Searching with URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray productsArray = response.getJSONArray("products");
                        totalPages = response.getInt("totalPages");
                        currentPage = response.getInt("currentPage");
                        
                        products.clear();
                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject json = productsArray.getJSONObject(i);
                            MProduct product = parseProduct(json);
                            if (product != null) {
                                products.add(product);
                            }
                        }

                        productAdapter.notifyDataSetChanged();
                        updatePaginationControls();
                        updateEmptyState();

                        // Log kết quả tìm kiếm
                        Log.d("ProductListFragment", "Found " + products.size() + " products for query: " + query);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Lỗi xử lý dữ liệu tìm kiếm: " + e.getMessage());
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    showError("Lỗi kết nối khi tìm kiếm");
                    Log.e("ProductListFragment", "Search error: " + error.toString());
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void showFilterDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_product_filter);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Ánh xạ views
        EditText edtMinPrice = dialog.findViewById(R.id.edtMinPrice);
        EditText edtMaxPrice = dialog.findViewById(R.id.edtMaxPrice);
        Spinner spinnerSort = dialog.findViewById(R.id.spinnerSort);
        RadioGroup rgStock = dialog.findViewById(R.id.rgStock);
        CheckBox chkPromotion = dialog.findViewById(R.id.chkPromotion);
        Button btnReset = dialog.findViewById(R.id.btnReset);
        Button btnApply = dialog.findViewById(R.id.btnApplyFilter);

        // Setup spinner sort
        List<String> sortOptions = Arrays.asList(
            "Mới nhất", 
            "Giá tăng dần", 
            "Giá giảm dần",
            "Tên A-Z",
            "Tên Z-A",
            "Tồn kho tăng dần",
            "Tồn kho giảm dần"
        );
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        // Set giá trị hiện tại
        if (minPrice > 0) edtMinPrice.setText(String.valueOf(minPrice));
        if (maxPrice > 0) edtMaxPrice.setText(String.valueOf(maxPrice));
        spinnerSort.setSelection(getSortPosition(sort));
        if (inStock == null) {
            rgStock.check(R.id.rbAll);
        } else if (inStock) {
            rgStock.check(R.id.rbInStock);
        } else {
            rgStock.check(R.id.rbOutOfStock);
        }
        chkPromotion.setChecked(hasPromotion);

        // Reset button
        btnReset.setOnClickListener(v -> {
            edtMinPrice.setText("");
            edtMaxPrice.setText("");
            spinnerSort.setSelection(0);
            rgStock.check(R.id.rbAll);
            chkPromotion.setChecked(false);
        });

        // Apply button
        btnApply.setOnClickListener(v -> {
            // Reset page
            currentPage = 1;

            // Lấy giá trị sort
            sort = getSortValue(spinnerSort.getSelectedItemPosition());

            // Lấy giá trị khoảng giá
            String minPriceStr = edtMinPrice.getText().toString();
            String maxPriceStr = edtMaxPrice.getText().toString();
            minPrice = minPriceStr.isEmpty() ? 0 : Integer.parseInt(minPriceStr);
            maxPrice = maxPriceStr.isEmpty() ? 0 : Integer.parseInt(maxPriceStr);

            // Lấy trạng thái stock
            int checkedRadioId = rgStock.getCheckedRadioButtonId();
            if (checkedRadioId == R.id.rbInStock) {
                inStock = true;
            } else if (checkedRadioId == R.id.rbOutOfStock) {
                inStock = false;
            } else {
                inStock = null;
            }

            // Lấy trạng thái khuyến mãi
            hasPromotion = chkPromotion.isChecked();

            // Load lại sản phẩm
            loadProducts();
            dialog.dismiss();
        });

        dialog.show();
    }

    private String getSortValue(int position) {
        switch (position) {
            case 0: return "newest";
            case 1: return "price-asc";
            case 2: return "price-desc";
            case 3: return "name-asc";
            case 4: return "name-desc";
            case 5: return "stock-asc";
            case 6: return "stock-desc";
            default: return "newest";
        }
    }

    private int getSortPosition(String sortValue) {
        switch (sortValue) {
            case "newest": return 0;
            case "price-asc": return 1;
            case "price-desc": return 2;
            case "name-asc": return 3;
            case "name-desc": return 4;
            case "stock-asc": return 5;
            case "stock-desc": return 6;
            default: return 0;
        }
    }

    private void updateEmptyState() {
        if (products.isEmpty()) {
            binding.rvProducts.setVisibility(View.GONE);
            binding.tvNoProducts.setVisibility(View.VISIBLE);
        } else {
            binding.rvProducts.setVisibility(View.VISIBLE);
            binding.tvNoProducts.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private MProduct parseProduct(JSONObject json) throws JSONException {
        MProduct product = new MProduct(json);

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
        if (requestQueue != null) {
            requestQueue.cancelAll(request -> true);
        }
        binding = null;  // Để tránh memory leak
    }
}
