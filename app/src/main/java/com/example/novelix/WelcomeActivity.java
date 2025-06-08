package com.example.novelix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.novelix.Adapter.WelcomePagerAdapter;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;

public class WelcomeActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private SpringDotsIndicator dotsIndicator;
    private Button btnSkip;
    private ImageView btnBack, btnNext;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        preferenceManager = new PreferenceManager(this);

        // Check if onboarding is already complete
        if (preferenceManager.isOnboardingComplete()) {
            startMainActivity();
            return;
        }
        setContentView(R.layout.activity_welcome);

        initializeViews();
        setupViewPager();
        setupClickListeners();
    }
    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        btnBack = findViewById(R.id.btnBack);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupViewPager() {
        WelcomePagerAdapter adapter = new WelcomePagerAdapter(this);
        viewPager.setAdapter(adapter);
        dotsIndicator.setViewPager2(viewPager);

        // Hide back button on first page
        btnBack.setVisibility(View.GONE);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUI(position);
            }
        });
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < viewPager.getAdapter().getItemCount() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });

        btnBack.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() > 0) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });

       btnSkip.setOnClickListener(view -> {
           Intent intent = new Intent(WelcomeActivity.this, LoginHome.class);
           startActivity(intent);
       });
    }

    private void updateUI(int position) {
        // Update back button visibility
        btnBack.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

        // Update next button icon if on last page
        if (position == viewPager.getAdapter().getItemCount() - 1) {
            btnNext.setImageResource(R.drawable.check); // You might want to add a checkmark icon
        } else {
            btnNext.setImageResource(R.drawable.arrow_right);
        }

        // Update skip button visibility (hide on last page)
        btnSkip.setVisibility(position == viewPager.getAdapter().getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    public void startMainActivity() {
        preferenceManager.setOnboardingComplete(true);
        startActivity(new Intent(WelcomeActivity.this, LoginHome.class));
        finish();
    }

    public void startLoginHomeActivity() {
        // Mark onboarding as complete
        preferenceManager.setOnboardingComplete(true);
        startActivity(new Intent(WelcomeActivity.this, LoginHome.class));
        finish();
    }
}