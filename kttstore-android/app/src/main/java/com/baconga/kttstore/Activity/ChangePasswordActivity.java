package com.baconga.kttstore.Activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.ActivityChangePasswordBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);

        // Setup listeners
        setupListeners();
    }

    private void setupListeners() {
        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút lưu
        binding.btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                changePassword();
            }
        });

        // Clear error khi người dùng nhập
        binding.edtCurrentPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilCurrentPassword.setError(null);
            }
        });

        binding.edtNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilNewPassword.setError(null);
            }
        });

        binding.edtConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilConfirmPassword.setError(null);
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;

        // Validate mật khẩu hiện tại
        String currentPassword = binding.edtCurrentPassword.getText().toString().trim();
        if (currentPassword.isEmpty()) {
            binding.tilCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            isValid = false;
        }

        // Validate mật khẩu mới
        String newPassword = binding.edtNewPassword.getText().toString().trim();
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            isValid = false;
        } else if (newPassword.length() < 6) {
            binding.tilNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        // Validate xác nhận mật khẩu
        String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu mới");
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            binding.tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        return isValid;
    }

    private void changePassword() {
        String url = SERVER.change_password;
        String token = sharedPreferences.getString("token", "");

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("currentPassword", binding.edtCurrentPassword.getText().toString().trim());
            requestBody.put("newPassword", binding.edtNewPassword.getText().toString().trim());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestBody,
            response -> {
                try {
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                try {
                    String errorMessage = new String(error.networkResponse.data, "UTF-8");
                    JSONObject errorJson = new JSONObject(errorMessage);
                    Toast.makeText(this, errorJson.getString("message"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
} 