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
import com.baconga.kttstore.databinding.ActivityAddressEditBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AddressEditActivity extends AppCompatActivity {
    private ActivityAddressEditBinding binding;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private long addressID = -1; // -1 là thêm mới, khác -1 là sửa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);

        // Lấy addressID nếu có (trường hợp sửa)
        addressID = getIntent().getLongExtra("addressID", -1);
        if (addressID != -1) {
            binding.txtTitle.setText("Sửa địa chỉ");
            loadAddressDetails();
        }

        // Setup listeners
        setupListeners();
    }

    private void setupListeners() {
        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút lưu
        binding.btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                if (addressID == -1) {
                    addAddress();
                } else {
                    updateAddress();
                }
            }
        });

        // Clear error khi người dùng nhập
        binding.edtAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.tilAddress.setError(null);
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;

        String address = binding.edtAddress.getText().toString().trim();
        if (address.isEmpty()) {
            binding.tilAddress.setError("Vui lòng nhập địa chỉ");
            isValid = false;
        }

        return isValid;
    }

    private void loadAddressDetails() {
        String url = SERVER.addresses + "/" + addressID;
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONObject address = response.getJSONObject("address");
                    binding.edtAddress.setText(address.getString("address"));
                    binding.cbDefault.setChecked(address.getBoolean("isDefault"));
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

    private void addAddress() {
        String url = SERVER.addresses;
        String token = sharedPreferences.getString("token", "");

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("address", binding.edtAddress.getText().toString().trim());
            requestBody.put("isDefault", binding.cbDefault.isChecked());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
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

    private void updateAddress() {
        String url = SERVER.addresses + "/" + addressID;
        String token = sharedPreferences.getString("token", "");

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("address", binding.edtAddress.getText().toString().trim());
            requestBody.put("isDefault", binding.cbDefault.isChecked());
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