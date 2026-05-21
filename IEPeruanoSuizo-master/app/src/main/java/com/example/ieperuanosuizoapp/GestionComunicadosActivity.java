package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class GestionComunicadosActivity extends AppCompatActivity {

    private LinearLayout containerHistorial, layoutEmptyHistory;
    private TextView tvCountComunicados;
    private android.widget.EditText etBuscar;
    private com.google.android.material.progressindicator.CircularProgressIndicator loadingIndicator;
    private String userMode;
    private List<HistorialItem> listaHistorial = new ArrayList<>();
    private List<HistorialItem> listaFiltrada = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        int colorScheme = themePrefs.getInt("colorScheme", 0);

        if (isDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (colorScheme == 2) setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_comunicados);

        // Obtener el modo de usuario
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = userPrefs.getString("user_mode", "ALUMNO");

        containerHistorial = findViewById(R.id.container_historial);
        layoutEmptyHistory = findViewById(R.id.layout_empty_history);
        loadingIndicator = findViewById(R.id.loading_indicator);
        tvCountComunicados = findViewById(R.id.tv_count_comunicados);
        etBuscar = findViewById(R.id.et_buscar_comunicado);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.fab_nuevo_comunicado).setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearComunicadoActivity.class);
            startActivity(intent);
        });

        setupSearchLogic();

        // Cargar historial desde el backend
        cargarHistorialDesdeBackend();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar comunicados cada vez que volvemos a esta actividad
        cargarHistorialDesdeBackend();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                Color.parseColor("#BA1924"),
                Color.parseColor("#5E5F60")
        };
        ColorStateList navTint = new ColorStateList(states, colors);
        bottomNav.setItemIconTintList(navTint);

        bottomNav.getMenu().setGroupCheckable(0, false, true);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_homework) {
                Intent intent = new Intent(this, CursosActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_horarios) {
                Intent intent = new Intent(this, HorariosActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupSearchLogic() {
        etBuscar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarComunicados(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filtrarComunicados(String query) {
        listaFiltrada.clear();
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaHistorial);
        } else {
            String lowerQuery = query.toLowerCase();
            for (HistorialItem item : listaHistorial) {
                if (item.titulo.toLowerCase().contains(lowerQuery) || 
                    item.emisor.toLowerCase().contains(lowerQuery) || 
                    item.salon.toLowerCase().contains(lowerQuery)) {
                    listaFiltrada.add(item);
                }
            }
        }
        actualizarVistaHistorial();
        tvCountComunicados.setText(String.valueOf(listaFiltrada.size()));
    }

    private void actualizarVistaHistorial() {
        containerHistorial.removeAllViews();
        
        if (listaFiltrada.isEmpty()) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyHistory.setVisibility(View.GONE);
            for (HistorialItem item : listaFiltrada) {
                View v = LayoutInflater.from(this).inflate(R.layout.item_comunicado_gestion, containerHistorial, false);
                
                // Configurar vistas
                ((TextView) v.findViewById(R.id.tv_titulo_comunicado)).setText(item.titulo);
                ((TextView) v.findViewById(R.id.tv_emisor_nombre)).setText(item.emisor);
                ((TextView) v.findViewById(R.id.tv_vistos_count)).setText("Vistos por " + item.vistosCount + " personas");
                ((TextView) v.findViewById(R.id.tv_hora)).setText(item.hora);
                ((TextView) v.findViewById(R.id.tv_salon_destinatario)).setText(item.salon);
                
                // Al hacer clic, abrir el detalle del comunicado
                v.setOnClickListener(view -> {
                    Intent intent = new Intent(this, DetalleComunicadoGestionActivity.class);
                    intent.putExtra("id", item.id);
                    intent.putExtra("titulo", item.titulo);
                    intent.putExtra("emisor", item.emisor);
                    intent.putExtra("hora", item.hora);
                    intent.putExtra("salon", item.salon);
                    intent.putExtra("vistos", item.vistosCount);
                    // Pasamos la descripción completa (necesitaremos extraerla en el loop de carga)
                    intent.putExtra("descripcion", item.contenido);
                    startActivity(intent);
                });
                
                containerHistorial.addView(v);
            }
        }
    }
    
    private void mostrarMenuOpciones(HistorialItem item, View cardView) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, cardView.findViewById(R.id.btn_opciones));
        popup.getMenuInflater().inflate(R.menu.menu_comunicado_opciones, popup.getMenu());
        
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_editar) {
                mostrarDialogoEditar(item);
                return true;
            } else if (id == R.id.action_eliminar) {
                mostrarDialogoEliminar(item);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void mostrarDialogoEditar(HistorialItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enviar_comunicado, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        // Pre-llenar los campos con los datos actuales
        android.widget.EditText etTitulo = dialogView.findViewById(R.id.et_titulo);
        android.widget.EditText etMensaje = dialogView.findViewById(R.id.et_mensaje);
        Spinner spinner = dialogView.findViewById(R.id.spinner_salon);
        
        etTitulo.setText(item.titulo);
        // Necesitamos obtener el contenido completo del comunicado
        obtenerComunicadoCompleto(item.id, etMensaje, spinner);

        dialogView.findViewById(R.id.btn_cancelar).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_publicar).setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString();
            String contenido = etMensaje.getText().toString();
            
            if (spinner.getSelectedItem() == null) {
                Toast.makeText(this, "Cargando secciones...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String salonSel = spinner.getSelectedItem().toString();

            if (titulo.isEmpty()) {
                Toast.makeText(this, "Escribe un título", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar en el backend
            actualizarComunicadoBackend(item.id, titulo, contenido, salonSel, dialog);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.95), 
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }
    
    private void obtenerComunicadoCompleto(String comunicadoId, android.widget.EditText etMensaje, Spinner spinner) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String userRol = prefs.getString("user_mode", "ALUMNO").toLowerCase();
        
        // Cargar secciones
        cargarSeccionesDisponibles(spinner, userId, userRol);
        
        // Obtener el comunicado completo
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .getHistorialComunicadosEnviados(userId)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Object> comunicados = response.body().getData();
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        
                        for (Object obj : comunicados) {
                            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                            String id = jsonObj.has("id") ? jsonObj.get("id").getAsString() : "";
                            
                            if (id.equals(comunicadoId)) {
                                String contenido = jsonObj.has("contenido") ? jsonObj.get("contenido").getAsString() : "";
                                etMensaje.setText(contenido);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    Toast.makeText(GestionComunicadosActivity.this, "Error al cargar datos: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void actualizarComunicadoBackend(String comunicadoId, String titulo, String contenido, String salon, android.app.AlertDialog dialog) {
        // Determinar el tipo de destinatario
        String destinatarioTipo = salon.equals("Global") ? "global" : "seccion";
        
        // Crear objeto de comunicado
        java.util.HashMap<String, Object> comunicadoData = new java.util.HashMap<>();
        comunicadoData.put("titulo", titulo);
        comunicadoData.put("contenido", contenido);
        comunicadoData.put("tipo", "general");
        comunicadoData.put("destinatario_tipo", destinatarioTipo);
        comunicadoData.put("seccion_id", null); // TODO: mapear salon a seccion_id real
        comunicadoData.put("grado_id", null);
        
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .actualizarComunicado(comunicadoId, comunicadoData)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(GestionComunicadosActivity.this, "Comunicado actualizado con éxito", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarHistorialDesdeBackend();
                    } else {
                        String errorMsg = "Error al actualizar comunicado";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        Toast.makeText(GestionComunicadosActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(GestionComunicadosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void mostrarDialogoEliminar(HistorialItem item) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Comunicado")
            .setMessage("¿Estás seguro de que deseas eliminar este comunicado?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                eliminarComunicadoBackend(item);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void eliminarComunicadoBackend(HistorialItem item) {
        if (item.id == null || item.id.isEmpty()) {
            Toast.makeText(this, "Error: ID de comunicado no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .eliminarComunicado(item.id)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        listaHistorial.remove(item);
                        actualizarVistaHistorial();
                        Toast.makeText(GestionComunicadosActivity.this, "Comunicado eliminado exitosamente", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMsg = "Error al eliminar comunicado";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        Toast.makeText(GestionComunicadosActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(GestionComunicadosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void cargarHistorialDesdeBackend() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String rol = prefs.getString("user_role", "profesor");  // Obtener el rol del usuario
        
        if (userId == null) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }

        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        containerHistorial.setVisibility(View.GONE);
        layoutEmptyHistory.setVisibility(View.GONE);
        
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .getHistorialComunicadosEnviados(userId, rol)  // Enviar el rol también
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                    if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                    containerHistorial.setVisibility(View.VISIBLE);
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Object> historialData = response.body().getData();
                        
                        listaHistorial.clear();
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        
                        for (Object obj : historialData) {
                            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                            
                            String id = jsonObj.has("id") ? jsonObj.get("id").getAsString() : "";
                            String titulo = jsonObj.has("titulo") ? jsonObj.get("titulo").getAsString() : "";
                            String contenido = jsonObj.has("contenido") ? jsonObj.get("contenido").getAsString() : "";
                            String fechaPublicacion = jsonObj.has("fecha_publicacion") ? jsonObj.get("fecha_publicacion").getAsString() : "";
                            String emisor = jsonObj.has("emisor") ? jsonObj.get("emisor").getAsString() : "Administración";
                            String destinatario = jsonObj.has("destinatario") ? jsonObj.get("destinatario").getAsString() : "Global";
                            String estado = jsonObj.has("estado") ? jsonObj.get("estado").getAsString() : "Enviado";
                            int vistos = jsonObj.has("vistos_count") ? jsonObj.get("vistos_count").getAsInt() : 0;
                            
                            // Formatear hora de "2024-05-14T10:30:00" a "10:30 AM"
                            String horaStr = "";
                            try {
                                String[] partes = fechaPublicacion.split("T");
                                if (partes.length > 1) {
                                    String[] horaPartes = partes[1].split(":");
                                    int h = Integer.parseInt(horaPartes[0]);
                                    int m = Integer.parseInt(horaPartes[1]);
                                    String ampm = h >= 12 ? "pm" : "am";
                                    if (h > 12) h -= 12;
                                    if (h == 0) h = 12;
                                    horaStr = String.format(java.util.Locale.US, "%d:%02d %s", h, m, ampm);
                                }
                            } catch (Exception e) {
                                horaStr = "--:--";
                            }
                            
                            listaHistorial.add(new HistorialItem(id, titulo, emisor, horaStr, destinatario, estado, vistos, contenido));
                        }
                        
                        listaFiltrada.clear();
                        listaFiltrada.addAll(listaHistorial);
                        tvCountComunicados.setText(String.valueOf(listaFiltrada.size()));
                        actualizarVistaHistorial();
                    } else {
                        layoutEmptyHistory.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                    containerHistorial.setVisibility(View.VISIBLE);
                    Toast.makeText(GestionComunicadosActivity.this, "Error al cargar historial: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    layoutEmptyHistory.setVisibility(View.VISIBLE);
                }
            });
    }

    private void mostrarDialogoEnviar() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enviar_comunicado, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        Spinner spinner = dialogView.findViewById(R.id.spinner_salon);
        
        // Cargar secciones desde el backend
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String userRol = prefs.getString("user_mode", "ALUMNO").toLowerCase();
        
        cargarSeccionesDisponibles(spinner, userId, userRol);

        dialogView.findViewById(R.id.btn_cancelar).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_publicar).setOnClickListener(v -> {
            String titulo = ((android.widget.EditText) dialogView.findViewById(R.id.et_titulo)).getText().toString();
            String contenido = ((android.widget.EditText) dialogView.findViewById(R.id.et_mensaje)).getText().toString();
            
            if (spinner.getSelectedItem() == null) {
                Toast.makeText(this, "Cargando secciones...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String salonSel = spinner.getSelectedItem().toString();

            if (titulo.isEmpty()) {
                Toast.makeText(this, "Escribe un título", Toast.LENGTH_SHORT).show();
                return;
            }

            // Enviar al backend
            enviarComunicadoAlBackend(titulo, contenido, salonSel, dialog);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.95), 
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }
    
    private void cargarSeccionesDisponibles(Spinner spinner, String userId, String rol) {
        // Mostrar loading temporal
        String[] loadingArray = new String[]{"Cargando..."};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, loadingArray));
        
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .getSecciones(userId, rol)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Object> seccionesData = response.body().getData();
                        
                        java.util.ArrayList<String> secciones = new java.util.ArrayList<>();
                        
                        // Admin y auxiliar pueden enviar a todos
                        if ("administrador".equals(rol) || "auxiliar".equals(rol)) {
                            secciones.add("Global");
                        }
                        
                        // Agregar secciones disponibles
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        for (Object obj : seccionesData) {
                            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                            String nombreSeccion = jsonObj.has("nombre") ? jsonObj.get("nombre").getAsString() : "";
                            if (!nombreSeccion.isEmpty()) {
                                secciones.add(nombreSeccion);
                            }
                        }
                        
                        if (secciones.isEmpty()) {
                            secciones.add("Sin secciones disponibles");
                        }
                        
                        String[] seccionesArray = secciones.toArray(new String[0]);
                        spinner.setAdapter(new ArrayAdapter<>(GestionComunicadosActivity.this, 
                                android.R.layout.simple_spinner_dropdown_item, seccionesArray));
                    } else {
                        // Fallback a secciones por defecto según rol
                        String[] salones;
                        if ("profesor".equals(rol) || "docente".equals(rol)) {
                            salones = new String[]{"4to A", "5to B"}; 
                        } else if ("auxiliar".equals(rol)) {
                            salones = new String[]{"1ro A", "2do B", "3ro C", "4to A", "5to B"};
                        } else {
                            salones = new String[]{"Global", "1ro A", "2do B", "3ro C", "4to A", "5to B"};
                        }
                        spinner.setAdapter(new ArrayAdapter<>(GestionComunicadosActivity.this, 
                                android.R.layout.simple_spinner_dropdown_item, salones));
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    // Fallback a secciones por defecto según rol
                    String[] salones;
                    if ("profesor".equals(rol) || "docente".equals(rol)) {
                        salones = new String[]{"4to A", "5to B"}; 
                    } else if ("auxiliar".equals(rol)) {
                        salones = new String[]{"1ro A", "2do B", "3ro C", "4to A", "5to B"};
                    } else {
                        salones = new String[]{"Global", "1ro A", "2do B", "3ro C", "4to A", "5to B"};
                    }
                    spinner.setAdapter(new ArrayAdapter<>(GestionComunicadosActivity.this, 
                            android.R.layout.simple_spinner_dropdown_item, salones));
                }
            });
    }
    
    private void enviarComunicadoAlBackend(String titulo, String contenido, String salon, android.app.AlertDialog dialog) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        // Log para debug
        android.util.Log.d("GestionComunicados", "User ID: " + userId);
        android.util.Log.d("GestionComunicados", "Titulo: " + titulo);
        android.util.Log.d("GestionComunicados", "Salon: " + salon);
        
        if (userId == null) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determinar el tipo de destinatario
        String destinatarioTipo = salon.equals("Global") ? "global" : "seccion";
        
        // Crear objeto de comunicado
        java.util.HashMap<String, Object> comunicadoData = new java.util.HashMap<>();
        comunicadoData.put("usuario_id", userId);
        comunicadoData.put("titulo", titulo);
        comunicadoData.put("contenido", contenido);
        comunicadoData.put("tipo", "general");
        comunicadoData.put("destinatario_tipo", destinatarioTipo);
        comunicadoData.put("seccion_id", null); // TODO: mapear salon a seccion_id real
        comunicadoData.put("grado_id", null);
        
        android.util.Log.d("GestionComunicados", "Datos a enviar: " + comunicadoData.toString());
        
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .crearComunicado(comunicadoData)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> response) {
                    android.util.Log.d("GestionComunicados", "Response code: " + response.code());
                    android.util.Log.d("GestionComunicados", "Response body: " + response.body());
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(GestionComunicadosActivity.this, "Comunicado enviado con éxito", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarHistorialDesdeBackend();
                    } else {
                        String errorMsg = "Error al enviar comunicado";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        android.util.Log.e("GestionComunicados", "Error: " + errorMsg);
                        Toast.makeText(GestionComunicadosActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                    android.util.Log.e("GestionComunicados", "Error de conexión", t);
                    Toast.makeText(GestionComunicadosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private static class HistorialItem {
        String id, titulo, emisor, hora, salon, estado, contenido;
        int vistosCount;
        HistorialItem(String id, String titulo, String emisor, String hora, String salon, String estado, int vistosCount, String contenido) {
            this.id = id;
            this.titulo = titulo;
            this.emisor = emisor;
            this.hora = hora;
            this.salon = salon;
            this.estado = estado;
            this.vistosCount = vistosCount;
            this.contenido = contenido;
        }
    }
}