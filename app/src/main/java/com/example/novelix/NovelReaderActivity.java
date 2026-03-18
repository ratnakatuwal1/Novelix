package com.example.novelix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NovelReaderActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {
    private PDFView pdfView;
    private ProgressBar progressBar;
    private static final String TAG = "NovelReaderActivity";
    private String novelId;
    private String novelTitle;
    private String novelUrl;
    private static final String PREFS_NAME = "NovelixPrefs";
    private static final String KEY_READING_TIME = "reading_time_minutes";
    private int lastKnownPage = 0;
    private long startTime;
    private boolean isPdfLoaded = false;
    private final Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private static final long INACTIVITY_TIMEOUT = 2 * 60 * 1000;
    private long lastInteractionTime;

    private final Runnable inactivityRunnable = () -> {
        if (isPdfLoaded && startTime != 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInteractionTime >= INACTIVITY_TIMEOUT) {
                saveReadingTime(currentTime - startTime);
                startTime = 0;
                Log.d(TAG, "Inactivity detected, paused time tracking");
            }
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) && isPdfLoaded && startTime != 0) {
                long endTime = System.currentTimeMillis();
                saveReadingTime(endTime - startTime);
                startTime = 0;
                Log.d(TAG, "Screen off, saved reading time");
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()) && isPdfLoaded) {
                startTime = System.currentTimeMillis();
                lastInteractionTime = startTime;
                inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT);
                Log.d(TAG, "Screen on, resumed reading time");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_novel_reader);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pdfView = findViewById(R.id.pdfView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        novelUrl = intent.getStringExtra("novel_url");
        novelTitle = intent.getStringExtra("novel_title");
        novelId = intent.getStringExtra("novel_id");

        if (novelTitle != null && !novelTitle.isEmpty()) {
            setTitle("Reading: " + novelTitle);
        } else {
            setTitle("Reading Novel");
        }

        if (novelId != null && !novelId.isEmpty()) {
            loadNovelWithCache();
        } else {
            Toast.makeText(this, "Invalid Novel ID", Toast.LENGTH_LONG).show();
            finish();
        }

        // Register screen on/off receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);

        // REMOVED: Touch listener - let PDFView handle all touch events naturally
    }

    private void loadNovelWithCache() {
        File novelFile = new File(getDir("novels", Context.MODE_PRIVATE), novelId + ".pdf");

        if (novelFile.exists()) {
            Log.d(TAG, "Novel found in cache. Loading from local file.");
            loadPdfFromFile(novelFile);
        } else {
            Log.d(TAG, "Novel not in cache. Downloading from URL...");
            if (novelUrl != null && !novelUrl.isEmpty()) {
                downloadAndCacheNovel(novelUrl, novelFile);
            } else {
                Toast.makeText(this, "Invalid novel URL", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void loadPdfFromFile(File file) {
        int savedPage = restorePagePosition();
        Log.d(TAG, "Restored page: " + savedPage + " for novel " + novelId);

        pdfView.fromFile(file)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(savedPage)
                .onPageChange(this)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .enableAntialiasing(true)
                .spacing(0)
                .pageFitPolicy(FitPolicy.WIDTH)
                .fitEachPage(true)
                .pageSnap(true)
                .autoSpacing(true)
                .pageFling(true)
                .nightMode(false)
                .load();
    }

    private void downloadAndCacheNovel(String urlString, File destinationFile) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);
                byte[] buffer = new byte[1024];
                int bufferLength;
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, bufferLength);
                }
                fileOutputStream.close();
                inputStream.close();

                Log.d(TAG, "Download complete. File cached at: " + destinationFile.getAbsolutePath());

                runOnUiThread(() -> loadPdfFromFile(destinationFile));

            } catch (IOException e) {
                Log.e(TAG, "Error downloading or caching novel", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NovelReaderActivity.this, "Error downloading novel: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPdfLoaded && startTime == 0) {
            startTime = System.currentTimeMillis();
            lastInteractionTime = startTime;
            inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT);
            Log.d(TAG, "Reading view resumed, start time: " + startTime);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPdfLoaded && startTime != 0) {
            long endTime = System.currentTimeMillis();
            saveReadingTime(endTime - startTime);
            startTime = 0;
            Log.d(TAG, "Reading view paused, time spent: " + (endTime - startTime) + "ms");
        }
        inactivityHandler.removeCallbacks(inactivityRunnable);
    }

    private void saveReadingTime(long milliseconds) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentReadingTime = prefs.getInt(KEY_READING_TIME, 0);
        int additionalMinutes = (int) (milliseconds / 60000);
        if (additionalMinutes > 0) {
            currentReadingTime += additionalMinutes;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_READING_TIME, currentReadingTime);
            editor.apply();
            Log.d(TAG, "Saved " + additionalMinutes + " minutes. Total reading time: " + currentReadingTime);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePagePosition();
    }

    private void savePagePosition() {
        if (novelId == null || novelId.isEmpty()) {
            Log.w(TAG, "Cannot save page position: novelId is null or empty.");
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("page_pos_" + novelId, lastKnownPage);
        editor.apply();
        Log.d(TAG, "Saved page " + lastKnownPage + " for novel " + novelId);
    }

    private int restorePagePosition() {
        if (novelId == null || novelId.isEmpty()) return 0;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt("page_pos_" + novelId, 0);
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        this.lastKnownPage = page;
        setTitle(String.format("%s (%d / %d)", novelTitle, page + 1, pageCount));

        // Track interaction for inactivity timer
        lastInteractionTime = System.currentTimeMillis();
        if (startTime == 0 && isPdfLoaded) {
            startTime = lastInteractionTime;
            Log.d(TAG, "Page changed, resumed time tracking");
        }
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT);
    }

    @Override
    public void loadComplete(int nbPages) {
        progressBar.setVisibility(View.GONE);
        isPdfLoaded = true;
        startTime = System.currentTimeMillis();
        lastInteractionTime = startTime;
        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT);
        Log.d(TAG, "PDF Load Complete. Total pages: " + nbPages);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inactivityHandler.removeCallbacks(inactivityRunnable);
        unregisterReceiver(screenReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}