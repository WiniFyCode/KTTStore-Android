package com.baconga.kttstore.Authentication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Layout.BottomNavigationActivity;
import com.baconga.kttstore.Layout.MainActivity;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.ActivityLoginBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);

        // Kiểm tra token
        if (sharedPreferences.contains("token")) {
            verifyToken();
        }

        setupListeners();
    }

    private void verifyToken() {
        String token = sharedPreferences.getString("token", "");
        binding.progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, SERVER.verify_token, null,
            response -> {
                binding.progressBar.setVisibility(View.GONE);
                try {
                    if (response.getBoolean("success")) {
                        // Nếu có token mới, cập nhật token
                        if (response.has("newToken")) {
                            String newToken = response.getString("newToken");
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("token", newToken);
                            editor.apply();
                        }
                        
                        // Chuyển đến màn hình chính
                        startActivity(new Intent(LoginActivity.this, BottomNavigationActivity.class));
                        finish();
                    } else {
                        // Token không hợp lệ
                        clearUserData();
                        Toast.makeText(LoginActivity.this, "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    // Có lỗi khi parse JSON, nhưng không xóa token
                    startActivity(new Intent(LoginActivity.this, BottomNavigationActivity.class));
                    finish();
                }
            },
            error -> {
                binding.progressBar.setVisibility(View.GONE);
                
                // Kiểm tra response code
                if (error.networkResponse != null) {
                    int statusCode = error.networkResponse.statusCode;
                    
                    if (statusCode == 401) {
                        // Token hết hạn hoặc không hợp lệ
                        clearUserData();
                        Toast.makeText(LoginActivity.this, "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                    } else {
                        // Lỗi khác (server, mạng...), vẫn cho phép vào app
                        startActivity(new Intent(LoginActivity.this, BottomNavigationActivity.class));
                        finish();
                    }
                } else {
                    // Không có response (lỗi mạng), vẫn cho phép vào app
                    startActivity(new Intent(LoginActivity.this, BottomNavigationActivity.class));
                    finish();
                }
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

    private void login(String email, String password) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        // Hủy tất cả request cũ trước khi tạo request mới
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER.login, requestBody,
            response -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);

                try {
                    // Luôn hiển thị message từ server
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    if (response.getBoolean("success")) {
                        // Lưu token
                        String token = response.getString("token");
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("token", token);
                        
                        // Lưu thông tin user
                        JSONObject user = response.getJSONObject("user");
                        editor.putLong("userID", user.getLong("userID"));
                        editor.putString("fullname", user.getString("fullname"));
                        editor.putString("email", user.getString("email")); 
                        editor.putString("phone", user.getString("phone"));
                        editor.putString("gender", user.getString("gender"));
                        editor.putString("role", user.getString("role"));
                        editor.apply();

                        // Chuyển đến MainActivity
                        startActivity(new Intent(this, BottomNavigationActivity.class));
                        finish();
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
                
                // Hiển thị lỗi từ server nếu có
                try {
                    String errorMessage = new String(error.networkResponse.data, "UTF-8");
                    JSONObject errorJson = new JSONObject(errorMessage);
                    Toast.makeText(this, errorJson.getString("message"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });

        requestQueue.add(request);
    }

    private void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void setupListeners() {
        // Xử lý đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.edtEmail.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();

            // Validate input
            if (email.isEmpty()) {
                binding.tilEmail.setError("Vui lòng nhập email");
                return;
            }
            if (password.isEmpty()) {
                binding.tilPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }

            // Gọi API đăng nhập
            login(email, password);
        });

        // Chuyển đến màn hình đăng ký
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Chuyển đến màn hình quên mật khẩu
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Clear error khi người dùng nhập
        binding.edtEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilEmail.setError(null);
            }
        });

        binding.edtPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilPassword.setError(null);
            }
        });
    }
}