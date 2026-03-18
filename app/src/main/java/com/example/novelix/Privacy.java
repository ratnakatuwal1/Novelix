package com.example.novelix;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Privacy extends AppCompatActivity {
    private WebView privacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacy);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        privacy = findViewById(R.id.privacy);

        String htmlFilePath = getIntent().getStringExtra("html_file");
        if (htmlFilePath != null) {
            privacy.loadUrl(htmlFilePath);
        } else {
            privacy.loadUrl("file:///android_asset/privacy.html");
        }

        privacy.getSettings().setJavaScriptEnabled(true);
    }
}