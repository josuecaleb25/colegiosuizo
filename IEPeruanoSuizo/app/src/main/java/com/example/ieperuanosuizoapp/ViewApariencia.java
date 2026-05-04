package com.example.ieperuanosuizoapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ViewApariencia extends AppCompatActivity {

    private RadioButton rbDark, rbLight;
    private LinearLayout layoutDark, layoutLight;
    private CardView colorScheme2;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "isDarkMode";
    private static final String KEY_COLOR_SCHEME = "colorScheme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_THEME, false);
        int colorScheme = sharedPreferences.getInt(KEY_COLOR_SCHEME, 0);

        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_apariencia);

        rbDark = findViewById(R.id.rb_dark);
        rbLight = findViewById(R.id.rb_light);
        layoutDark = findViewById(R.id.layout_dark_theme);
        layoutLight = findViewById(R.id.layout_light_theme);
        colorScheme2 = findViewById(R.id.color_scheme_2);

        if (isDarkMode) {
            rbDark.setChecked(true);
            rbLight.setChecked(false);
        } else {
            rbLight.setChecked(true);
            rbDark.setChecked(false);
        }

        layoutDark.setOnClickListener(v -> selectTheme(true));
        rbDark.setOnClickListener(v -> selectTheme(true));
        layoutLight.setOnClickListener(v -> selectTheme(false));
        rbLight.setOnClickListener(v -> selectTheme(false));

        colorScheme2.setOnClickListener(v -> applyColorScheme(2));

        View colorScheme1 = findViewById(R.id.color_scheme_1);
        if (colorScheme1 != null) {
            colorScheme1.setOnClickListener(v -> applyColorScheme(0));
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goBackToHome());
        }

        setupBottomNavigation();
    }

    private void selectTheme(boolean dark) {
        rbDark.setChecked(dark);
        rbLight.setChecked(!dark);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_THEME, dark);
        editor.apply();

        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyColorScheme(int schemeId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COLOR_SCHEME, schemeId);
        editor.apply();

        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void goBackToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goBackToHome();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            int colorSeleccionado;
            TypedValue typedValue = new TypedValue();
            if (getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
                colorSeleccionado = typedValue.data;
            } else {
                colorSeleccionado = Color.parseColor("#BA1924");
            }

            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            };
            int[] colors = new int[]{
                    colorSeleccionado,
                    Color.parseColor("#5E5F60")
            };
            ColorStateList navTint = new ColorStateList(states, colors);
            bottomNavigationView.setItemIconTintList(navTint);
            bottomNavigationView.setItemTextColor(navTint);

            // Siempre seleccionar los 3 puntos (nav_more)
            bottomNavigationView.setSelectedItemId(R.id.nav_more);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    goBackToHome();
                    return true;
                } else if (itemId == R.id.nav_more) {
                    return true;
                }
                return false;
            });
        }
    }
}