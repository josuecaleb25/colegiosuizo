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

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

        mostrarDialogoInicioAsistencia();
        iniciarSimulacionEscaneo();
    }

    private void mostrarDialogoInicioAsistencia() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_inicio_asistencia, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setCancelable(false) // Forzar elección
                .create();

        // Configurar Fecha Dinámica
        TextView tvFecha = dialogView.findViewById(R.id.tv_fecha_dialog);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "PE"));
        String fechaActual = sdf.format(Calendar.getInstance().getTime());
        tvFecha.setText(fechaActual.substring(0, 1).toUpperCase() + fechaActual.substring(1));

        dialogView.findViewById(R.id.btn_volver_asistencia).setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Regresa a la pantalla anterior
        });

        dialogView.findViewById(R.id.btn_comenzar_asistencia).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Panel listo para registro", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void iniciarSimulacionEscaneo() {
        // Crear una lista de escaneo aleatoria con solo algunos alumnos (Simular ausentismo)
        List<Alumno> poolEscaneo = new ArrayList<>(listaCompleta);
        java.util.Collections.shuffle(poolEscaneo);
        
        // Solo el 60% de los alumnos asistirán hoy
        int limiteAsistencia = (int) (poolEscaneo.size() * 0.6);
        List<Alumno> alumnosQueAsistiran = poolEscaneo.subList(0, limiteAsistencia);

        simulationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (simulacionIndex < alumnosQueAsistiran.size()) {
                    Alumno alumno = alumnosQueAsistiran.get(simulacionIndex);
                    
                    // Marcar como presente con la hora actual
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    alumno.hora = sdf.format(Calendar.getInstance().getTime());
                    
                    simulacionIndex++;

                    registrarAlumnoEnVivo(alumno);
                    mostrarModalExito(alumno.nombre);

                    // Programar siguiente escaneo
                    simulationHandler.postDelayed(this, 10000);
                }
            }
        }, 10000);
    }

    private void registrarAlumnoEnVivo(Alumno alumno) {
        listaActiva.add(0, alumno);
        actualizarVistaTabla();
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
            listaFiltrada = listaActiva.stream()
                    .filter(a -> a.nombre.toLowerCase().contains(query))
                    .collect(Collectors.toList());
        } else if (hasSalon) {
            listaFiltrada = listaCompleta.stream()
                    .filter(a -> a.fecha.equals(salon) && (query.isEmpty() || a.nombre.toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        } else {
            listaFiltrada = new ArrayList<>(listaActiva);
        }

        int totalItems = listaFiltrada.size();
        if (totalItems == 0) {
            rvAlumnos.setVisibility(View.GONE);
            rvAlumnosSearch.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            layoutPaginacion.setVisibility(View.GONE);
            dividerPaginacion.setVisibility(View.GONE);
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
        cargarAlumnosDesdeBackend();
    }

    private void cargarAlumnosDesdeBackend() {
        RetrofitClient.getApiService().getAlumnosAsistencia(null).enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>> call, retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno> alumnosApi = response.body().getData();
                    
                    listaCompleta.clear();
                    listaActiva.clear();
                    
                    for (com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno a : alumnosApi) {
                        Alumno alumno = new Alumno(
                            a.getNombre_completo(),
                            a.getSalon(),
                            a.getHora_registro()
                        );
                        listaCompleta.add(alumno);
                        
                        // Si ya tiene asistencia registrada, agregarlo a listaActiva
                        if (a.getHora_registro() != null && !a.getHora_registro().isEmpty()) {
                            listaActiva.add(alumno);
                        }
                    }
                    
                    actualizarVistaTabla();
                } else {
                    Toast.makeText(PanelAsistencia.this, "Error al cargar alumnos del backend", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>> call, Throwable t) {
                Toast.makeText(PanelAsistencia.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
            } else if (id == R.id.nav_homework) {
                startActivity(new Intent(this, CursosActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_horarios) {
                startActivity(new Intent(this, HorariosActivity.class));
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
        String[] salones = {"1ro A", "2do B", "3ro C", "4to A", "5to B"};
        autoCompleteSalon.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, salones));
        autoCompleteSalon.setOnItemClickListener((parent, view, position, id) -> { showSearchMode(); actualizarVistaTabla(); });
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
