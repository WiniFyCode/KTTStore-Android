package com.baconga.kttstore.MenuFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.ProductAdapter;
import com.baconga.kttstore.Models.MCategory;
import com.baconga.kttstore.Models.MProduct;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CategoryDetailFragment extends Fragment {
    private MCategory category;
    private TextView tvTitle;
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private List<MProduct> products;
    private RequestQueue requestQueue;

    public static CategoryDetailFragment newInstance(MCategory category) {
        CategoryDetailFragment fragment = new CategoryDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = (MCategory) getArguments().getSerializable("category");
        }
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_detail, container, false);

        // Ánh xạ view
        tvTitle = view.findViewById(R.id.tvTitle);
        rvProducts = view.findViewById(R.id.rvProducts);

        // Thiết lập RecyclerView
        products = new ArrayList<>();
        setupRecyclerView();

        // Hiển thị tên category
        if (category != null) {
            tvTitle.setText(category.getName());
            loadProducts();
        }

        return view;
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(requireContext(), products, product -> {
            // Navigate to ProductDetailFragment
            ProductDetailFragment fragment = ProductDetailFragment.newInstance(product.getProductID());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvProducts.setAdapter(productAdapter);
    }

    private void loadProducts() {
        String url = SERVER.get_products + "?categoryID=" + category.getCategoryID();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    products.clear();
                    try {
                        JSONArray productsArray = response.getJSONArray("products");
                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject json = productsArray.getJSONObject(i);
                            MProduct product = parseProduct(json);
                            if (product != null) {
                                products.add(product);
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), 
                            "Lỗi tải dữ liệu: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(requireContext(),
                        "Lỗi kết nối server", 
                        Toast.LENGTH_SHORT).show();
                });

        // Giảm timeout xuống 5 giây
        request.setRetryPolicy(new DefaultRetryPolicy(
            5000,  // 5 seconds
            1,     // không retry
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

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
            
            // Set các thông tin promotion chi tiết
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
    }
}
