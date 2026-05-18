package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CrearComunicadoActivity extends AppCompatActivity {

    private EditText etAsunto, etDescripcion;
    private Spinner spinnerDestinatarios;
    private TextView tvTitleHeader;
    private com.google.android.material.button.MaterialButton btnCrear;
    private String userId, userRol, comunicadoId;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        int colorScheme = themePrefs.getInt("colorScheme", 0);

        if (isDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (colorScheme == 2) setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_comunicado);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        userRol = prefs.getString("user_mode", "ALUMNO").toLowerCase();

        etAsunto = findViewById(R.id.et_asunto);
        etDescripcion = findViewById(R.id.et_descripcion);
        spinnerDestinatarios = findViewById(R.id.spinner_destinatarios);
        tvTitleHeader = findViewById(R.id.tv_title_header);
        btnCrear = findViewById(R.id.btn_enviar);

        // Detectar si es modo edición
        isEdit = getIntent().getBooleanExtra("isEdit", false);
        if (isEdit) {
            comunicadoId = getIntent().getStringExtra("id");
            String t = getIntent().getStringExtra("titulo");
            String d = getIntent().getStringExtra("descripcion");
            
            tvTitleHeader.setText("Editar comunicado");
            btnCrear.setText("Finalizar");
            etAsunto.setText(t);
            etDescripcion.setText(d);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnCrear.setOnClickListener(v -> {
            if (isEdit) actualizarComunicado();
            else enviarComunicado();
        });

        cargarSecciones();
        setupBottomNavigation();
    }

    private void actualizarComunicado() {
        String asunto = etAsunto.getText().toString();
        String descripcion = etDescripcion.getText().toString();
        String destinatario = spinnerDestinatarios.getSelectedItem() != null ? 
                spinnerDestinatarios.getSelectedItem().toString() : "";

        if (asunto.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> body = new HashMap<>();
        body.put("titulo", asunto);
        body.put("contenido", descripcion);
        body.put("destinatario_tipo", destinatario.equalsIgnoreCase("GLOBAL") ? "global" : "seccion");

        RetrofitClient.getApiService().actualizarComunicado(comunicadoId, body).enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mostrarDialogoEditado(asunto, destinatario);
                } else {
                    Toast.makeText(CrearComunicadoActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(CrearComunicadoActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoEditado(String asunto, String destinatario) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_comunicado_editado, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(v)
                .setCancelable(false)
                .create();

        ((TextView) v.findViewById(R.id.tv_dialog_asunto)).setText(asunto + " (Editado)");
        ((TextView) v.findViewById(R.id.tv_dialog_destinatarios)).setText(destinatario);
        
        String hora = new SimpleDateFormat("h:mm a", Locale.US).format(Calendar.getInstance().getTime());
        ((TextView) v.findViewById(R.id.tv_dialog_hora)).setText(hora);

        v.findViewById(R.id.btn_volver).setOnClickListener(view -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9), 
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void cargarSecciones() {
        RetrofitClient.getApiService().getSecciones(userId, userRol).enqueue(new retrofit2.Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<List<Object>>> call, retrofit2.Response<ApiResponse<List<Object>>> response) {
                java.util.ArrayList<String> secciones = new java.util.ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Object> data = response.body().getData();
                    if (userRol.contains("admin") || userRol.contains("auxiliar")) {
                        secciones.add("GLOBAL");
                    }
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    for (Object obj : data) {
                        com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                        String nombre = jsonObj.has("nombre") ? jsonObj.get("nombre").getAsString() : "";
                        if (!nombre.isEmpty()) secciones.add(nombre);
                    }
                } else {
                    // Fallback si falla la API
                    secciones.add("GLOBAL");
                    secciones.add("1ro A");
                    secciones.add("2do B");
                    secciones.add("3ro C");
                    secciones.add("4to A");
                    secciones.add("5to B");
                }
                
                if (secciones.isEmpty()) secciones.add("Sin secciones");

                // Usamos un layout personalizado para que el texto sea blanco en el Spinner rojo
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(CrearComunicadoActivity.this, 
                        android.R.layout.simple_spinner_item, secciones) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        if (v instanceof TextView) {
                            ((TextView) v).setTextColor(Color.WHITE);
                            ((TextView) v).setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                        }
                        return v;
                    }
                };
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDestinatarios.setAdapter(adapter);

                // Si es edición, seleccionar el salón que venía del intent
                if (isEdit) {
                    String salonAEditar = getIntent().getStringExtra("salon");
                    if (salonAEditar != null) {
                        for (int i = 0; i < secciones.size(); i++) {
                            if (secciones.get(i).equalsIgnoreCase(salonAEditar)) {
                                spinnerDestinatarios.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiResponse<List<Object>>> call, @NonNull Throwable t) {
                String[] defaultSalones = {"GLOBAL", "1ro A", "2do B", "3ro C", "4to A", "5to B"};
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(CrearComunicadoActivity.this, 
                        android.R.layout.simple_spinner_item, defaultSalones) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        if (v instanceof TextView) {
                            ((TextView) v).setTextColor(Color.WHITE);
                            ((TextView) v).setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                        }
                        return v;
                    }
                };
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDestinatarios.setAdapter(adapter);
            }
        });
    }

    private void enviarComunicado() {
        String asunto = etAsunto.getText().toString();
        String descripcion = etDescripcion.getText().toString();
        String destinatario = spinnerDestinatarios.getSelectedItem() != null ? 
                spinnerDestinatarios.getSelectedItem().toString() : "";

        if (asunto.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> body = new HashMap<>();
        body.put("usuario_id", userId);
        body.put("titulo", asunto);
        body.put("contenido", descripcion);
        body.put("tipo", "general");
        body.put("destinatario_tipo", destinatario.equalsIgnoreCase("GLOBAL") ? "global" : "seccion");

        RetrofitClient.getApiService().crearComunicado(body).enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mostrarDialogoExito(asunto, destinatario);
                } else {
                    Toast.makeText(CrearComunicadoActivity.this, "Error al enviar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(CrearComunicadoActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoExito(String asunto, String destinatario) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_comunicado_enviado, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(v)
                .setCancelable(false)
                .create();

        ((TextView) v.findViewById(R.id.tv_dialog_asunto)).setText(asunto);
        ((TextView) v.findViewById(R.id.tv_dialog_destinatarios)).setText(destinatario);
        
        String hora = new SimpleDateFormat("h:mm a", Locale.US).format(Calendar.getInstance().getTime());
        ((TextView) v.findViewById(R.id.tv_dialog_hora)).setText(hora);

        v.findViewById(R.id.btn_ver_cambios).setOnClickListener(view -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9), 
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}};
        int[] colors = new int[]{Color.parseColor("#BA1924"), Color.parseColor("#5E5F60")};
        bottomNav.setItemIconTintList(new ColorStateList(states, colors));
        bottomNav.getMenu().setGroupCheckable(0, false, true);
        bottomNav.setOnItemSelectedListener(item -> {
            finish();
            return true;
        });
    }
}
