package com.example.ieperuanosuizoapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.media.MediaPlayer;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
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
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno;
import com.example.ieperuanosuizoapp.api.models.AsistenciaVivo;
import com.example.ieperuanosuizoapp.api.models.EscanearQrData;
import com.example.ieperuanosuizoapp.api.models.SesionAsistencia;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.common.util.concurrent.ListenableFuture;

public class PanelAsistencia extends AppCompatActivity {

    private PreviewView previewView;
    private MaterialButton btnActivarCamara, btnCulminarAsistencia;
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

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final com.google.mlkit.vision.barcode.BarcodeScanner barcodeScanner = BarcodeScanning.getClient();

    private volatile boolean procesandoQr = false;
    private long ultimoScanMs;
    private String ultimoCodigoLeido;

    private boolean sesionComenzada = false;
    private int registrosEnSesion = 0;
    private boolean verificandoSesion = false;
    private boolean dialogoInicioVisible = false;
    private boolean dialogoErrorVisible = false;
    private boolean falloRedMostrado = false;

    // ========== TIEMPO REAL COMPARTIDO ==========
    private String sesionActualId = null;
    private String sesionActualFecha = null;
    private Handler pollingHandler = new Handler();
    private boolean pollingActivo = false;
    private static final int POLLING_INTERVALO_MS = 3000;
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            sincronizarAsistencias();
            if (pollingActivo) {
                pollingHandler.postDelayed(this, POLLING_INTERVALO_MS);
            }
        }
    };

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
        btnCulminarAsistencia = findViewById(R.id.btn_culminar_asistencia);
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

        // LIMPIEZA TOTAL AL INICIAR
        limpiarTodosDatos();

        adapter = new AlumnoAdapter(new ArrayList<>());
        rvAlumnos.setAdapter(adapter);

        setupPaginacion();
        setupSalonSelector();
        setupSearchAnimation();
        actualizarVistaTabla();

        // Al presionar arriba (título), volvemos a la cámara si estábamos buscando
        findViewById(R.id.tv_title).setOnClickListener(v -> hideSearchMode());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (!isCameraActive) {
                            startCamera();
                        }
                    } else {
                        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnActivarCamara.setOnClickListener(v -> {
            if (isCameraActive) {
                stopCamera();
            } else {
                checkCameraPermissionAndStartOnly();
            }
        });

        btnCulminarAsistencia.setOnClickListener(v -> {
            if (sesionComenzada) {
                mostrarDialogoConfirmarCulminarSesion();
            } else {
                Toast.makeText(this, "Inicia la asistencia primero", Toast.LENGTH_SHORT).show();
            }
        });

        setupBottomNavigation(findViewById(R.id.bottom_navigation));

    }

    @Override
    protected void onResume() {
        super.onResume();
        limpiarTodosDatos();
        verificarAsistenciaExistenteHoy();
    }

    @Override
    protected void onStop() {
        detenerPolling();
        stopCamera();
        super.onStop();
    }

    private void limpiarTodosDatos() {
        detenerPolling();
        listaCompleta.clear();
        listaActiva.clear();
        listaFiltrada.clear();
        registrosEnSesion = 0;
        sesionComenzada = false;
        sesionActualId = null;
        sesionActualFecha = null;
        ultimoCodigoLeido = null;
        ultimoScanMs = 0;
        procesandoQr = false;
    }

    private void verificarAsistenciaExistenteHoy() {
        if (verificandoSesion) return;
        verificandoSesion = true;

        // Buscar la sesión activa del día
        RetrofitClient.getApiService().getSesionActiva().enqueue(new retrofit2.Callback<ApiResponse<SesionAsistencia>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<SesionAsistencia>> call, retrofit2.Response<ApiResponse<SesionAsistencia>> response) {
                verificandoSesion = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SesionAsistencia sesion = response.body().getData();
                    if (sesion != null) {
                        activarSesion(sesion, false);
                        Toast.makeText(PanelAsistencia.this, "Te uniste a la sesión de asistencia activa", Toast.LENGTH_SHORT).show();
                    } else {
                        sesionActualId = null;
                        sesionComenzada = false;
                        cargarAlumnosDesdeBackend();
                        mostrarDialogoInicioAsistencia();
                    }
                } else {
                    if (response.code() != 401) {
                        mostrarErrorRecuperacion("El servidor no pudo verificar la sesión de asistencia.");
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<SesionAsistencia>> call, Throwable t) {
                verificandoSesion = false;
                mostrarErrorRecuperacion("No hay conexión. El escáner permanecerá bloqueado hasta recuperar la sesión.");
            }
        });
    }

    private void activarSesion(SesionAsistencia sesion, boolean iniciarCamara) {
        if (sesion == null || sesion.getId() == null || sesion.getId().trim().isEmpty()) {
            mostrarErrorRecuperacion("El servidor devolvió una sesión sin identificador válido.");
            return;
        }
        sesionActualId = sesion.getId();
        sesionActualFecha = sesion.getFecha();
        sesionComenzada = true;
        falloRedMostrado = false;
        btnActivarCamara.setEnabled(true);
        cargarAlumnosDesdeBackend();
        iniciarPolling();
        if (iniciarCamara) checkCameraPermissionAndStartOnly();
    }

    private void mostrarErrorRecuperacion(String mensaje) {
        detenerPolling();
        stopCamera();
        btnActivarCamara.setEnabled(false);
        if (dialogoErrorVisible || isFinishing() || isDestroyed()) return;
        dialogoErrorVisible = true;
        new MaterialAlertDialogBuilder(this)
                .setTitle("No se pudo recuperar la sesión")
                .setMessage(mensaje)
                .setCancelable(false)
                .setNegativeButton("Volver", (d, w) -> {
                    dialogoErrorVisible = false;
                    d.dismiss();
                    finish();
                })
                .setPositiveButton("Reintentar", (d, w) -> {
                    dialogoErrorVisible = false;
                    d.dismiss();
                    verificarAsistenciaExistenteHoy();
                })
                .show();
    }

    private void crearSesionEnBackend(Runnable onSuccess) {
        HashMap<String, String> body = new HashMap<>();
        RetrofitClient.getApiService().crearSesion(body).enqueue(new retrofit2.Callback<ApiResponse<SesionAsistencia>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<SesionAsistencia>> call, retrofit2.Response<ApiResponse<SesionAsistencia>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SesionAsistencia sesion = response.body().getData();
                    if (sesion != null && sesion.getId() != null && !sesion.getId().trim().isEmpty()) {
                        sesionActualId = sesion.getId();
                        sesionActualFecha = sesion.getFecha();
                        onSuccess.run();
                    } else {
                        mostrarErrorRecuperacion("No se recibió un identificador válido para la sesión.");
                    }
                } else if (response.code() == 409) {
                    recuperarSesionCreadaPorOtraTablet(onSuccess);
                } else {
                    Toast.makeText(PanelAsistencia.this, "No se pudo iniciar la sesión en el servidor", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<SesionAsistencia>> call, Throwable t) {
                mostrarErrorRecuperacion("No hay conexión. No se inició ninguna sesión y no se registrarán escaneos.");
            }
        });
    }

    private void recuperarSesionCreadaPorOtraTablet(Runnable onSuccess) {
        RetrofitClient.getApiService().getSesionActiva().enqueue(new retrofit2.Callback<ApiResponse<SesionAsistencia>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<SesionAsistencia>> call, retrofit2.Response<ApiResponse<SesionAsistencia>> response) {
                SesionAsistencia sesion = response.body() != null ? response.body().getData() : null;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && sesion != null && sesion.getId() != null && !sesion.getId().trim().isEmpty()) {
                    sesionActualId = sesion.getId();
                    sesionActualFecha = sesion.getFecha();
                    Toast.makeText(PanelAsistencia.this, "Te uniste a la sesión creada en otra tablet", Toast.LENGTH_SHORT).show();
                    onSuccess.run();
                } else if (response.code() != 401) {
                    mostrarErrorRecuperacion("Otra tablet inició la sesión, pero no fue posible recuperarla.");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<SesionAsistencia>> call, Throwable t) {
                mostrarErrorRecuperacion("Otra tablet inició la sesión, pero se perdió la conexión al recuperarla.");
            }
        });
    }

    private void iniciarPolling() {
        if (pollingActivo) return;
        pollingActivo = true;
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVALO_MS);
    }

    private void detenerPolling() {
        pollingActivo = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void sincronizarAsistencias() {
        String hoy = sesionActualFecha;
        String sesionId = sesionActualId;
        if (hoy == null || sesionId == null || sesionId.trim().isEmpty()) return;

        RetrofitClient.getApiService().getAsistenciasDelDia(hoy, sesionId).enqueue(new retrofit2.Callback<ApiResponse<List<AsistenciaVivo>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<List<AsistenciaVivo>>> call, retrofit2.Response<ApiResponse<List<AsistenciaVivo>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    if (response.code() == 404 || response.code() == 409) {
                        manejarSesionCerrada();
                    }
                    return;
                }
                falloRedMostrado = false;
                btnActivarCamara.setEnabled(true);
                List<AsistenciaVivo> asistencias = response.body().getData();
                if (asistencias == null) return;

                // Construir mapa de persona_id -> asistencia
                java.util.Map<String, AsistenciaVivo> mapa = new java.util.HashMap<>();
                for (AsistenciaVivo a : asistencias) {
                    if (a.getPersona_id() != null) {
                        mapa.put(a.getPersona_id(), a);
                    }
                }

                // Actualizar la lista local marcando quiénes escanearon, incluso los de otros dispositivos
                for (Alumno alumno : listaCompleta) {
                    AsistenciaVivo as = mapa.get(alumno.persona_id);
                    if (as != null) {
                        alumno.hora = as.getHora_entrada();
                        alumno.estado = as.getEstado();
                        if (!listaActiva.contains(alumno)) {
                            listaActiva.add(alumno);
                        }
                    }
                }
                actualizarVistaTabla();
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<List<AsistenciaVivo>>> call, Throwable t) {
                stopCamera();
                btnActivarCamara.setEnabled(false);
                if (!falloRedMostrado) {
                    falloRedMostrado = true;
                    Toast.makeText(PanelAsistencia.this, "Conexión perdida: el escáner se pausó", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void manejarSesionCerrada() {
        detenerPolling();
        stopCamera();
        sesionComenzada = false;
        sesionActualId = null;
        sesionActualFecha = null;
        mostrarDialogoAsistenciaYaCompletada();
    }

    private void mostrarDialogoAsistenciaYaCompletada() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Asistencia completada")
                .setMessage("Ya se ha registrado y confirmado la asistencia el día de hoy. No puedes realizar un nuevo registro.")
                .setCancelable(false)
                .setPositiveButton("Entendido", (d, w) -> {
                    d.dismiss();
                    finish();
                })
                .show();
    }

    private void mostrarDialogoInicioAsistencia() {
        if (dialogoInicioVisible || isFinishing() || isDestroyed()) return;
        dialogoInicioVisible = true;
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
            dialogoInicioVisible = false;
            dialog.dismiss();
            finish(); // Regresa a la pantalla anterior
        });

        dialogView.findViewById(R.id.btn_comenzar_asistencia).setOnClickListener(v -> {
            dialogoInicioVisible = false;
            dialog.dismiss();
            // Crear la sesión en el backend para que otros puedan unirse
            crearSesionEnBackend(() -> {
                sesionComenzada = true;
                registrosEnSesion = 0;
                cargarAlumnosDesdeBackend(); // Refrescar para mostrar datos reales del día
                iniciarPolling();
                checkCameraPermissionAndStartOnly();
            });
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void mostrarDialogoConfirmarCulminarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("¿Culminar registro?")
                .setMessage("Se dará por terminado este registro de asistencia y se guardará el resumen en el historial.")
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .setPositiveButton("Culminar", (d, w) -> {
                    d.dismiss();
                    detenerPolling();
                    stopCamera();
                    String fechaIso = sesionActualFecha != null
                            ? sesionActualFecha
                            : new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
                    List<AsistenciaAlumno> snapshot = construirListaConfirmacionDesdeAlumnos();
                    cerrarSesionEnBackend(() -> mostrarModalConfirmacionAsistenciaDia(fechaIso, snapshot));
                })
                .show();
    }

    private void cerrarSesionEnBackend(Runnable onSuccess) {
        if (sesionActualId == null || sesionActualId.trim().isEmpty()) {
            Toast.makeText(this, "No hay una sesión válida para cerrar", Toast.LENGTH_LONG).show();
            return;
        }
        final String id = sesionActualId;
        RetrofitClient.getApiService().cerrarSesion(id).enqueue(new retrofit2.Callback<ApiResponse<SesionAsistencia>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<SesionAsistencia>> call, retrofit2.Response<ApiResponse<SesionAsistencia>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    sesionActualId = null;
                    sesionActualFecha = null;
                    sesionComenzada = false;
                    onSuccess.run();
                } else if (response.code() != 401) {
                    Toast.makeText(PanelAsistencia.this, "No se pudo cerrar la sesión. Puedes volver a intentarlo.", Toast.LENGTH_LONG).show();
                    btnActivarCamara.setEnabled(true);
                    iniciarPolling();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<SesionAsistencia>> call, Throwable t) {
                Toast.makeText(PanelAsistencia.this, "Sin conexión: la sesión sigue abierta", Toast.LENGTH_LONG).show();
                btnActivarCamara.setEnabled(true);
                iniciarPolling();
            }
        });
    }

    private void checkCameraPermissionAndStartOnly() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (!isCameraActive) {
                startCamera();
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void procesarQrToken(String token) {
        if (!sesionComenzada || !isCameraActive || procesandoQr) {
            return;
        }
        if (sesionActualId == null || sesionActualId.trim().isEmpty()) {
            stopCamera();
            mostrarErrorRecuperacion("La sesión perdió su identificador. Debe recuperarse antes de escanear.");
            return;
        }
        long now = System.currentTimeMillis();
        if (ultimoCodigoLeido != null && token.equals(ultimoCodigoLeido) && (now - ultimoScanMs) < 3000) {
            return;
        }
        ultimoScanMs = now;
        ultimoCodigoLeido = token;

        procesandoQr = true;
        HashMap<String, String> body = new HashMap<>();
        body.put("qr_token", token);
        body.put("sesion_id", sesionActualId);
        RetrofitClient.getApiService().escanearQrAsistencia(body).enqueue(new Callback<ApiResponse<EscanearQrData>>() {
            @Override
            public void onResponse(Call<ApiResponse<EscanearQrData>> call, Response<ApiResponse<EscanearQrData>> response) {
                procesandoQr = false;
                if (!response.isSuccessful()) {
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String errorJson = errorBody.string();
                            ApiResponse<?> errorRes = new com.google.gson.Gson().fromJson(errorJson, ApiResponse.class);
                            String msg = (errorRes != null && errorRes.getMessage() != null) ? errorRes.getMessage() : "Error del servidor";

                            if (response.code() == 409) {
                                manejarSesionCerrada();
                            } else {
                                Toast.makeText(PanelAsistencia.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(PanelAsistencia.this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                ApiResponse<EscanearQrData> res = response.body();
                if (res != null && res.isSuccess() && res.getData() != null) {
                    if (res.getData().isReused()) {
                        mostrarModalAlumnoYaRegistrado(res.getMessage());
                        sincronizarAsistencias();
                        return;
                    }
                    registrosEnSesion++;
                    String nombre = res.getData().getAlumno();
                    String hora = res.getData().getHora();
                    String estado = res.getData().getEstado();
                    
                    // Actualizar la lista local con la hora del escaneo
                    if (nombre != null && hora != null) {
                        for (Alumno alumno : listaCompleta) {
                            if (alumno.nombre.equals(nombre)) {
                                alumno.hora = hora;
                                alumno.estado = estado;
                                if (!listaActiva.contains(alumno)) {
                                    listaActiva.add(alumno);
                                }
                                break;
                            }
                        }
                        actualizarVistaTabla();
                        mostrarModalExito(nombre);
                    }
                } else {
                    String msg = res != null && res.getMessage() != null ? res.getMessage() : "No se pudo registrar";
                    Toast.makeText(PanelAsistencia.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<EscanearQrData>> call, Throwable t) {
                procesandoQr = false;
                Toast.makeText(PanelAsistencia.this, "Sin conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarModalAlumnoYaRegistrado(String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("El alumno ya escaneó su QR")
                .setMessage(mensaje != null ? mensaje : "Este alumno ya cuenta con un registro de asistencia para el día de hoy.")
                .setPositiveButton("Aceptar", (d, w) -> d.dismiss())
                .show();
    }

    private void mostrarModalExito(String nombre) {
        tvModalBienvenida.setText("Bienvenido, " + nombre);
        modalExito.bringToFront();
        modalExito.setVisibility(View.VISIBLE);
        modalExito.setTranslationY(1500f);

        try {
            MediaPlayer.create(this, R.raw.soundassist).start();
        } catch (Exception ignored) {}

        modalExito.animate()
                .translationY(0f) 
                .setDuration(600)
                .withEndAction(() -> {
                    new Handler().postDelayed(() -> {
                        ocultarModalExito();
                    }, 3000);
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
                        // NUNCA mostrar hora_registro de asistencias previas
                        // Solo mostrar si fue escaneado EN ESTA SESIÓN
                        // IMPORTANTE: Guardar persona_id para usarlo al culminar
                        Alumno alumno = new Alumno(
                            a.getNombre_completo(),
                            a.getSalon(),
                            null,  // Siempre null, la hora se actualiza solo cuando se escanea QR
                            a.getPersona_id()  // Guardar persona_id
                        );
                        listaCompleta.add(alumno);
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
        NavigationRoleHelper.apply(this, bottomNav);
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
        btnCulminarAsistencia.setVisibility(View.GONE);
        actualizarVistaTabla();
    }

    private void hideSearchMode() {
        TransitionManager.beginDelayedTransition(mainLayout, new AutoTransition());
        etBuscador.setText("");
        autoCompleteSalon.setText("Salon", false);
        cameraContainer.setVisibility(View.VISIBLE);
        btnActivarCamara.setVisibility(View.VISIBLE);
        btnCulminarAsistencia.setVisibility(View.VISIBLE);
        etBuscador.clearFocus();
        actualizarVistaTabla();
    }

    private void setupSalonSelector() {
        String[] salones = {"1ro A", "2do B", "3ro C", "4to A", "5to B"};
        autoCompleteSalon.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, salones));
        autoCompleteSalon.setOnItemClickListener((parent, view, position, id) -> { showSearchMode(); actualizarVistaTabla(); });
    }

    private String formatTituloDesdeIso(String fechaIso) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = iso.parse(fechaIso);
            SimpleDateFormat fmt = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "PE"));
            String s = fmt.format(d);
            return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
        } catch (ParseException e) {
            return fechaIso;
        }
    }

    private boolean esTardanzaPorHora(String horaStr) {
        if (horaStr == null || horaStr.isEmpty()) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date d = sdf.parse(horaStr);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            int mins = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            return mins > 8 * 60 + 15;
        } catch (ParseException e) {
            return false;
        }
    }

    private List<AsistenciaAlumno> construirListaConfirmacionDesdeAlumnos() {
        List<AsistenciaAlumno> out = new ArrayList<>();
        int i = 0;
        for (Alumno al : listaCompleta) {
            AsistenciaAlumno m = new AsistenciaAlumno();
            m.setId("local-" + (i++));
            m.setPersona_id(al.persona_id);  // IMPORTANTE: Setear persona_id
            m.setNombre_completo(al.nombre);
            m.setSalon(al.fecha);
            boolean tiene = al.hora != null && !al.hora.isEmpty();
            m.setHora_registro(tiene ? al.hora : null);
            if (!tiene) {
                m.setEstado_entrada("ausente");
            } else if (esTardanzaPorHora(al.hora)) {
                m.setEstado_entrada("tardanza");
            } else {
                m.setEstado_entrada("presente");
            }
            out.add(m);
        }
        return out;
    }

    private void mostrarModalConfirmacionAsistenciaDia(String fechaIso, List<AsistenciaAlumno> snapshot) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_confirmacion_asistencia_dia, null);
        TextView tit = v.findViewById(R.id.tv_confirm_titulo);
        tit.setText("Asistencia del " + formatTituloDesdeIso(fechaIso) + " confirmada");
        int at = 0;
        int tard = 0;
        int aus = 0;
        for (AsistenciaAlumno a : snapshot) {
            boolean tieneHora = a.getHora_registro() != null && !a.getHora_registro().isEmpty();
            if (!tieneHora) {
                aus++;
            } else {
                String e = a.getEstado_entrada() != null ? a.getEstado_entrada().toLowerCase(Locale.ROOT) : "";
                if (e.contains("tardanza")) {
                    tard++;
                } else {
                    at++;
                }
            }
        }
        ((TextView) v.findViewById(R.id.tv_confirm_asistieron)).setText(String.valueOf(at));
        ((TextView) v.findViewById(R.id.tv_confirm_tardanza)).setText(String.valueOf(tard));
        ((TextView) v.findViewById(R.id.tv_confirm_ausentes)).setText(String.valueOf(aus));

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this);
        b.setView(v);
        AlertDialog ad = b.create();
        ad.setCancelable(false);
        MaterialButton btn = v.findViewById(R.id.btn_confirm_aceptar);
        btn.setOnClickListener(x -> {
            ad.dismiss();
            Intent intent = new Intent(PanelAsistencia.this, GestionAsistenciaActivity.class);
            startActivity(intent);
            finish();
        });
        ad.show();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!sesionComenzada) {
                        imageProxy.close();
                        return;
                    }
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage == null) {
                        imageProxy.close();
                        return;
                    }
                    InputImage image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.getImageInfo().getRotationDegrees());
                    barcodeScanner.process(image)
                            .addOnCompleteListener(task -> {
                                imageProxy.close();
                                if (!task.isSuccessful() || task.getResult() == null) {
                                    return;
                                }
                                List<Barcode> codes = task.getResult();
                                if (codes.isEmpty()) {
                                    return;
                                }
                                String raw = codes.get(0).getRawValue();
                                if (raw == null || raw.trim().isEmpty()) {
                                    return;
                                }
                                runOnUiThread(() -> procesarQrToken(raw.trim()));
                            });
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
                previewView.setVisibility(View.VISIBLE);
                isCameraActive = true;
                btnActivarCamara.setText("Desactivar Camara");
                btnActivarCamara.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                btnActivarCamara.setTextColor(Color.BLACK);
            } catch (Exception e) {
                Toast.makeText(this, "Error cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        isCameraActive = false;
        previewView.setVisibility(View.INVISIBLE);
        btnActivarCamara.setText("Activar Camara");
        btnActivarCamara.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BA1924")));
        btnActivarCamara.setTextColor(Color.WHITE);
    }

    static class Alumno {
        String nombre, fecha, hora, persona_id, estado;
        Alumno(String nombre, String fecha, String hora, String persona_id) { 
            this.nombre = nombre; 
            this.fecha = fecha; 
            this.hora = hora; 
            this.persona_id = persona_id;
            this.estado = null;
        }
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
            } else if ("tardanza".equals(a.estado)) {
                holder.tvHora.setText(a.hora);
                holder.tvHora.setTextColor(Color.parseColor("#FF9800"));
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
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerPolling();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }

    /**
     * Guardar alumnos ausentes en el backend al culminar
     */
    private void guardarAusentesEnBackend(List<AsistenciaAlumno> snapshot, Runnable onComplete) {
        // Filtrar solo los ausentes (los que no tienen hora_registro)
        List<AsistenciaAlumno> ausentes = new ArrayList<>();
        for (AsistenciaAlumno alumno : snapshot) {
            if (alumno.getHora_registro() == null || alumno.getHora_registro().isEmpty()) {
                ausentes.add(alumno);
            }
        }

        if (ausentes.isEmpty()) {
            // No hay ausentes, continuar
            onComplete.run();
            return;
        }

        // Mostrar progreso
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Guardando registro de asistencia...");
        progress.setCancelable(false);
        progress.show();

        // Preparar array de ausentes para envío batch
        org.json.JSONArray ausentesArray = new org.json.JSONArray();
        for (AsistenciaAlumno alumno : ausentes) {
            try {
                org.json.JSONObject ausenteObj = new org.json.JSONObject();
                ausenteObj.put("persona_id", alumno.getPersona_id());
                ausentesArray.put(ausenteObj);
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
        }

        // Crear body con array de ausentes y fecha
        org.json.JSONObject jsonBody = new org.json.JSONObject();
        try {
            jsonBody.put("ausentes", ausentesArray);
            jsonBody.put("fecha", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonBody.toString(),
                    okhttp3.MediaType.parse("application/json")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(com.example.ieperuanosuizoapp.api.ApiConfig.BASE_URL + "asistencia/registrar-ausentes-batch")
                    .post(body)
                    .build();

            // Crear cliente con timeout más largo para batch grande
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // UNA SOLA petición para todos los ausentes
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(PanelAsistencia.this, 
                            "Error al guardar ausentes: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                        // NO llamar onComplete aquí - el guardado falló
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        
                        runOnUiThread(() -> {
                            progress.dismiss();
                            
                            if (response.isSuccessful()) {
                                Toast.makeText(PanelAsistencia.this, 
                                    "Registro guardado exitosamente", 
                                    Toast.LENGTH_SHORT).show();
                                // Solo llamar onComplete cuando el guardado fue exitoso
                                onComplete.run();
                            } else {
                                Toast.makeText(PanelAsistencia.this, 
                                    "Error al guardar: " + response.code(), 
                                    Toast.LENGTH_LONG).show();
                                // NO llamar onComplete - el guardado falló
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(PanelAsistencia.this, 
                                "Error procesando respuesta", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (org.json.JSONException e) {
            runOnUiThread(() -> {
                progress.dismiss();
                Toast.makeText(this, "Error al preparar datos", Toast.LENGTH_SHORT).show();
                // NO llamar onComplete - hubo error
            });
        }
    }
}
