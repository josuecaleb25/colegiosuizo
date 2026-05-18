package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DetalleComunicadoGestionActivity extends AppCompatActivity {

    private String id, titulo, emisor, hora, salon, descripcion;
    private int vistos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        int colorScheme = themePrefs.getInt("colorScheme", 0);
        if (isDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (colorScheme == 2) setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_comunicado_gestion);

        // Obtener datos del Intent
        id = getIntent().getStringExtra("id");
        titulo = getIntent().getStringExtra("titulo");
        emisor = getIntent().getStringExtra("emisor");
        hora = getIntent().getStringExtra("hora");
        salon = getIntent().getStringExtra("salon");
        vistos = getIntent().getIntExtra("vistos", 0);
        
        // La descripción la traeremos del backend o si la pasamos por intent
        descripcion = getIntent().getStringExtra("descripcion");

        initViews();
    }

    private void initViews() {
        ((TextView) findViewById(R.id.tv_detalle_asunto)).setText(titulo);
        ((TextView) findViewById(R.id.tv_detalle_descripcion)).setText(descripcion != null ? descripcion : "Cargando contenido...");
        ((TextView) findViewById(R.id.tv_detalle_destinatarios)).setText(salon);
        ((TextView) findViewById(R.id.tv_detalle_hora)).setText(hora);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_detalle_editar).setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearComunicadoActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("id", id);
            intent.putExtra("titulo", titulo);
            intent.putExtra("descripcion", descripcion);
            intent.putExtra("salon", salon);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_detalle_eliminar).setOnClickListener(v -> {
            eliminarComunicado();
        });
        
        if (descripcion == null) {
            cargarDescripcion();
        }
        
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}};
        int[] colors = new int[]{Color.parseColor("#BA1924"), Color.parseColor("#5E5F60")};
        bottomNav.setItemIconTintList(new android.content.res.ColorStateList(states, colors));
        bottomNav.getMenu().setGroupCheckable(0, false, true);
        bottomNav.setOnItemSelectedListener(item -> {
            finish();
            return true;
        });
    }

    private void cargarDescripcion() {
        // En un caso real, haríamos un GET /comunicados/{id}
        // Por ahora simulamos o usamos el que ya tenemos en la lista si lo pasamos
    }

    private void eliminarComunicado() {
        RetrofitClient.getApiService().eliminarComunicado(id).enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mostrarDialogoEliminado();
                } else {
                    Toast.makeText(DetalleComunicadoGestionActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(DetalleComunicadoGestionActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoEliminado() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_comunicado_eliminado, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(v)
                .setCancelable(false)
                .create();

        ((TextView) v.findViewById(R.id.tv_dialog_asunto)).setText(titulo);
        ((TextView) v.findViewById(R.id.tv_dialog_destinatarios)).setText(salon);
        
        String fechaHoy = new SimpleDateFormat("'Hoy, ' d 'de ' MMMM", new Locale("es", "PE")).format(Calendar.getInstance().getTime());
        ((TextView) v.findViewById(R.id.tv_dialog_fecha)).setText(fechaHoy);

        v.findViewById(R.id.btn_volver).setOnClickListener(view -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9), 
                                       ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
