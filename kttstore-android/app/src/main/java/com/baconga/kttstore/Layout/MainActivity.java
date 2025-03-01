package com.baconga.kttstore.Layout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.baconga.kttstore.Authentication.LoginActivity;
import com.baconga.kttstore.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("KTTStore", MODE_PRIVATE);

        // Hiển thị thông tin người dùng
        String fullname = sharedPreferences.getString("fullname", "");
        binding.tvWelcome.setText("Xin chào, " + fullname + "!");

        // Xử lý đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            // Xóa dữ liệu SharedPreferences
            sharedPreferences.edit().clear().apply();

            // Chuyển về màn hình đăng nhập
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}