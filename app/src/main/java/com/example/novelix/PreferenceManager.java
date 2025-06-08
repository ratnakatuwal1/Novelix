package com.example.novelix;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "novelix_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setOnboardingComplete(boolean isComplete) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, isComplete).apply();
    }

    public boolean isOnboardingComplete() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }
}
