package com.baconga.kttstore.MenuFragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baconga.kttstore.Activity.EditProfileActivity;
import com.baconga.kttstore.Authentication.LoginActivity;
import com.baconga.kttstore.Models.User;
import com.baconga.kttstore.R;
import com.baconga.kttstore.SERVER;
import com.baconga.kttstore.Activity.AddressListActivity;
import com.baconga.kttstore.Activity.ChangePasswordActivity;
import com.baconga.kttstore.Activity.ContactActivity;
import com.baconga.kttstore.Activity.VoucherActivity;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private View view;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private User currentUser;

    // Views
    private TextView txtFullname, txtEmail, txtPhone, txtGender;
    private View btnEdit, btnAddress, btnChangePassword, btnContact, btnVoucher, btnLogout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Khởi tạo
        requestQueue = Volley.newRequestQueue(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("KTTStore", requireContext().MODE_PRIVATE);

        // Ánh xạ views
        txtFullname = view.findViewById(R.id.txtFullname);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtPhone = view.findViewById(R.id.txtPhone);
        txtGender = view.findViewById(R.id.txtGender);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnAddress = view.findViewById(R.id.btnAddress);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnContact = view.findViewById(R.id.btnContact);
        btnVoucher = view.findViewById(R.id.btnVoucher);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Setup listeners
        setupListeners();

        // Load thông tin người dùng
        loadUserProfile();

        return view;
    }

    private void setupListeners() {
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnAddress.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddressListActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ContactActivity.class);
            startActivity(intent);
        });

        btnVoucher.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VoucherActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        String url = SERVER.get_profile;
        String token = sharedPreferences.getString("token", "");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    // Parse thông tin user
                    currentUser = new User(
                        response.getLong("userID"),
                        response.getString("fullname"),
                        response.getString("email"),
                        response.getString("phone"),
                        response.getString("gender"),
                        response.getString("role"),
                        response.getBoolean("isDisabled")
                    );

                    // Hiển thị thông tin
                    updateUI();

                } catch (JSONException e) {
                    e.printStackTrace();
                    showError("Có lỗi xảy ra khi tải thông tin");
                }
            },
            error -> showError("Không thể tải thông tin người dùng")) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void updateUI() {
        if (currentUser != null) {
            txtFullname.setText(currentUser.getFullname());
            txtEmail.setText(currentUser.getEmail());
            txtPhone.setText(currentUser.getPhone());
            
            // Chuyển đổi giới tính từ mã sang text hiển thị
            String genderCode = currentUser.getGender();
            String genderText;
            switch (genderCode.toLowerCase()) {
                case "male":
                    genderText = "Nam";
                    break;
                case "female":
                    genderText = "Nữ";
                    break;
                default:
                    genderText = "Khác";
                    break;
            }
            txtGender.setText(genderText);
        }
    }

    private void logout() {
        // Xóa token và thông tin người dùng
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}