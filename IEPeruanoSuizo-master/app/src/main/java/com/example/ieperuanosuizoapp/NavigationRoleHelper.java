package com.example.ieperuanosuizoapp;

import android.content.Context;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class NavigationRoleHelper {
    private NavigationRoleHelper() {}

    public static void apply(Context context, BottomNavigationView navigation) {
        if (navigation == null) return;
        String role = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("user_mode", "ALUMNO");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role)
                || "ADMINISTRADOR".equalsIgnoreCase(role);
        MenuItem schedule = navigation.getMenu().findItem(R.id.nav_horarios);
        if (schedule != null) schedule.setVisible(!isAdmin);
    }
}
