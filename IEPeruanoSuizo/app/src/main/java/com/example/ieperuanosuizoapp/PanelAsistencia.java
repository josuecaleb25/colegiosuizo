package com.example.ieperuanosuizoapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import com.example.ieperuanosuizoapp.api.ApiClient;
import com.example.ieperuanosuizoapp.api.ApiResponse;
import com.example.ieperuanosuizoapp.models.AlumnoAsistencia;
import com.example.ieperuanosuizoapp.models.QrScanRequest;
import com.example.ieperuanosuizoapp.models.QrScanResponse;
import com.example.ieperuanosuizoapp.models.Salon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PanelAsistencia extends AppCompatActivity {

    private PreviewView previewView;
    private MaterialButton btnActivarCamara;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private boolean isCameraActive = false;
    private ProcessCameraProvider cameraProvider;
    private AutoCompleteTextView autoCompleteSalon;
    private ConstraintLayout mainLayout;
    private View cameraContainer, layoutEmptyState, layoutPaginacion, dividerPaginacion;
    private EditText etBuscador;
    private RecyclerView rvAlumnos, rvAlumnosSearch;
    private TextView tvPaginationInfo;
    private ImageButton btnPagePrev, btnPageNext;

    private View modalExito;
    private TextView tvModalBienvenida;

    private AlumnoAdapter adapter;
    private List<Alumno> listaCompleta = new ArrayList<>();
    private List<Alumno> listaActiva = new ArrayList<>(); 
    private List<Alumno> listaFiltrada = new ArrayList<>();
    private int paginaActual = 1;
    private final int itemsPorPagina = 3;

    private final Handler simulationHandler = new Handler();
    private int simulacionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", false);
        int colorScheme = prefs.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_asistencia);

        mainLayout = findViewById(R.id.main);
        previewView = findViewById(R.id.previewView);
        btnActivarCamara = findViewById(R.id.btn_activar_camara);
        autoCompleteSalon = findViewById(R.id.autoComplete_salon);
        cameraContainer = findViewById(R.id.camera_container);
        etBuscador = findViewById(R.id.et_buscador);
        rvAlumnos = findViewById(R.id.rv_alumnos);
        rvAlumnosSearch = findViewById(R.id.rv_alumnos_search);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutPaginacion = findViewById(R.id.layout_paginacion);
        dividerPaginacion = findViewById(R.id.divider_paginacion);
        tvPaginationInfo = findViewById(R.id.tv_pagination_info);
        btnPagePrev = findViewById(R.id.btn_page_prev);
        btnPageNext = findViewById(R.id.btn_page_next);
        modalExito = findViewById(R.id.modal_exito);
        tvModalBienvenida = findViewById(R.id.tv_modal_bienvenida);

        rvAlumnos.setLayoutManager(new LinearLayoutManager(this));
        rvAlumnosSearch.setLayoutManager(new LinearLayoutManager(this));

        generarDatosMaestros();

        adapter = new AlumnoAdapter(new ArrayList<>());
        rvAlumnos.setAdapter(adapter);

        setupPaginacion();
        setupSalonSelector();
        setupSearchAnimation();
        actualizarVistaTabla();

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (etBuscador.hasFocus() || !etBuscador.getText().toString().isEmpty() || !autoCompleteSalon.getText().toString().equals("Salon")) {
                etBuscador.setText("");
                autoCompleteSalon.setText("Salon", false);
                hideSearchMode();
            } else {
                finish();
            }
        });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) toggleCamera();
                    else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                }
        );

        btnActivarCamara.setOnClickListener(v -> checkCameraPermission());
        setupBottomNavigation(findViewById(R.id.bottom_navigation));

        iniciarSimulacionEscaneo();
        
        // Configurar auto-refresh cada 30 segundos
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        Handler refreshHandler = new Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Refrescar datos del backend cada 30 segundos
                loadAlumnosFromBackend();
                refreshHandler.postDelayed(this, 30000); // 30 segundos
            }
        };
        
        // Iniciar el auto-refresh después de 30 segundos
        refreshHandler.postDelayed(refreshRunnable, 30000);
    }

    private void iniciarSimulacionEscaneo() {
        // Cargar datos iniciales del backend
        loadAlumnosFromBackend();
        
        // TODO: Implementar escaneo QR real con cámara
        // El método procesarEscaneoQR() ya está listo para ser llamado cuando se detecte un QR
        
        Toast.makeText(this, "Sistema de asistencia conectado al servidor", Toast.LENGTH_LONG).show();
    }

    // Método para escanear QR real (se llamará cuando se implemente la cámara)
    private void procesarEscaneoQR(String qrToken) {
        // Crear request para el backend
        QrScanRequest request = new QrScanRequest(qrToken);
        
        // Llamar al backend
        ApiClient.getApiService().escanearQr(request).enqueue(new Callback<ApiResponse<QrScanResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<QrScanResponse>> call, 
                                 Response<ApiResponse<QrScanResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    QrScanResponse scanResponse = response.body().getData();
                    String mensaje = response.body().getMessage();
                    
                    // Mostrar resultado exitoso
                    runOnUiThread(() -> {
                        Toast.makeText(PanelAsistencia.this, mensaje, Toast.LENGTH_LONG).show();
                        
                        // Actualizar la lista de alumnos desde el backend
                        loadAlumnosFromBackend();
                        
                        // Mostrar modal de éxito
                        if (scanResponse != null) {
                            String nombreCompleto = scanResponse.getAlumno() != null ? 
                                scanResponse.getAlumno() : scanResponse.getDocente();
                            if (nombreCompleto != null) {
                                mostrarModalExito(nombreCompleto);
                            } else {
                                mostrarModalExito("Asistencia registrada");
                            }
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        String error = response.body() != null ? response.body().getMessage() : "Error desconocido";
                        Toast.makeText(PanelAsistencia.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<QrScanResponse>> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(PanelAsistencia.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void registrarAlumnoEnVivo(Alumno alumno) {
        // Agregar al inicio de la lista activa si no existe ya
        boolean yaExiste = listaActiva.stream().anyMatch(a -> a.nombre.equals(alumno.nombre));
        if (!yaExiste) {
            listaActiva.add(0, alumno);
            actualizarVistaTabla();
        }
    }

    private void mostrarModalExito(String nombre) {
        tvModalBienvenida.setText("Bienvenido, " + nombre);
        modalExito.bringToFront();
        modalExito.setVisibility(View.VISIBLE);
        modalExito.setTranslationY(1500f); 

        modalExito.animate()
                .translationY(0f) 
                .setDuration(600)
                .withEndAction(() -> {
                    new Handler().postDelayed(() -> {
                        ocultarModalExito();
                    }, 5000);
                })
                .start();
    }

    private void ocultarModalExito() {
        modalExito.animate()
                .translationY(1500f)
                .setDuration(600)
                .withEndAction(() -> modalExito.setVisibility(View.INVISIBLE))
                .start();
    }

    private void actualizarVistaTabla() {
        String query = etBuscador.getText().toString().toLowerCase();
        String salon = autoCompleteSalon.getText().toString();

        boolean hasQuery = !query.isEmpty();
        boolean hasSalon = !salon.equals("Salon");
        boolean isSearching = etBuscador.hasFocus() || hasQuery;

        if (isSearching && !hasSalon) {
            // Buscar en la lista activa (alumnos presentes)
            listaFiltrada = listaActiva.stream()
                    .filter(a -> a.nombre.toLowerCase().contains(query))
                    .collect(Collectors.toList());
        } else if (hasSalon) {
            // Buscar en la lista completa por salón
            listaFiltrada = listaCompleta.stream()
                    .filter(a -> a.fecha.toLowerCase().contains(salon.toLowerCase()) && 
                               (query.isEmpty() || a.nombre.toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        } else {
            // Mostrar solo alumnos presentes por defecto
            listaFiltrada = new ArrayList<>(listaActiva);
        }

        int totalItems = listaFiltrada.size();
        if (totalItems == 0) {
            rvAlumnos.setVisibility(View.GONE);
            rvAlumnosSearch.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            layoutPaginacion.setVisibility(View.GONE);
            dividerPaginacion.setVisibility(View.GONE);
            
            // Actualizar mensaje de estado vacío
            TextView tvEmptyText = findViewById(R.id.tv_empty_text);
            if (hasQuery || hasSalon) {
                tvEmptyText.setText("No se encontraron resultados");
            } else {
                tvEmptyText.setText("Aun no hay estudiantes presentes");
            }
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);

        if (isSearching || hasSalon) {
            rvAlumnos.setVisibility(View.GONE);
            rvAlumnosSearch.setVisibility(View.VISIBLE);
            rvAlumnosSearch.setAdapter(adapter);
            adapter.updateList(listaFiltrada);
            layoutPaginacion.setVisibility(View.GONE);
            dividerPaginacion.setVisibility(View.GONE);
        } else {
            rvAlumnos.setVisibility(View.VISIBLE);
            rvAlumnosSearch.setVisibility(View.GONE);
            rvAlumnos.setAdapter(adapter);

            int inicio = (paginaActual - 1) * itemsPorPagina;
            int fin = Math.min(inicio + itemsPorPagina, totalItems);
            adapter.updateList(listaFiltrada.subList(inicio, fin));

            if (totalItems > itemsPorPagina) {
                layoutPaginacion.setVisibility(View.VISIBLE);
                dividerPaginacion.setVisibility(View.VISIBLE);
                int totalPaginas = (int) Math.ceil((double) totalItems / itemsPorPagina);
                tvPaginationInfo.setText("Pagina " + paginaActual + " de " + totalPaginas);
                btnPagePrev.setEnabled(paginaActual > 1);
                btnPageNext.setEnabled(paginaActual < totalPaginas);
            } else {
                layoutPaginacion.setVisibility(View.GONE);
                dividerPaginacion.setVisibility(View.GONE);
            }
        }
    }

    private void generarDatosMaestros() {
        // Cargar alumnos desde el backend
        loadAlumnosFromBackend();
    }

    private void loadAlumnosFromBackend() {
        String token = ApiClient.getAuthToken(this);
        ApiClient.getApiService().getAsistenciaAlumnos(token, null, null).enqueue(new Callback<ApiResponse<List<AlumnoAsistencia>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AlumnoAsistencia>>> call, Response<ApiResponse<List<AlumnoAsistencia>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AlumnoAsistencia> alumnosBackend = response.body().getData();
                    
                    // Convertir AlumnoAsistencia a Alumno (clase local)
                    listaCompleta.clear();
                    listaActiva.clear();
                    
                    for (AlumnoAsistencia alumnoBackend : alumnosBackend) {
                        String hora = alumnoBackend.getHora_entrada() != null ? alumnoBackend.getHora_entrada() : null;
                        String estado = alumnoBackend.getEstado_entrada() != null ? alumnoBackend.getEstado_entrada() : "ausente";
                        
                        Alumno alumno = new Alumno(alumnoBackend.getNombre_completo(), alumnoBackend.getSalon(), hora);
                        listaCompleta.add(alumno);
                        
                        // Solo agregar a listaActiva si tiene asistencia registrada (presente o tardanza)
                        if (hora != null && !hora.isEmpty() && !estado.equals("ausente")) {
                            listaActiva.add(alumno);
                        }
                    }
                    
                    // Actualizar la vista
                    runOnUiThread(() -> {
                        actualizarVistaTabla();
                        String mensaje = "Cargados " + listaCompleta.size() + " alumnos del servidor";
                        if (listaActiva.size() > 0) {
                            mensaje += " (" + listaActiva.size() + " presentes)";
                        }
                        Toast.makeText(PanelAsistencia.this, mensaje, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        String error = response.body() != null ? response.body().getMessage() : "Error al cargar datos";
                        Toast.makeText(PanelAsistencia.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        // NO cargar datos de ejemplo, dejar vacío
                        listaCompleta.clear();
                        listaActiva.clear();
                        actualizarVistaTabla();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<AlumnoAsistencia>>> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(PanelAsistencia.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    // NO cargar datos de ejemplo, dejar vacío
                    listaCompleta.clear();
                    listaActiva.clear();
                    actualizarVistaTabla();
                });
            }
        });
    }

    private void setupBottomNavigation(BottomNavigationView bottomNav) {
        int colorSeleccionado;
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            colorSeleccionado = typedValue.data;
        } else {
            colorSeleccionado = Color.parseColor("#BA1924");
        }

        int[][] states = new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked} };
        int[] colors = new int[]{ colorSeleccionado, Color.parseColor("#5E5F60") };
        ColorStateList navTint = new ColorStateList(states, colors);
        bottomNav.setItemIconTintList(navTint);
        bottomNav.setItemTextColor(navTint);
        bottomNav.setSelectedItemId(R.id.nav_more);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                return true;
            }
            return true;
        });
    }

    private void setupSearchAnimation() {
        etBuscador.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showSearchMode(); });
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                paginaActual = 1; actualizarVistaTabla();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPaginacion() {
        btnPagePrev.setOnClickListener(v -> { if (paginaActual > 1) { paginaActual--; actualizarVistaTabla(); } });
        btnPageNext.setOnClickListener(v -> {
            int totalPaginas = (int) Math.ceil((double) listaFiltrada.size() / itemsPorPagina);
            if (paginaActual < totalPaginas) { paginaActual++; actualizarVistaTabla(); }
        });
    }

    private void showSearchMode() {
        TransitionManager.beginDelayedTransition(mainLayout, new AutoTransition());
        cameraContainer.setVisibility(View.GONE);
        btnActivarCamara.setVisibility(View.GONE);
        actualizarVistaTabla();
    }

    private void hideSearchMode() {
        TransitionManager.beginDelayedTransition(mainLayout, new AutoTransition());
        cameraContainer.setVisibility(View.VISIBLE);
        btnActivarCamara.setVisibility(View.VISIBLE);
        etBuscador.clearFocus();
        actualizarVistaTabla();
    }

    private void setupSalonSelector() {
        // Cargar salones desde el backend
        loadSalonesFromBackend();
        
        autoCompleteSalon.setOnItemClickListener((parent, view, position, id) -> { 
            showSearchMode(); 
            actualizarVistaTabla(); 
        });
    }
    
    private void loadSalonesFromBackend() {
        String token = ApiClient.getAuthToken(this);
        ApiClient.getApiService().getSalones(token).enqueue(new Callback<ApiResponse<List<Salon>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Salon>>> call, Response<ApiResponse<List<Salon>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Salon> salones = response.body().getData();
                    List<String> nombresSalones = new ArrayList<>();
                    
                    for (Salon salon : salones) {
                        nombresSalones.add(salon.getNombre());
                    }
                    
                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(PanelAsistencia.this, 
                            android.R.layout.simple_dropdown_item_1line, nombresSalones);
                        autoCompleteSalon.setAdapter(adapter);
                    });
                } else {
                    // Usar salones por defecto si falla
                    runOnUiThread(() -> {
                        String[] salonesDefault = {"1ro A", "1ro B", "1ro C", "1ro D", "1ro E", "2do A", "2do B", "2do C", "2do D", "2do E"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(PanelAsistencia.this, 
                            android.R.layout.simple_dropdown_item_1line, salonesDefault);
                        autoCompleteSalon.setAdapter(adapter);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Salon>>> call, Throwable t) {
                // Usar salones por defecto si falla la conexión
                runOnUiThread(() -> {
                    String[] salonesDefault = {"1ro A", "1ro B", "1ro C", "1ro D", "1ro E", "2do A", "2do B", "2do C", "2do D", "2do E"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(PanelAsistencia.this, 
                        android.R.layout.simple_dropdown_item_1line, salonesDefault);
                    autoCompleteSalon.setAdapter(adapter);
                });
            }
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) toggleCamera();
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void toggleCamera() { if (isCameraActive) stopCamera(); else startCamera(); }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);
                previewView.setVisibility(View.VISIBLE);
                isCameraActive = true;
                btnActivarCamara.setText("Desactivar Camara");
                btnActivarCamara.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                btnActivarCamara.setTextColor(Color.BLACK);
            } catch (Exception e) { Toast.makeText(this, "Error cámara", Toast.LENGTH_SHORT).show(); }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            isCameraActive = false;
            previewView.setVisibility(View.INVISIBLE);
            btnActivarCamara.setText("Activar Camara");
            btnActivarCamara.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BA1924")));
            btnActivarCamara.setTextColor(Color.WHITE);
        }
    }

    static class Alumno {
        String nombre, fecha, hora;
        Alumno(String nombre, String fecha, String hora) { this.nombre = nombre; this.fecha = fecha; this.hora = hora; }
    }

    class AlumnoAdapter extends RecyclerView.Adapter<AlumnoAdapter.ViewHolder> {
        private List<Alumno> alumnos;
        AlumnoAdapter(List<Alumno> alumnos) { this.alumnos = alumnos; }
        void updateList(List<Alumno> newList) { this.alumnos = newList; notifyDataSetChanged(); }
        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alumno_asistencia, parent, false));
        }
        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            Alumno a = alumnos.get(position);
            holder.tvNombre.setText(a.nombre);
            holder.tvFecha.setText(a.fecha);
            if (a.hora == null || a.hora.isEmpty()) {
                holder.tvHora.setText("Ausente");
                holder.tvHora.setTextColor(Color.parseColor("#BA1924"));
            } else {
                holder.tvHora.setText(a.hora);
                holder.tvHora.setTextColor(Color.parseColor("#27AE60"));
            }
        }
        @Override public int getItemCount() { return alumnos.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvFecha, tvHora;
            ViewHolder(View v) {
                super(v);
                tvNombre = v.findViewById(R.id.tv_nombre_alumno);
                tvFecha = v.findViewById(R.id.tv_salon_alumno);
                tvHora = v.findViewById(R.id.tv_estado_entrada);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        simulationHandler.removeCallbacksAndMessages(null);
    }
}
