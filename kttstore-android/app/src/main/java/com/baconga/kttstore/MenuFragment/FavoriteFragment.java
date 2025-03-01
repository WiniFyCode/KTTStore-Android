package com.baconga.kttstore.MenuFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.FavoriteAdapter;
import com.baconga.kttstore.Models.FavoriteItem;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.FragmentFavoriteBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteFragment extends Fragment implements FavoriteAdapter.FavoriteItemListener {
    private FragmentFavoriteBinding binding;
    private FavoriteAdapter adapter;
    private RequestQueue requestQueue;
    private List<FavoriteItem> favoriteItems = new ArrayList<>();
    private static final String TAG = "FavoriteFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());
        setupViews();
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupViews() {
        // Setup RecyclerView
        binding.recyclerViewFavorite.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerViewFavorite.setLayoutManager(layoutManager);
        
        // Thêm divider
        RecyclerView.ItemDecoration divider = new DividerItemDecoration(requireContext(), layoutManager.getOrientation());
        binding.recyclerViewFavorite.addItemDecoration(divider);
        
        // Setup adapter
        if (adapter == null) {
            adapter = new FavoriteAdapter(this);
        }
        binding.recyclerViewFavorite.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadFavorites);
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.Primary_blue);

        // Setup button
        binding.btnContinueShopping.setOnClickListener(v -> {
            // Chuyển đến màn hình Home
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new HomeFragment())
                .commit();
        });
    }

    private void loadFavorites() {
        if (!isAdded() || binding == null) return;

        binding.swipeRefreshLayout.setRefreshing(true);
        
        String url = SERVER.get_favorites;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded() || binding == null) return;
                    
                    binding.swipeRefreshLayout.setRefreshing(false);
                    handleFavoriteResponse(response);
                },
                error -> {
                    if (!isAdded() || binding == null) return;
                    
                    binding.swipeRefreshLayout.setRefreshing(false);
                    showError("Không thể tải danh sách yêu thích");
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

    private void handleFavoriteResponse(JSONObject response) {
        try {
            JSONArray items = response.getJSONArray("items");
            List<FavoriteItem> favoriteItems = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                
                // Lấy thông tin promotion nếu có
                JSONObject promotionObj = item.optJSONObject("promotion");
                FavoriteItem.Product.Promotion promotion = null;
                if (promotionObj != null) {
                    promotion = new FavoriteItem.Product.Promotion(
                        promotionObj.getInt("discountPercent"),
                        promotionObj.getString("endDate"),
                        promotionObj.getDouble("finalPrice")
                    );
                }

                // Tạo đối tượng Product
                FavoriteItem.Product product = new FavoriteItem.Product(
                    item.getInt("productID"),
                    item.getString("name"),
                    item.getDouble("price"),
                    item.getString("thumbnail"),
                    promotion
                );

                // Tạo FavoriteItem
                FavoriteItem favoriteItem = new FavoriteItem(
                    item.getString("_id"),
                    item.getInt("favoriteID"),
                    product,
                    item.optString("note", "")
                );

                favoriteItems.add(favoriteItem);
            }

            // Cập nhật adapter
            adapter.setItems(favoriteItems);
            
            // Cập nhật UI dựa trên số lượng items
            updateEmptyState(favoriteItems.isEmpty());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Có lỗi xảy ra khi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
            updateEmptyState(true);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        if (binding != null) {
            binding.recyclerViewFavorite.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onRemoveClicked(int favoriteId) {
        String url = SERVER.delete_favorite.replace(":id", String.valueOf(favoriteId));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> loadFavorites(),
                error -> showError("Không thể xóa sản phẩm yêu thích")
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
    public void onNoteChanged(int favoriteId, String note) {
        String url = SERVER.update_favorite.replace(":id", String.valueOf(favoriteId));

        JSONObject params = new JSONObject();
        try {
            params.put("note", note);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // Hiển thị loading indicator nếu cần
        binding.swipeRefreshLayout.setRefreshing(true);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, params,
                response -> {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(requireContext(), "Đã lưu ghi chú", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    showError("Không thể cập nhật ghi chú");
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
    public void onItemClicked(FavoriteItem item) {
        Bundle bundle = new Bundle();
        bundle.putString("productId", String.valueOf(item.getProduct().getProductID()));
        
        ProductDetailFragment productDetailFragment = new ProductDetailFragment();
        productDetailFragment.setArguments(bundle);
        
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.frameLayout, productDetailFragment)
            .addToBackStack(null)
            .commit();
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
}