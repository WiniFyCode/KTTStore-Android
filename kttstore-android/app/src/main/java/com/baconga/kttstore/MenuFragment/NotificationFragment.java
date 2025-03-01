package com.baconga.kttstore.MenuFragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.NotificationAdapter;
import com.baconga.kttstore.Models.Notification;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class NotificationFragment extends Fragment implements NotificationAdapter.NotificationListener {
    private View view;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NotificationAdapter adapter;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;
    private View emptyStateLayout;
    private View btnMarkAllRead;
    private TextView txtUnreadCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_notification, container, false);
        
        // Khởi tạo
        requestQueue = Volley.newRequestQueue(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("KTTStore", requireContext().MODE_PRIVATE);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Ánh xạ views
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        txtUnreadCount = view.findViewById(R.id.txtUnreadCount);

        // Setup RecyclerView
        adapter = new NotificationAdapter(this);
        androidx.recyclerview.widget.RecyclerView recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            loadUnreadCount();
        });

        // Setup button mark all as read
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        // Load dữ liệu
        loadNotifications();
        loadUnreadCount();

        return view;
    }

    private void loadNotifications() {
        String url = SERVER.get_notifications;
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                swipeRefreshLayout.setRefreshing(false);
                try {
                    JSONArray notificationsArray = response.getJSONArray("notifications");
                    List<Notification> notifications = new ArrayList<>();

                    for (int i = 0; i < notificationsArray.length(); i++) {
                        JSONObject obj = notificationsArray.getJSONObject(i);
                        notifications.add(new Notification(
                            String.valueOf(obj.getInt("notificationID")),
                            obj.getString("title"),
                            obj.getString("type"),
                            obj.getString("message"),
                            obj.getInt("readCount"),
                            dateFormat.parse(obj.getString("scheduledFor")),
                            dateFormat.parse(obj.getString("expiresAt")),
                            dateFormat.parse(obj.getString("createdAt")),
                            obj.getString("createdBy"),
                            obj.getBoolean("isRead"),
                            obj.has("readAt") && !obj.isNull("readAt") ? 
                                dateFormat.parse(obj.getString("readAt")) : null,
                            String.valueOf(obj.getInt("userNotificationID"))
                        ));
                    }

                    adapter.setNotifications(notifications);
                    updateEmptyState(notifications.isEmpty());

                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                    showError("Có lỗi xảy ra khi tải thông báo");
                }
            },
            error -> {
                swipeRefreshLayout.setRefreshing(false);
                showError("Không thể tải thông báo");
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

    private void loadUnreadCount() {
        String url = SERVER.get_unread_count;
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    int count = response.getInt("count");
                    updateUnreadCount(count);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                // Xử lý lỗi nếu cần
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

    private void updateUnreadCount(int count) {
        if (count > 0) {
            txtUnreadCount.setVisibility(View.VISIBLE);
            txtUnreadCount.setText(String.valueOf(count));
        } else {
            txtUnreadCount.setVisibility(View.GONE);
        }
    }

    private void markAsRead(String userNotificationId) {
        String url = SERVER.mark_as_read.replace(":id", userNotificationId);
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, null,
            response -> loadNotifications(), // Tải lại danh sách sau khi đánh dấu đã đọc
            error -> showError("Không thể đánh dấu đã đọc")) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void markAllAsRead() {
        String url = SERVER.mark_all_as_read;
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, null,
            response -> {
                Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                loadNotifications();
                loadUnreadCount();
            },
            error -> showError("Không thể đánh dấu tất cả đã đọc")) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    @Override
    public void onNotificationClicked(Notification notification) {
        if (!notification.isRead()) {
            markAsRead(notification.getUserNotificationID());
            loadUnreadCount();
        }
        // TODO: Xử lý click vào thông báo (mở màn hình liên quan)
    }

    private void updateEmptyState(boolean isEmpty) {
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnMarkAllRead.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}