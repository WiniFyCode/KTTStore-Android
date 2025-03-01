package com.baconga.kttstore.MenuFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.OrderAdapter;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.Models.MOrder;
import com.baconga.kttstore.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.DefaultRetryPolicy;

public class OrderFragment extends Fragment {
    // Khai báo các biến UI
    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    
    // Khai báo các biến xử lý dữ liệu
    private RequestQueue requestQueue;
    private List<MOrder> orderList;
    private static final String TAG = "OrdersFragment";
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int ITEMS_PER_PAGE = 10;
    private boolean isLoading = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo RequestQueue và danh sách đơn hàng
        requestQueue = Volley.newRequestQueue(requireContext());
        orderList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout cho fragment
        View view = inflater.inflate(R.layout.fragment_order, container, false);
        
        // Khởi tạo các view
        initializeViews(view);
        
        // Thiết lập RecyclerView
        setupRecyclerView();
        
        // Thiết lập SwipeRefreshLayout
        setupSwipeRefresh();
        
        // Tải dữ liệu đơn hàng
        loadOrders(true); // true = refresh
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.ordersRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new OrderAdapter(requireContext(), orderList);
        
        // Thêm xử lý sự kiện click
        adapter.setOnOrderClickListener(orderId -> {
            // Chuyển đến màn hình chi tiết đơn hàng
            OrderDetailFragment detailFragment = new OrderDetailFragment(orderId);
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, detailFragment)
                .addToBackStack(null)
                .commit();
        });
        
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // Scroll down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && currentPage < totalPages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            loadMoreOrders();
                        }
                    } else if (currentPage >= totalPages) {
                        // Ẩn loading nếu đã load hết trang
                        adapter.setLoading(false);
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> loadOrders(true));
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
    }

    private void loadOrders(boolean isRefresh) {
        if (isRefresh) {
            currentPage = 1;
            orderList.clear();
            adapter.notifyDataSetChanged();
        }

        if (isLoading) return;
        isLoading = true;

        showLoading(true);
        String url = SERVER.get_orders + "?page=" + currentPage + "&limit=" + ITEMS_PER_PAGE;
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET, 
            url, 
            null,
            response -> {
                try {
                    JSONArray ordersArray = response.getJSONArray("orders");
                    totalPages = response.getInt("totalPages");
                    currentPage = response.getInt("currentPage");
                    
                    for (int i = 0; i < ordersArray.length(); i++) {
                        JSONObject orderObject = ordersArray.getJSONObject(i);
                        MOrder order = parseOrderFromJson(orderObject);
                        orderList.add(order);
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    
                } catch (JSONException e) {
                    showError("Có lỗi xảy ra khi tải đơn hàng");
                } finally {
                    isLoading = false;
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    // Ẩn loading khi đã load xong
                    adapter.setLoading(false);
                }
            },
            error -> {
                showError("Không thể tải danh sách đơn hàng");
                isLoading = false;
                showLoading(false);
                swipeRefreshLayout.setRefreshing(false);
                // Ẩn loading khi có lỗi
                adapter.setLoading(false);
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

    private void loadMoreOrders() {
        // Kiểm tra điều kiện trước khi load more
        if (isLoading || currentPage >= totalPages) {
            // Ẩn loading nếu đã load hết
            adapter.setLoading(false);
            return;
        }
        
        currentPage++;
        adapter.setLoading(true);
        loadOrders(false);
    }

    private MOrder parseOrderFromJson(JSONObject json) throws JSONException {
        MOrder order = new MOrder(
            json.getString("_id"),
            json.getInt("orderID"),
            json.getLong("userID"),
            json.getString("fullname"),
            json.getString("phone"),
            json.getString("address"),
            json.getDouble("totalPrice"),
            json.getDouble("paymentPrice"),
            json.getString("orderStatus"),
            json.getString("shippingStatus"),
            json.getBoolean("isPayed"),
            json.getString("createdAt"),
            json.getString("updatedAt")
        );
        
        // Log order được parse
        Log.d(TAG, String.format("Parsed Order: ID=%d, Total=%.2f, Status=%s", 
            order.getOrderID(), 
            order.getTotalPrice(), 
            order.getOrderStatus()));
        
        return order;
    }

    private void updateEmptyState() {
        boolean isEmpty = orderList.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "UI updated with " + orderList.size() + " orders");
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(isLoading);
    }

    private void showError(String message) {
        if (isAdded()) {
            // Hiển thị thông báo lỗi (có thể sử dụng Toast hoặc Snackbar)
            Log.e(TAG, message);
        }
    }

    private String getToken() {
        SharedPreferences prefs = requireContext().getSharedPreferences("KTTStore", Context.MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy tất cả các request đang pending
        if (requestQueue != null) {
            requestQueue.cancelAll(request -> true);
        }
    }
}