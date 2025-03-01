package com.baconga.kttstore.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.baconga.kttstore.databinding.ActivityContactBinding;

public class ContactActivity extends AppCompatActivity {
    
    private ActivityContactBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup listeners
        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Gọi điện thoại
        binding.btnPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:0123456789")); // Thay số điện thoại thực tế
            startActivity(intent);
        });

        // Gửi email
        binding.btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@kttstore.com")); // Thay email thực tế
            intent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ KTT Store");
            try {
                startActivity(intent);
            } catch (Exception e) {
                showError("Không tìm thấy ứng dụng email");
            }
        });

        // Mở Facebook
        binding.btnFacebook.setOnClickListener(v -> {
            String facebookUrl = "https://www.facebook.com/kttstore"; // Thay URL Facebook thực tế
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("fb://facewebmodal/f?href=" + facebookUrl));
                startActivity(intent);
            } catch (Exception e) {
                // Mở bằng trình duyệt nếu không có ứng dụng Facebook
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(facebookUrl));
                startActivity(intent);
            }
        });

        // Mở Zalo
        binding.btnZalo.setOnClickListener(v -> {
            String zaloUrl = "https://zalo.me/0123456789"; // Thay số Zalo thực tế
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(zaloUrl));
                startActivity(intent);
            } catch (Exception e) {
                showError("Không tìm thấy ứng dụng Zalo");
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 