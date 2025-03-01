package com.baconga.kttstore.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Adapters.AddressAdapter;
import com.baconga.kttstore.Models.Address;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.databinding.ActivityAddressListBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressListActivity extends AppCompatActivity implements AddressAdapter.OnAddressClickListener {
    private ActivityAddressListBinding binding;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private AddressAdapter adapter;
    private List<Address> addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);
        addresses = new ArrayList<>();
        adapter = new AddressAdapter(addresses, this);

        // Setup RecyclerView
        binding.recyclerAddress.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAddress.setAdapter(adapter);

        // Setup listeners
        setupListeners();

        // Load danh sách địa chỉ
        loadAddresses();
    }

    private void setupListeners() {
        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút thêm địa chỉ
        binding.btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressEditActivity.class);
            startActivity(intent);
        });
    }

    private void loadAddresses() {
        String url = SERVER.addresses;
        String token = sharedPreferences.getString("token", "");

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    addresses.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        Address address = new Address();
                        address.setAddressID(obj.getLong("addressID"));
                        address.setUserID(obj.getLong("userID"));
                        address.setAddress(obj.getString("address"));
                        address.setDefault(obj.getBoolean("isDefault"));
                        address.setDelete(obj.getBoolean("isDelete"));
                        address.setCreatedAt(obj.getString("createdAt"));
                        address.setUpdatedAt(obj.getString("updatedAt"));
                        addresses.add(address);
                    }
                    adapter.updateData(addresses);
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

    @Override
    public void onSetDefaultClick(Address address) {
        String url = SERVER.addresses + "/" + address.getAddressID() + "/default";
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PATCH, url, null,
            response -> {
                try {
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadAddresses(); // Reload danh sách
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

    @Override
    public void onEditClick(Address address) {
        Intent intent = new Intent(this, AddressEditActivity.class);
        intent.putExtra("addressID", address.getAddressID());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Address address) {
        String url = SERVER.addresses + "/" + address.getAddressID();
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
            response -> {
                try {
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadAddresses(); // Reload danh sách
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

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses(); // Reload khi quay lại màn hình
    }
} 