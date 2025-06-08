package com.example.novelix;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ProgressBar mainProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainProgressBar = findViewById(R.id.mainProgressBar);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainerView2);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                NavigationUI.setupWithNavController(bottomNavigationView, navController);
            } else {
                // Handle null case gracefully
                Toast.makeText(this, "Navigation host not found", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "NavHostFragment is null");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing navigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Exception: ", e);
        }

    }
}