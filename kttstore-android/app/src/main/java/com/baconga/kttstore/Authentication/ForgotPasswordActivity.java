package com.baconga.kttstore.Authentication;

import android.content.Intent;
import android.os.Bundle;
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
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.ActivityForgotPasswordBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;
    private String emailForReset;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestQueue = Volley.newRequestQueue(this);
        setupListeners();
    }

    private void sendOTP(String email) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSendOTP.setEnabled(false);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // Hủy tất cả request cũ
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER.forgot_password, requestBody,
            response -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSendOTP.setEnabled(true);

                try {
                    // Hiển thị message từ server
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    // Chỉ hiện form nhập OTP nếu email được trả về
                    if (response.has("email")) {
                        emailForReset = response.getString("email");
                        binding.tilOTP.setVisibility(View.VISIBLE);
                        binding.tilNewPassword.setVisibility(View.VISIBLE);
                        binding.btnSendOTP.setVisibility(View.GONE);
                        binding.btnResetPassword.setVisibility(View.VISIBLE);
                        binding.tilEmail.setEnabled(false);
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSendOTP.setEnabled(true);
                
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

    private void resetPassword(String email, String otp, String newPassword) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnResetPassword.setEnabled(false);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("otp", otp);
            requestBody.put("newPassword", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER.reset_password, requestBody,
            response -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnResetPassword.setEnabled(true);

                try {
                    // Hiển thị message từ server
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    
                    // Nếu thành công thì chuyển về trang đăng nhập
                    if (response.has("success") && response.getBoolean("success")) {
                        // Chuyển về trang đăng nhập
                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                        finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnResetPassword.setEnabled(true);
                
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

    private void setupListeners() {
        // Gửi OTP
        binding.btnSendOTP.setOnClickListener(v -> {
            String email = binding.edtEmail.getText().toString().trim();
            if (email.isEmpty()) {
                binding.tilEmail.setError("Vui lòng nhập email");
                return;
            }
            sendOTP(email);
        });

        // Đặt lại mật khẩu
        binding.btnResetPassword.setOnClickListener(v -> {
            String otp = binding.edtOTP.getText().toString().trim();
            String newPassword = binding.edtNewPassword.getText().toString().trim();

            if (otp.isEmpty()) {
                binding.tilOTP.setError("Vui lòng nhập mã OTP");
                return;
            }
            if (newPassword.isEmpty()) {
                binding.tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
                return;
            }

            resetPassword(emailForReset, otp, newPassword);
        });

        // Quay lại đăng nhập
        binding.tvBackToLogin.setOnClickListener(v -> finish());

        // Clear error khi focus
        binding.edtEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilEmail.setError(null);
        });

        binding.edtOTP.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilOTP.setError(null);
        });

        binding.edtNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilNewPassword.setError(null);
        });
    }
} 