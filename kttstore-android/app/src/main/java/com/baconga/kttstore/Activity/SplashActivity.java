package com.baconga.kttstore.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.baconga.kttstore.Adapters.OnboardingAdapter;
import com.baconga.kttstore.Authentication.LoginActivity;
import com.baconga.kttstore.Models.OnboardingItem;
import com.baconga.kttstore.R;
import com.baconga.kttstore.Utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private ImageButton btnNext;
    private TextView tvSkip;
    private OnboardingAdapter onboardingAdapter;
    private List<OnboardingItem> onboardingItems;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo PreferenceManager
        preferenceManager = new PreferenceManager(this);

        // Kiểm tra xem có phải lần đầu mở app không
        if (!preferenceManager.isFirstTimeLaunch()) {
            // Nếu không phải lần đầu, chuyển thẳng đến LoginActivity
            startLoginActivity();
            finish();
            return;
        }

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
        
        // Slide 1: Tìm kiếm sản phẩm
        onboardingItems.add(new OnboardingItem(
            R.drawable.onboarding_search,
            "Tìm kiếm sản phẩm dễ dàng",
            "Hàng nghìn sản phẩm được phân loại rõ ràng, giúp bạn tìm kiếm nhanh chóng"
        ));

        // Slide 2: Giỏ hàng
        onboardingItems.add(new OnboardingItem(
            R.drawable.onboarding_cart,
            "Mua sắm tiện lợi",
            "Thêm vào giỏ hàng hoặc lưu vào danh sách yêu thích để mua sau"
        ));

        // Slide 3: Thanh toán
        onboardingItems.add(new OnboardingItem(
            R.drawable.onboarding_payment,
            "Thanh toán an toàn",
            "Đa dạng phương thức thanh toán, bảo mật thông tin tuyệt đối"
        ));

        // Slide 4: Giao hàng
        onboardingItems.add(new OnboardingItem(
            R.drawable.onboarding_delivery,
            "Giao hàng nhanh chóng",
            "Đội ngũ giao hàng chuyên nghiệp, theo dõi đơn hàng mọi lúc"
        ));
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
        // Đánh dấu đã xem onboarding
        preferenceManager.setFirstTimeLaunch(false);
        
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