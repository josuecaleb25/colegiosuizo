package com.example.ieperuanosuizoapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar preferencia de tema antes de crear la vista
        SharedPreferences sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        int colorScheme = sharedPreferences.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Aplicar el esquema de color si es el verde (esquema 2)
        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Ajuste de insets
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); 
                return insets;
            });
        }

        // Cargar datos iniciales de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "Usuario");
        String savedEmail = prefs.getString("user_email", "correo@ejemplo.com");
        
        android.widget.TextView tvNombre = findViewById(R.id.tv_user_name);
        android.widget.TextView tvEmail = findViewById(R.id.tv_user_email);
        
        if (tvNombre != null) tvNombre.setText(savedName);
        if (tvEmail != null) tvEmail.setText(savedEmail);

        // Cargar datos actualizados del perfil desde el backend
        cargarPerfilDesdeBackend();

        // Botón Atrás
        View btnBack = findViewById(R.id.btn_back_profile);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(0, 0);
            });
        }

        // Navegación a Apariencia
        View optionApariencia = findViewById(R.id.option_apariencia);
        if (optionApariencia != null) {
            optionApariencia.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfile.this, ViewApariencia.class);
                startActivity(intent);
            });
        }

        // Navegación a Mi Información
        View optionMiInformacion = findViewById(R.id.option_mi_informacion);
        if (optionMiInformacion != null) {
            optionMiInformacion.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfile.this, MiInformacionActivity.class);
                startActivity(intent);
            });
        }

        // Cerrar sesión
        View btnLogout = findViewById(R.id.btn_logout_profile);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> cerrarSesion());
        }

        setupBottomNavigation();
    }

    private void cerrarSesion() {
        // Limpiar preferencias de usuario
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Redirigir al Login
        Intent intent = new Intent(UserProfile.this, AuthLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void cargarPerfilDesdeBackend() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            return;
        }
        
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .getPerfilUsuario(userId)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Object perfilData = response.body().getData();
                        mostrarDatosPerfil(perfilData);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                    // Silencioso, mantener datos por defecto
                }
            });
    }
    
    private void mostrarDatosPerfil(Object perfilData) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject jsonObj = gson.toJsonTree(perfilData).getAsJsonObject();
        
        String email = jsonObj.has("email") ? jsonObj.get("email").getAsString() : "";
        
        String nombreCompleto = "";
        if (jsonObj.has("persona")) {
            com.google.gson.JsonObject persona = jsonObj.getAsJsonObject("persona");
            String nombres = persona.has("nombres") ? persona.get("nombres").getAsString() : "";
            String apellidos = persona.has("apellidos") ? persona.get("apellidos").getAsString() : "";
            nombreCompleto = nombres + " " + apellidos;
        }
        
        // Actualizar TextViews (necesitamos agregar IDs al layout)
        android.widget.TextView tvNombre = findViewById(R.id.tv_user_name);
        android.widget.TextView tvEmail = findViewById(R.id.tv_user_email);
        
        if (tvNombre != null && !nombreCompleto.isEmpty()) {
            tvNombre.setText(nombreCompleto);
        }
        
        if (tvEmail != null && !email.isEmpty()) {
            tvEmail.setText(email);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            // Obtener color seleccionado del tema
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
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_homework) {
                    Intent intent = new Intent(this, CursosActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_horarios) {
                    Intent intent = new Intent(this, HorariosActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_more) {
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}
