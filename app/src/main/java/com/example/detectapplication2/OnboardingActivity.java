package com.example.detectapplication2;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageButton btnNext;
    private LinearLayout layoutDots;
    private List<Fragment> fragmentList;
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        layoutDots = findViewById(R.id.layoutDots);

        setupOnboardingFragments();
        setupViewPager();
        setupButtons();
    }

    private void setupOnboardingFragments() {
        fragmentList = new ArrayList<>();
        fragmentList.add(OnboardingFragment.newInstance(
                "Welcome to Potholes Detection",
                "A visual basic application to advance your driving and ensure traffic safety.",
                R.drawable.logo));
        fragmentList.add(OnboardingFragment.newInstance(
                "Pothole Detection",
                "Automatically detect and report potholes within a 1km radius while driving. Our app uses advanced AI to identify road hazards.",
                R.drawable.winter_road_cuate));
        fragmentList.add(OnboardingFragment.newInstance(
                "Map Visualization",
                "View routes along with pothole locations on each path. Easily plan safer journeys with real-time hazard mapping.",
                R.drawable.paper_map_bro));
        fragmentList.add(OnboardingFragment.newInstance(
                "Live Weather Updates",
                "Stay informed with continuous weather updates based on your location. Drive safely under any condition.",
                R.drawable.weather_intro));
    }


    private void setupViewPager() {
        OnboardingAdapter adapter = new OnboardingAdapter(this, fragmentList);
        viewPager.setAdapter(adapter);
        
        setupDots(fragmentList.size());
        selectDot(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                selectDot(position);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtons() {
        btnNext.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btnNext, "scaleX", 0.9f);
                    ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btnNext, "scaleY", 0.9f);
                    scaleDownX.setDuration(100);
                    scaleDownY.setDuration(100);
                    scaleDownX.start();
                    scaleDownY.start();
                    break;

                case MotionEvent.ACTION_UP:
                    ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btnNext, "scaleX", 1f);
                    ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btnNext, "scaleY", 1f);
                    scaleUpX.setDuration(100);
                    scaleUpY.setDuration(100);
                    scaleUpX.start();
                    scaleUpY.start();
                    
                    if (currentIndex < fragmentList.size() - 1) {
                        viewPager.setCurrentItem(currentIndex + 1, true);
                    } else {
                        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
                        finish();
                    }
                    break;
                
                case MotionEvent.ACTION_CANCEL:
                    ObjectAnimator scaleNormalX = ObjectAnimator.ofFloat(btnNext, "scaleX", 1f);
                    ObjectAnimator scaleNormalY = ObjectAnimator.ofFloat(btnNext, "scaleY", 1f);
                    scaleNormalX.setDuration(100);
                    scaleNormalY.setDuration(100);
                    scaleNormalX.start();
                    scaleNormalY.start();
                    break;
            }
            return true;
        });
    }

    private void setupDots(int count) {
        layoutDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(24);
            dot.setTextColor(Color.LTGRAY);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(12, 0, 12, 0);
            dot.setLayoutParams(params);
            
            layoutDots.addView(dot);
        }
    }

    private void selectDot(int index) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            TextView dot = (TextView) layoutDots.getChildAt(i);
            if (i == index) {
                dot.setTextColor(Color.WHITE);
                dot.setTextSize(24);
            } else {
                dot.setTextColor(Color.LTGRAY);
                dot.setTextSize(24);
            }
        }
    }
}

