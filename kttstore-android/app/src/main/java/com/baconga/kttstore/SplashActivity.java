package com.baconga.kttstore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.baconga.kttstore.Adapters.OnboardingAdapter;
import com.baconga.kttstore.Authentication.LoginActivity;
import com.baconga.kttstore.Models.OnboardingItem;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private ImageButton btnNext;
    private TextView tvSkip;
    private OnboardingAdapter onboardingAdapter;
    private List<OnboardingItem> onboardingItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        setupOnboardingItems();
        setupViewPager();
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btn_next);
        tvSkip = findViewById(R.id.tv_skip);
    }

    private void setupOnboardingItems() {
        onboardingItems = new ArrayList<>();
        
        // Thêm các slide onboarding
        onboardingItems.add(new OnboardingItem(
            R.drawable.onboarding_search,
            "Find the item you've been looking for",
            "Here you'll see rich varieties of goods, carefully classified for seamless browsing experience."
        ));

        onboardingItems.add(new OnboardingItem(
            R.drawable.onboarding_cart,
            "Get those shopping bags filled",
            "Add any item you want to your cart, or save it on your wishlist, so you don't miss it in your future purchases."
        ));

        // Có thể thêm nhiều slide khác nếu cần
    }

    private void setupViewPager() {
        onboardingAdapter = new OnboardingAdapter(this, onboardingItems);
        viewPager.setAdapter(onboardingAdapter);
        addDots(0); // Hiển thị dots cho slide đầu tiên

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                addDots(position);
                
                // Nếu là slide cuối cùng, đổi text của nút Next thành Start
                if (position == onboardingItems.size() - 1) {
                    btnNext.setImageResource(R.drawable.ic_check);
                } else {
                    btnNext.setImageResource(R.drawable.ic_arrow_forward);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < onboardingItems.size() - 1) {
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                startLoginActivity();
            }
        });

        tvSkip.setOnClickListener(v -> startLoginActivity());
    }

    private void startLoginActivity() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void addDots(int currentPage) {
        TextView[] dots = new TextView[onboardingItems.size()];
        dotsLayout.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText("•");
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(
                i == currentPage ? R.color.primary : R.color.gray_light
            ));
            dotsLayout.addView(dots[i]);
        }
    }
} 