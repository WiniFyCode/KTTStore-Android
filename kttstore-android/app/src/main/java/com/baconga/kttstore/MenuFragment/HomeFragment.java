package com.baconga.kttstore.MenuFragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.FragmentHomeBinding;
import com.baconga.kttstore.Models.MBanner;
import com.baconga.kttstore.Adapters.BannerAdapter;
import com.baconga.kttstore.Adapters.ProductAdapter;
import com.baconga.kttstore.Adapters.CategoryAdapter;
import com.baconga.kttstore.Models.MCategory;
import com.baconga.kttstore.Models.MProduct;
import android.os.Handler;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.AuthFailureError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.DefaultRetryPolicy;
import android.util.Log;
import android.widget.ImageView;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private RequestQueue requestQueue;
    private BannerAdapter bannerAdapter;
    private ProductAdapter featuredAdapter, menProductAdapter, womenProductAdapter;
    private CategoryAdapter categoryAdapter;
    private List<MBanner> banners;
    private Handler bannerHandler;
    private Runnable bannerRunnable;
    private static final long BANNER_DELAY = 3000; // 3 giây
    private ImageView[] dots;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        requestQueue = Volley.newRequestQueue(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup favorite button
        binding.btnFavorite.setOnClickListener(v -> {
            // Kiểm tra đăng nhập
            SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
            String token = prefs.getString("token", "");
            
            if (token.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem danh sách yêu thích", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chuyển đến FavoriteFragment
            FavoriteFragment favoriteFragment = new FavoriteFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, favoriteFragment)
                .addToBackStack(null)
                .commit();
        });

        // Setup search card click
        binding.cardSearch.setOnClickListener(v -> {
            // Chuyển sang ProductListFragment với tất cả sản phẩm
            ProductListFragment fragment = ProductListFragment.newInstance(null, "Tất cả sản phẩm");
            getParentFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit();
        });

        setupBanners();
        setupRecyclerViews();
        loadData();
    }

    private void setupBanners() {
        // Tạo banner tĩnh
        banners = new ArrayList<>();
        
        // Banner chính
        banners.add(new MBanner(R.drawable.banner1, "Khuyến mãi mùa hè", "Giảm giá đến 50%"));
        banners.add(new MBanner(R.drawable.banner2, "Khuyến mãi mùa đông", "Giảm giá đến 30%"));
        banners.add(new MBanner(R.drawable.banner1, "Ưu đãi đặc biệt", "Freeship cho đơn từ 500K"));
        banners.add(new MBanner(R.drawable.banner2, "Bộ sưu tập mới", "Xu hướng thời trang 2025"));

        bannerAdapter = new BannerAdapter(requireContext(), banners);
        binding.viewPagerBanner.setAdapter(bannerAdapter);

        // Tắt animation khi scroll
        binding.viewPagerBanner.setOffscreenPageLimit(3);

        // Thêm dots indicator
        setupDots(0);

        // Cập nhật callback cho ViewPager
        binding.viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private boolean isScrolling = false;

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    isScrolling = false;
                } else if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isScrolling = true;
                }
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);
                if (!isScrolling) {
                    startAutoSlide();
                }
            }
        });

        // Auto slide banner
        startAutoSlide();
    }

    private void startAutoSlide() {
        if (bannerHandler == null) {
            bannerHandler = new Handler();
        }
        
        // Dừng auto slide cũ nếu có
        stopAutoSlide();

        // Tạo auto slide mới
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding != null && binding.viewPagerBanner != null && isAdded()) {
                    int currentItem = binding.viewPagerBanner.getCurrentItem();
                    int totalItems = banners.size();
                    
                    if (currentItem < totalItems - 1) {
                        binding.viewPagerBanner.setCurrentItem(currentItem + 1);
                    } else {
                        binding.viewPagerBanner.setCurrentItem(0);
                    }
                    
                    bannerHandler.postDelayed(this, BANNER_DELAY);
                }
            }
        };
        
        // Chỉ start auto slide nếu fragment vẫn attached
        if (isAdded() && binding != null) {
            bannerHandler.postDelayed(bannerRunnable, BANNER_DELAY);
        }
    }

    private void stopAutoSlide() {
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }

    private void setupRecyclerViews() {
        // Setup Category RecyclerView
        categoryAdapter = new CategoryAdapter(requireContext(), new ArrayList<>());
        categoryAdapter.setOnItemClickListener(category -> {
            // Chuyển sang màn hình chi tiết category
            CategoryDetailFragment fragment = CategoryDetailFragment.newInstance(category);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);

        // Setup Featured Products RecyclerView (Horizontal)
        featuredAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), product -> {
            // Navigate to ProductDetailFragment
            ProductDetailFragment fragment = ProductDetailFragment.newInstance(product.getProductID());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvFeaturedProducts.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeaturedProducts.setAdapter(featuredAdapter);

        // Setup Men Products RecyclerView (Grid)
        menProductAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), product -> {
            ProductDetailFragment fragment = ProductDetailFragment.newInstance(product.getProductID());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvMenProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvMenProducts.setAdapter(menProductAdapter);

        // Setup Women Products RecyclerView (Grid)
        womenProductAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), product -> {
            ProductDetailFragment fragment = ProductDetailFragment.newInstance(product.getProductID());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvWomenProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvWomenProducts.setAdapter(womenProductAdapter);

        // Thêm xử lý click cho nút "Tất cả" của sản phẩm Nam
        binding.btnMenSeeAll.setOnClickListener(v -> {
            ProductListFragment fragment = ProductListFragment.newInstance(1, "Dành cho Nam");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Thêm xử lý click cho nút "Tất cả" của sản phẩm Nữ
        binding.btnWomenSeeAll.setOnClickListener(v -> {
            ProductListFragment fragment = ProductListFragment.newInstance(2, "Dành cho Nữ");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadData() {
        loadCategories();
        loadFeaturedProducts();
        loadMenProducts();
        loadWomenProducts();
    }

    private void loadCategories() {
        String url = SERVER.get_categories;
        
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    List<MCategory> categoryList = new ArrayList<>();
                    
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject category = response.getJSONObject(i);
                        MCategory mCategory = new MCategory(
                            String.valueOf(category.getInt("categoryID")),
                            category.getString("name"),
                            category.optString("description", ""),
                            category.getString("imageURL")
                        );
                        categoryList.add(mCategory);
                    }
                    
                    if (isAdded() && getActivity() != null) {
                        categoryAdapter.updateData(categoryList);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            },
            error -> {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setShouldCache(false);
        
        if (requestQueue != null) {
            requestQueue.add(request);
        }
    }

    private void loadFeaturedProducts() {
        String url = SERVER.get_products + "?limit=10&hasPromotion=true&sort=newest";
        loadProducts(url, featuredAdapter);
    }

    private void loadMenProducts() {
        String url = SERVER.get_products + "?limit=6&targetID=1&sort=newest"; // Giả sử targetID=1 là Nam
        loadProducts(url, menProductAdapter);
    }

    private void loadWomenProducts() {
        String url = SERVER.get_products + "?limit=6&targetID=2&sort=newest"; // Giả sử targetID=2 là Nữ
        loadProducts(url, womenProductAdapter);
    }

    private void loadProducts(String url, ProductAdapter adapter) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray products = response.getJSONArray("products");
                    List<MProduct> productList = new ArrayList<>();

                    for (int i = 0; i < products.length(); i++) {
                        JSONObject product = products.getJSONObject(i);
                        MProduct mProduct = parseProduct(product);
                        productList.add(mProduct);
                    }

                    if (isAdded() && getActivity() != null) {
                        adapter.updateData(productList);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), 
                            "Lỗi tải dữ liệu: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            },
            error -> {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(requireContext(),
                        "Lỗi kết nối server", 
                        Toast.LENGTH_SHORT).show();
                }
            });

        request.setRetryPolicy(new DefaultRetryPolicy(
            5000,
            1,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        if (requestQueue != null) {
            requestQueue.add(request);
        }
    }

    private MProduct parseProduct(JSONObject json) throws JSONException {
        // Log toàn bộ JSON response
        Log.d("HomeFragment", "Raw JSON: " + json.toString());

        // Tạo đối tượng sản phẩm với thông tin cơ bản
        MProduct product = new MProduct(
            String.valueOf(json.getInt("productID")),
            json.getString("name"),
            "", // Không còn targetID trong response
            "", // Không còn description trong response
            json.getDouble("price"),
            "", // Không còn categoryID trong response
            json.getString("thumbnail"),
            json.getBoolean("isActivated")
        );

        // Đọc trạng thái tồn kho
        product.setInStock(json.getBoolean("inStock"));
        product.setTotalStock(json.getInt("totalStock"));

        // Xử lý thông tin khuyến mãi nếu có
        if (!json.isNull("promotion")) {
            JSONObject promotion = json.getJSONObject("promotion");
            // Log chi tiết về promotion
            Log.d("HomeFragment", "Promotion data for product " + product.getProductID() + ":");
            Log.d("HomeFragment", "Discount Percent: " + promotion.getInt("discountPercent"));
            Log.d("HomeFragment", "Final Price: " + promotion.getString("finalPrice"));
            
            product.setDiscountPercent(promotion.getInt("discountPercent"));
            double finalPrice = Double.parseDouble(promotion.getString("finalPrice"));
            product.setDiscountedPrice(finalPrice);
            
            // Log giá sau khi parse
            Log.d("HomeFragment", "Parsed final price: " + finalPrice);
        } else {
            Log.d("HomeFragment", "No promotion for product " + product.getProductID());
        }

        return product;
    }

    private void setupDots(int currentPage) {
        if (binding.layoutDots == null) return;
        
        binding.layoutDots.removeAllViews();
        dots = new ImageView[banners.size()];

        for (int i = 0; i < banners.size(); i++) {
            dots[i] = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            dots[i].setImageResource(R.drawable.dot_unselected);
            binding.layoutDots.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[currentPage].setImageResource(R.drawable.dot_selected);
        }
    }

    private void updateDots(int currentPage) {
        if (dots == null) return;
        
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null) {
                dots[i].setImageResource(i == currentPage ? 
                    R.drawable.dot_selected : 
                    R.drawable.dot_unselected
                );
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoSlide();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoSlide();
        binding = null;
    }
}