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
    private com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerDestinatarios;
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
        spinnerDestinatarios = findViewById(R.id.auto_complete_destinatarios);
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
        
        spinnerDestinatarios.setOnClickListener(v -> spinnerDestinatarios.showDropDown());
        
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
        String destinatario = spinnerDestinatarios.getText().toString();
        
        if (asunto.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        HashMap<String, Object> body = new HashMap<>();
        body.put("titulo", asunto);
        body.put("contenido", descripcion);
        body.put("destinatario_tipo", destinatario.equalsIgnoreCase("GLOBAL") ? "global" : "seccion");
        
        RetrofitClient.getApiService().actualizarComunicado(comunicadoId, body)
            .enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
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
        RetrofitClient.getApiService().getSecciones(userId, userRol)
            .enqueue(new retrofit2.Callback<ApiResponse<List<Object>>>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse<List<Object>>> call, retrofit2.Response<ApiResponse<List<Object>>> response) {
                    java.util.ArrayList<String> secciones = new java.util.ArrayList<>();
                    final java.util.HashMap<String, String> seccionIdMap = new java.util.HashMap<>(); // Mapa nombre -> id
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Object> data = response.body().getData();
                        
                        if (userRol.contains("admin") || userRol.contains("auxiliar")) {
                            secciones.add("GLOBAL");
                            seccionIdMap.put("GLOBAL", null);
                        }
                        
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        for (Object obj : data) {
                            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                            String nombre = jsonObj.has("nombre") ? jsonObj.get("nombre").getAsString() : "";
                            String id = jsonObj.has("id") ? jsonObj.get("id").getAsString() : "";
                            if (!nombre.isEmpty()) {
                                secciones.add(nombre);
                                seccionIdMap.put(nombre, id);
                            }
                        }
                    } else {
                        // Fallback si falla la API
                        secciones.add("GLOBAL");
                        seccionIdMap.put("GLOBAL", null);
                        secciones.add("1ro A");
                        secciones.add("2do B");
                        secciones.add("3ro C");
                        secciones.add("4to A");
                        secciones.add("5to B");
                    }
                    
                    if (secciones.isEmpty()) secciones.add("Sin secciones");
                    
                    // Usamos un layout de dropdown estándar
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CrearComunicadoActivity.this, 
                        R.layout.item_dropdown_salon, secciones);
                    spinnerDestinatarios.setAdapter(adapter);
                    
                    // IMPORTANTE: Para que no filtre nada y muestre todo al hacer clic
                    spinnerDestinatarios.setThreshold(Integer.MAX_VALUE);
                    
                    // Guardar el mapa para usarlo al enviar
                    spinnerDestinatarios.setTag(seccionIdMap);
                    
                    // Si es edición, seleccionar el salón que venía del intent
                    if (isEdit) {
                        String salonAEditar = getIntent().getStringExtra("salon");
                        if (salonAEditar != null) {
                            spinnerDestinatarios.setText(salonAEditar, false);
                        }
                    } else if (secciones.contains("GLOBAL")) {
                        // Predeterminado: GLOBAL si no es edición
                        spinnerDestinatarios.setText("GLOBAL", false);
                    }
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<ApiResponse<List<Object>>> call, @NonNull Throwable t) {
                    String[] defaultSalones = {"GLOBAL", "1ro A", "2do B", "3ro C", "4to A", "5to B"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CrearComunicadoActivity.this, 
                        R.layout.item_dropdown_salon, defaultSalones);
                    spinnerDestinatarios.setAdapter(adapter);
                    spinnerDestinatarios.setThreshold(Integer.MAX_VALUE);
                }
            });
    }

    private void enviarComunicado() {
        String asunto = etAsunto.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String destinatario = spinnerDestinatarios.getText().toString().trim();
        
        android.util.Log.d("CrearComunicado", "=== INICIO ENVIAR ===");
        android.util.Log.d("CrearComunicado", "Asunto: '" + asunto + "'");
        android.util.Log.d("CrearComunicado", "Descripcion: '" + descripcion + "'");
        android.util.Log.d("CrearComunicado", "Destinatario: '" + destinatario + "'");
        android.util.Log.d("CrearComunicado", "UserId: '" + userId + "'");
        
        if (asunto.isEmpty()) {
            Toast.makeText(this, "Escribe un asunto", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (descripcion.isEmpty()) {
            Toast.makeText(this, "Escribe una descripción", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (destinatario.isEmpty()) {
            Toast.makeText(this, "Selecciona un destinatario", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: No se encontró el ID de usuario", Toast.LENGTH_SHORT).show();
            return;
        }
        
        HashMap<String, Object> body = new HashMap<>();
        body.put("usuario_id", userId);
        body.put("titulo", asunto);
        body.put("contenido", descripcion);
        body.put("tipo", "general");
        
        // Obtener el mapa de IDs guardado en el tag
        @SuppressWarnings("unchecked")
        java.util.HashMap<String, String> seccionIdMap = 
            (java.util.HashMap<String, String>) spinnerDestinatarios.getTag();
        
        android.util.Log.d("CrearComunicado", "SeccionIdMap: " + (seccionIdMap != null ? seccionIdMap.toString() : "null"));
        
        // Determinar destinatario_tipo y seccion_id
        if (destinatario.equalsIgnoreCase("GLOBAL")) {
            body.put("destinatario_tipo", "global");
            android.util.Log.d("CrearComunicado", "Tipo: GLOBAL");
        } else {
            body.put("destinatario_tipo", "seccion");
            // Obtener el ID de la sección seleccionada
            if (seccionIdMap != null && seccionIdMap.containsKey(destinatario)) {
                String seccionId = seccionIdMap.get(destinatario);
                android.util.Log.d("CrearComunicado", "SeccionId encontrado: " + seccionId);
                if (seccionId != null && !seccionId.isEmpty()) {
                    body.put("seccion_id", seccionId);
                } else {
                    Toast.makeText(this, "Error: No se encontró el ID de la sección", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                android.util.Log.e("CrearComunicado", "No se encontró el ID para: " + destinatario);
                Toast.makeText(this, "Error: No se pudo obtener el ID de la sección", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        android.util.Log.d("CrearComunicado", "Body final: " + body.toString());
        
        RetrofitClient.getApiService().crearComunicado(body)
            .enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                    android.util.Log.d("CrearComunicado", "Response code: " + response.code());
                    android.util.Log.d("CrearComunicado", "Response body: " + (response.body() != null ? response.body().toString() : "null"));
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        android.util.Log.d("CrearComunicado", "✅ Comunicado creado exitosamente");
                        mostrarDialogoExito(asunto, destinatario);
                    } else {
                        String errorMsg = "Error al enviar";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        } else if (response.errorBody() != null) {
                            try {
                                errorMsg = response.errorBody().string();
                            } catch (Exception e) {
                                errorMsg = "Error desconocido";
                            }
                        }
                        android.util.Log.e("CrearComunicado", "❌ Error: " + errorMsg);
                        Toast.makeText(CrearComunicadoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                    android.util.Log.e("CrearComunicado", "❌ Error de conexión: " + t.getMessage());
                    t.printStackTrace();
                    Toast.makeText(CrearComunicadoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
