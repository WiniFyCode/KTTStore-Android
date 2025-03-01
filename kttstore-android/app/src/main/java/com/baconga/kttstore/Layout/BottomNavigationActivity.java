package com.baconga.kttstore.Layout;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.baconga.kttstore.MenuFragment.CartFragment;
import com.baconga.kttstore.MenuFragment.FavoriteFragment;
import com.baconga.kttstore.MenuFragment.HomeFragment;
import com.baconga.kttstore.MenuFragment.NotificationFragment;
import com.baconga.kttstore.MenuFragment.ProfileFragment;
import com.baconga.kttstore.R;
import com.baconga.kttstore.databinding.ActivityBottomNavigationBinding;
import com.baconga.kttstore.MenuFragment.OrderFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationActivity extends AppCompatActivity {
    private ActivityBottomNavigationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBottomNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hiển thị HomeFragment khi mở app
        loadFragment(new HomeFragment());

        // Xử lý sự kiện khi chọn item trong bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            
            if (item.getItemId() == R.id.home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.cart) {
                fragment = new CartFragment();
            } else if (item.getItemId() == R.id.order) {
                fragment = new OrderFragment();
            } else if (item.getItemId() == R.id.notification) {
                fragment = new NotificationFragment();
            } else if (item.getItemId() == R.id.profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        // Thay thế fragment hiện tại bằng fragment mới
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}