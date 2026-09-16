package com.example.ieperuanosuizoapp;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import com.example.ieperuanosuizoapp.api.RetrofitClient;

public class ColegioApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        RetrofitClient.init(this);
    }
}
