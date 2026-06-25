package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class GestionAsistenciaActivity extends AppCompatActivity {

    private com.google.android.material.progressindicator.CircularProgressIndicator progressHistorial;
    private RecyclerView rvHistorial;
    private LinearLayout layoutEmpty;

    private HistorialAdapter historialAdapter;

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
        setContentView(R.layout.gestion_asistencia);

        initViews();
        setupRecyclerView();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarHistorial();
    }

    private void initViews() {
        progressHistorial = findViewById(R.id.progress_historial);
        rvHistorial = findViewById(R.id.rv_historial);
        layoutEmpty = findViewById(R.id.layout_empty);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        historialAdapter = new HistorialAdapter(this, this::mostrarDetalleDia, this::confirmarEliminarRegistroLocal);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(historialAdapter);
    }

    /**
     * Días hábiles desde el 1 de enero del año en curso hasta hoy (una petición API por fecha).
     * En el peor caso (fin de año) son ~261 días laborables; en mitad de año, menos.
     */
    private List<String> construirFechasLaborablesAnoEnCurso() {
        List<String> out = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.MONTH, Calendar.JANUARY);
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        inicio.set(Calendar.HOUR_OF_DAY, 0);
        inicio.set(Calendar.MINUTE, 0);
        inicio.set(Calendar.SECOND, 0);
        inicio.set(Calendar.MILLISECOND, 0);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        while (!c.before(inicio)) {
            int dow = c.get(Calendar.DAY_OF_WEEK);
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                out.add(sdf.format(c.getTime()));
            }
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return out;
    }

    private void cargarHistorial() {
        progressHistorial.setVisibility(View.VISIBLE);
        rvHistorial.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        new Thread(() -> {
            List<String> fechas = construirFechasLaborablesAnoEnCurso();
            // Asegurarnos de que "hoy" esté en la lista para que el registro local se vea
            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
            if (!fechas.contains(hoy)) {
                fechas.add(0, hoy);
            }
            ExecutorService pool = Executors.newFixedThreadPool(8);
            CountDownLatch latch = new CountDownLatch(fechas.size());
            List<AsistenciaDiaResumen> acumulado = Collections.synchronizedList(new ArrayList<>());

            for (String fecha : fechas) {
                pool.execute(() -> {
                    try {
                        Response<ApiResponse<List<AsistenciaAlumno>>> response =
                            RetrofitClient.getApiService().getAsistenciaPorFecha(fecha).execute();
                        if (response.isSuccessful()
                            && response.body() != null
                            && response.body().isSuccess()) {
                            List<AsistenciaAlumno> data = response.body().getData();
                            if (data != null && !data.isEmpty()) {
                                acumulado.add(new AsistenciaDiaResumen(fecha, new ArrayList<>(data)));
                            }
                        }
                    } catch (IOException ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(240, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pool.shutdown();

            List<AsistenciaDiaResumen> ordenados = new ArrayList<>(acumulado);
            
            // Ya no usamos caché local, todo viene del backend
            ordenados.sort((a, b) -> b.fechaIso.compareTo(a.fechaIso));
            
            List<Object> filas = construirFilasAgrupadas(ordenados);

            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                progressHistorial.setVisibility(View.GONE);
                if (filas.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvHistorial.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvHistorial.setVisibility(View.VISIBLE);
                    historialAdapter.setItems(filas);
                }
            });
        }).start();
    }

    private List<Object> construirFilasAgrupadas(List<AsistenciaDiaResumen> ordenadosDesc) {
        List<Object> filas = new ArrayList<>();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat mesTitulo = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
        String ultimoMes = null;

        for (AsistenciaDiaResumen r : ordenadosDesc) {
            try {
                Date d = iso.parse(r.fechaIso);
                String claveMes = mesTitulo.format(d);
                if (!claveMes.isEmpty()) {
                    claveMes = Character.toUpperCase(claveMes.charAt(0)) + claveMes.substring(1);
                }
                if (!claveMes.equals(ultimoMes)) {
                    filas.add(claveMes);
                    ultimoMes = claveMes;
                }
            } catch (ParseException ignored) {
            }
            filas.add(r);
        }
        return filas;
    }

    private void mostrarDetalleDia(AsistenciaDiaResumen resumen) {
        DetalleAsistenciaActivity.dataTransfer = resumen.alumnos;
        DetalleAsistenciaActivity.dateTransfer = resumen.fechaIso;
        Intent intent = new Intent(this, DetalleAsistenciaActivity.class);
        startActivity(intent);
    }

    private void confirmarEliminarRegistroLocal(AsistenciaDiaResumen resumen) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar asistencias")
                .setMessage("¿Eliminar TODAS las asistencias del " + formatTituloLargo(resumen.fechaIso) + "? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .setPositiveButton("Eliminar", (d, w) -> {
                    eliminarAsistenciasDelBackend(resumen);
                    d.dismiss();
                })
                .show();
    }

    private void eliminarAsistenciasDelBackend(AsistenciaDiaResumen resumen) {
        // Mostrar progreso
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Eliminando asistencias...");
        progress.setCancelable(false);
        progress.show();

        // Recolectar todos los IDs válidos
        List<String> idsParaEliminar = new ArrayList<>();
        for (AsistenciaAlumno alumno : resumen.alumnos) {
            if (alumno.getId() != null && !alumno.getId().isEmpty() && !alumno.getId().startsWith("local-")) {
                idsParaEliminar.add(alumno.getId());
            }
        }

        if (idsParaEliminar.isEmpty()) {
            progress.dismiss();
            Toast.makeText(this, "No hay asistencias válidas para eliminar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparar body con array de IDs
        org.json.JSONObject jsonBody = new org.json.JSONObject();
        try {
            org.json.JSONArray idsArray = new org.json.JSONArray();
            for (String id : idsParaEliminar) {
                idsArray.put(id);
            }
            jsonBody.put("ids", idsArray);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonBody.toString(),
                    okhttp3.MediaType.parse("application/json")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(com.example.ieperuanosuizoapp.api.ApiConfig.BASE_URL + "asistencia/eliminar-batch")
                    .post(body)
                    .build();

            // Crear cliente con timeout adecuado
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // UNA SOLA petición para eliminar todas las asistencias
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(GestionAsistenciaActivity.this, 
                            "Error al eliminar: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        
                        runOnUiThread(() -> {
                            progress.dismiss();
                            
                            if (response.isSuccessful()) {
                                Toast.makeText(GestionAsistenciaActivity.this, 
                                    "Asistencias eliminadas exitosamente", 
                                    Toast.LENGTH_SHORT).show();
                                cargarHistorial();
                            } else {
                                Toast.makeText(GestionAsistenciaActivity.this, 
                                    "Error al eliminar: " + response.code(), 
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(GestionAsistenciaActivity.this, 
                                "Error procesando respuesta", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (org.json.JSONException e) {
            progress.dismiss();
            Toast.makeText(this, "Error al preparar datos", Toast.LENGTH_SHORT).show();
        }
    }


    private String formatTituloLargo(String fechaIso) {
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

    private String diaDelMes(String fechaIso) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = iso.parse(fechaIso);
            return new SimpleDateFormat("d", Locale.getDefault()).format(d);
        } catch (ParseException e) {
            return "";
        }
    }

    private static String estadoCsvParaAlumno(AsistenciaAlumno a) {
        boolean tieneHora = a.getHora_registro() != null && !a.getHora_registro().isEmpty();
        if (!tieneHora) {
            return "Ausente";
        }
        String e = a.getEstado_entrada() != null ? a.getEstado_entrada().toLowerCase(Locale.ROOT) : "";
        if (e.contains("tardanza")) {
            return "Tardanza";
        }
        return "Presente";
    }

    private void descargarCsv(AsistenciaDiaResumen resumen) {
        if (resumen.alumnos.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Usamos un nombre de archivo seguro
            String fileName = "asistencia_" + resumen.fechaIso.replace("-", "_") + ".csv";
            File file = new File(getCacheDir(), fileName);
            
            StringBuilder csvContent = new StringBuilder("ID,Nombre Completo,Salon,Hora de Registro,Estado\n");
            for (AsistenciaAlumno a : resumen.alumnos) {
                String estado = estadoCsvParaAlumno(a);
                String nombre = a.getNombre_completo() != null ? a.getNombre_completo() : "";
                String salon = a.getSalon() != null ? a.getSalon() : "";
                String hora = a.getHora_registro() != null ? a.getHora_registro() : "";

                csvContent.append(a.getId()).append(",")
                    .append("\"").append(nombre.replace("\"", "\"\"")).append("\",")
                    .append("\"").append(salon.replace("\"", "\"\"")).append("\",")
                    .append("\"").append(hora.replace("\"", "\"\"")).append("\",")
                    .append("\"").append(estado).append("\"\n");
            }

            FileWriter writer = new FileWriter(file);
            writer.write(csvContent.toString());
            writer.flush();
            writer.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Asistencia " + resumen.fechaIso);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Compartir CSV"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
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

    private static class AsistenciaDiaResumen {
        final String fechaIso;
        final List<AsistenciaAlumno> alumnos;
        final boolean desdeLocal;
        final int presentesAtiempo;
        final int tardanzas;
        final int ausentes;

        AsistenciaDiaResumen(String fechaIso, List<AsistenciaAlumno> alumnos) {
            this(fechaIso, alumnos, false);
        }

        AsistenciaDiaResumen(String fechaIso, List<AsistenciaAlumno> alumnos, boolean desdeLocal) {
            this.fechaIso = fechaIso;
            this.alumnos = alumnos;
            this.desdeLocal = desdeLocal;
            int at = 0;
            int tard = 0;
            int aus = 0;
            for (AsistenciaAlumno a : alumnos) {
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
            this.presentesAtiempo = at;
            this.tardanzas = tard;
            this.ausentes = aus;
        }
    }

    private interface OnDiaClickListener {
        void onDiaClick(AsistenciaDiaResumen resumen);
    }

    private interface OnEliminarDiaListener {
        void onEliminarDia(AsistenciaDiaResumen resumen);
    }

    private static class HistorialAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_MES = 0;
        private static final int VIEW_TYPE_DIA = 1;

        private final GestionAsistenciaActivity activity;
        private final OnDiaClickListener listener;
        private final OnEliminarDiaListener eliminarListener;
        private final List<Object> items = new ArrayList<>();

        HistorialAdapter(GestionAsistenciaActivity activity, OnDiaClickListener listener, OnEliminarDiaListener eliminarListener) {
            this.activity = activity;
            this.listener = listener;
            this.eliminarListener = eliminarListener;
        }

        void setItems(List<Object> nuevos) {
            items.clear();
            items.addAll(nuevos);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? VIEW_TYPE_MES : VIEW_TYPE_DIA;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_MES) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asistencia_mes_header, parent, false);
                return new MesViewHolder(v);
            }
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asistencia_dia_card, parent, false);
            return new DiaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MesViewHolder) {
                ((MesViewHolder) holder).bind((String) items.get(position));
            } else if (holder instanceof DiaViewHolder) {
                AsistenciaDiaResumen r = (AsistenciaDiaResumen) items.get(position);
                ((DiaViewHolder) holder).bind(r, listener, eliminarListener, activity);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class MesViewHolder extends RecyclerView.ViewHolder {
            final TextView tv;

            MesViewHolder(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv_mes_header);
            }

            void bind(String mes) {
                tv.setText(mes);
            }
        }

        static class DiaViewHolder extends RecyclerView.ViewHolder {
            final TextView tvDiaGrande;
            final TextView tvTitulo;
            final TextView tvResumen;
            final CardView card;
            final ImageButton btnEliminar;

            DiaViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDiaGrande = itemView.findViewById(R.id.tv_dia_grande);
                tvTitulo = itemView.findViewById(R.id.tv_titulo_dia);
                tvResumen = itemView.findViewById(R.id.tv_resumen_dia);
                card = itemView.findViewById(R.id.card_dia);
                btnEliminar = itemView.findViewById(R.id.btn_eliminar_registro_dia);
            }

            void bind(AsistenciaDiaResumen r, OnDiaClickListener listener, OnEliminarDiaListener eliminarListener, GestionAsistenciaActivity act) {
                tvDiaGrande.setText(act.diaDelMes(r.fechaIso));
                tvTitulo.setText("Asistencia del " + act.formatTituloLargo(r.fechaIso));
                tvResumen.setText(
                    r.presentesAtiempo + " a tiempo · " + r.tardanzas + " tardanza · " + r.ausentes + " ausentes");
                // SIEMPRE mostrar botón eliminar
                btnEliminar.setVisibility(View.VISIBLE);
                btnEliminar.setOnClickListener(v -> eliminarListener.onEliminarDia(r));
                card.setOnClickListener(v -> listener.onDiaClick(r));
            }
        }
    }

    private static class AlumnosAsistenciaAdapter extends RecyclerView.Adapter<AlumnosAsistenciaAdapter.ViewHolder> {
        private final List<AsistenciaAlumno> lista;

        AlumnosAsistenciaAdapter(List<AsistenciaAlumno> lista) {
            this.lista = lista;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alumno_asistencia, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AsistenciaAlumno a = lista.get(position);
            holder.tvNombre.setText(a.getNombre_completo());
            holder.tvSalon.setText(a.getSalon());
            String est = a.getEstado_entrada() != null ? a.getEstado_entrada().toLowerCase(Locale.ROOT) : "";
            boolean tieneHora = a.getHora_registro() != null && !a.getHora_registro().isEmpty();
            if (!tieneHora) {
                holder.tvEstado.setText("Ausente");
                holder.tvEstado.setTextColor(0xFFBA1924);
            } else if (est.contains("tardanza")) {
                holder.tvEstado.setText(a.getHora_registro());
                holder.tvEstado.setTextColor(0xFFFF9800);
            } else {
                holder.tvEstado.setText(a.getHora_registro());
                holder.tvEstado.setTextColor(0xFF27AE60);
            }
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvSalon, tvEstado;

            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tv_nombre_alumno);
                tvSalon = itemView.findViewById(R.id.tv_salon_alumno);
                tvEstado = itemView.findViewById(R.id.tv_estado_entrada);
            }
        }
    }
}
