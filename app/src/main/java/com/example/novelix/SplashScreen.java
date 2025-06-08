package com.example.novelix;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {
    private ImageView imageViewLogo;
    private static final int TOTAL_SPLASH_TIME = 2500; // Full screen duration in ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Cinematic fade in
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        imageViewLogo = findViewById(R.id.imageViewLogo);

        // Load cinematic splash animation
        Animation splashAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_zoom);
        imageViewLogo.startAnimation(splashAnimation);

        splashAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Optional: Add sound effect here if needed
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(() -> {
                    Intent intent = new Intent(SplashScreen.this, WelcomeActivity.class);
                    startActivity(intent);

                    // Apply fade transitions to keep cinematic mood
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                    finish();
                }, 300); // short pause for smoother transition
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // not used
            }
        });
    }
}
