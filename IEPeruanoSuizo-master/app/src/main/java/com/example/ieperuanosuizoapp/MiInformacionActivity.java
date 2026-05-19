package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MiInformacionActivity extends AppCompatActivity {

    private TextView tvNombres, tvApellidos, tvEmail, tvFechaNac, tvRol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar tema
        SharedPreferences sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        int colorScheme = sharedPreferences.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_informacion);

        // Inicializar vistas
        tvNombres = findViewById(R.id.tv_info_nombres);
        tvApellidos = findViewById(R.id.tv_info_apellidos);
        tvEmail = findViewById(R.id.tv_info_email);
        tvFechaNac = findViewById(R.id.tv_info_fecha_nac);
        tvRol = findViewById(R.id.tv_info_rol);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Cargar datos iniciales
        cargarDatosLocales();
        
        // Cargar datos actualizados
        cargarDatosRemotos();
    }

    private void cargarDatosLocales() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String nombres = prefs.getString("user_nombres", "");
        String apellidos = prefs.getString("user_apellidos", "");
        String email = prefs.getString("user_email", "");
        String rol = prefs.getString("user_mode", "Alumno");

        // Si no hay nombres/apellidos separados, intentar separar del nombre completo
        if (nombres.isEmpty() && apellidos.isEmpty()) {
            String nombreCompleto = prefs.getString("user_name", "Usuario");
            String[] partes = nombreCompleto.split(" ", 2);
            nombres = partes.length > 0 ? partes[0] : nombreCompleto;
            apellidos = partes.length > 1 ? partes[1] : "";
        }

        if (tvNombres != null) tvNombres.setText(nombres);
        if (tvApellidos != null) tvApellidos.setText(apellidos);
        if (tvEmail != null) tvEmail.setText(email);
        if (tvRol != null) tvRol.setText(rol);
    }

    private void cargarDatosRemotos() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId == null) return;

        RetrofitClient.getApiService().getPerfilUsuario(userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mostrarDatos(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // Error silencioso
            }
        });
    }

    private void mostrarDatos(Object data) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObj = gson.toJsonTree(data).getAsJsonObject();

            if (jsonObj.has("persona")) {
                JsonObject persona = jsonObj.getAsJsonObject("persona");
                String nombres = persona.has("nombres") ? persona.get("nombres").getAsString() : "";
                String apellidos = persona.has("apellidos") ? persona.get("apellidos").getAsString() : "";
                String fechaNac = persona.has("fecha_nacimiento") && !persona.get("fecha_nacimiento").isJsonNull() 
                    ? formatearFecha(persona.get("fecha_nacimiento").getAsString()) 
                    : "No registrada";

                if (tvNombres != null) tvNombres.setText(nombres);
                if (tvApellidos != null) tvApellidos.setText(apellidos);
                if (tvFechaNac != null) tvFechaNac.setText(fechaNac);
            }

            if (jsonObj.has("email")) {
                String email = jsonObj.get("email").getAsString();
                if (tvEmail != null) tvEmail.setText(email);
            }
            
            if (jsonObj.has("rol")) {
                String rol = jsonObj.get("rol").getAsString();
                if (tvRol != null) tvRol.setText(rol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Formatear fecha de YYYY-MM-DD a "15 de Mayo, 2008"
     */
    private String formatearFecha(String fechaISO) {
        try {
            if (fechaISO == null || fechaISO.isEmpty()) {
                return "No registrada";
            }

            // Parsear fecha ISO (YYYY-MM-DD)
            String[] partes = fechaISO.split("T")[0].split("-");
            if (partes.length < 3) return fechaISO;

            int año = Integer.parseInt(partes[0]);
            int mes = Integer.parseInt(partes[1]);
            int dia = Integer.parseInt(partes[2]);

            String[] meses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            };

            return dia + " de " + meses[mes - 1] + ", " + año;
        } catch (Exception e) {
            return fechaISO;
        }
    }
}
