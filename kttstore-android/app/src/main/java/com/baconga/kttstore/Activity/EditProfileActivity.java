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
import com.baconga.kttstore.databinding.ActivityEditProfileBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);

        // Load thông tin hiện tại
        loadCurrentInfo();

        // Setup listeners
        setupListeners();
    }

    private void loadCurrentInfo() {
        // Load từ SharedPreferences
        binding.edtFullname.setText(sharedPreferences.getString("fullname", ""));
        binding.edtPhone.setText(sharedPreferences.getString("phone", ""));
        binding.txtEmail.setText(sharedPreferences.getString("email", ""));

        // Set giới tính
        String gender = sharedPreferences.getString("gender", "");
        switch (gender.toLowerCase()) {
            case "male":
                binding.rbMale.setChecked(true);
                break;
            case "female":
                binding.rbFemale.setChecked(true);
                break;
            case "other":
                binding.rbOther.setChecked(true);
                break;
        }
    }

    private void setupListeners() {
        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút lưu
        binding.btnSave.setOnClickListener(v -> {
            // Validate input
            if (!validateInput()) {
                return;
            }

            // Lưu thông tin
            updateProfile();
        });

        // Clear error khi người dùng nhập
        binding.edtFullname.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilFullname.setError(null);
            }
        });

        binding.edtPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilPhone.setError(null);
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;

        // Validate họ tên
        String fullname = binding.edtFullname.getText().toString().trim();
        if (fullname.isEmpty()) {
            binding.tilFullname.setError("Vui lòng nhập họ tên");
            isValid = false;
        }

        // Validate số điện thoại
        String phone = binding.edtPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            binding.tilPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else if (!phone.matches("^[0-9]{10}$")) {
            binding.tilPhone.setError("Số điện thoại không hợp lệ");
            isValid = false;
        }

        return isValid;
    }

    private void updateProfile() {
        String url = SERVER.update_profile;
        String token = sharedPreferences.getString("token", "");

        // Chuyển đổi giới tính sang định dạng của server
        String gender;
        int selectedId = binding.rgGender.getCheckedRadioButtonId();
        if (selectedId == binding.rbMale.getId()) {
            gender = "male";
        } else if (selectedId == binding.rbFemale.getId()) {
            gender = "female";
        } else {
            gender = "other";
        }

        // Tạo request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("fullname", binding.edtFullname.getText().toString().trim());
            requestBody.put("phone", binding.edtPhone.getText().toString().trim());
            requestBody.put("gender", gender);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestBody,
            response -> {
                try {
                    // Hiển thị thông báo
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    // Lưu thông tin mới
                    JSONObject user = response.getJSONObject("user");
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("fullname", user.getString("fullname"));
                    editor.putString("phone", user.getString("phone"));
                    editor.putString("gender", user.getString("gender"));
                    editor.apply();

                    // Đóng màn hình
                    finish();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                // Hiển thị lỗi từ server nếu có
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