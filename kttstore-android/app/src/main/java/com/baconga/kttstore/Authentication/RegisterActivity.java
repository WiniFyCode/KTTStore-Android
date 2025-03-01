package com.baconga.kttstore.Authentication;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.baconga.kttstore.MenuFragment.HomeFragment;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.ActivityRegisterBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);

        setupListeners();
    }

    private void setupListeners() {
        // Xử lý đăng ký
        binding.btnRegister.setOnClickListener(v -> {
            String fullname = binding.edtFullname.getText().toString().trim();
            String email = binding.edtEmail.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();
            String phone = binding.edtPhone.getText().toString().trim();
            String gender = binding.rbMale.isChecked() ? "male" : "female";

            // Validate input
            if (fullname.isEmpty()) {
                binding.tilFullname.setError("Vui lòng nhập họ và tên");
                return;
            }
            if (email.isEmpty()) {
                binding.tilEmail.setError("Vui lòng nhập email");
                return;
            }
            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.setError("Email không hợp lệ");
                return;
            }
            if (password.isEmpty()) {
                binding.tilPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }
            if (phone.isEmpty()) {
                binding.tilPhone.setError("Vui lòng nhập số điện thoại");
                return;
            }

            // Validate password format
            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$")) {
                binding.tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự, 1 chữ hoa, 1 chữ thường và 1 số");
                return;
            }

            // Validate phone format
            if (!phone.matches("^(0[3|5|7|8|9])+([0-9]{8})\\b")) {
                binding.tilPhone.setError("Số điện thoại không hợp lệ");
                return;
            }

            // Gọi API đăng ký
            register(fullname, email, password, phone, gender);
        });

        // Chuyển đến màn hình đăng nhập
        binding.tvLogin.setOnClickListener(v -> {
            finish();
        });

        // Clear error khi người dùng nhập
        binding.edtFullname.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilFullname.setError(null);
            }
        });

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

        binding.edtPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilPhone.setError(null);
            }
        });
    }

    private void register(String fullname, String email, String password, String phone, String gender) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("fullname", fullname);
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("phone", phone);
            requestBody.put("gender", gender);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER.register, requestBody,
            response -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnRegister.setEnabled(true);

                try {
                    // Luôn hiển thị message từ server
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    if (response.getBoolean("success")) {
                        // Lưu token
                        String token = response.getString("token");
                        SharedPreferences.Editor editor = getSharedPreferences("auth", MODE_PRIVATE).edit();
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

                        // Chuyển đến HomeFragment
                        startActivity(new Intent(this, HomeFragment.class));
                        finishAffinity();
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "Có lỗi xử lý dữ liệu từ server", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnRegister.setEnabled(true);
                
                // Hiển thị lỗi từ server nếu có
                try {
                    String errorMessage = new String(error.networkResponse.data, "UTF-8");
                    JSONObject errorJson = new JSONObject(errorMessage);
                    
                    // Kiểm tra nếu có lỗi validation
                    if (errorJson.has("errors")) {
                        JSONObject errors = errorJson.getJSONObject("errors");
                        if (errors.has("email")) {
                            binding.tilEmail.setError(errors.getJSONObject("email").getString("message"));
                        }
                    } else {
                        // Hiển thị message chung nếu không có lỗi cụ thể
                        String message = errorJson.getString("message");
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi kết nối đến server", Toast.LENGTH_SHORT).show();
                }
            });

        requestQueue.add(request);
    }
}