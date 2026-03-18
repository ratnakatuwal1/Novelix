package com.example.novelix;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Terms extends AppCompatActivity {
    private WebView terms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terms);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        terms = findViewById(R.id.terms);

        String htmlFilePath = getIntent().getStringExtra("html_file");
        if (htmlFilePath != null) {
            terms.loadUrl(htmlFilePath);
        } else {
            terms.loadUrl("file:///android_asset/terms.html");
        }

        terms.getSettings().setJavaScriptEnabled(true);
    }
}