package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.Calificacion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleCalificacionFragment extends Fragment {

    private String alumnoId;
    private String estudianteNombre;
    private String cursoId;
    private TextView tvPromedioTotal;
    private RecyclerView rvEvaluaciones;
    private EvaluacionesAdapter adapter;
    private View progressBar;

    public static DetalleCalificacionFragment newInstance(String alumnoId, String nombre, String cursoId) {
        DetalleCalificacionFragment fragment = new DetalleCalificacionFragment();
        Bundle args = new Bundle();
        args.putString("alumno_id", alumnoId);
        args.putString("nombre", nombre);
        args.putString("curso_id", cursoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            alumnoId = getArguments().getString("alumno_id");
            estudianteNombre = getArguments().getString("nombre");
            cursoId = getArguments().getString("curso_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detalle_calificacion_estudiante, container, false);

        TextView tvNombre = view.findViewById(R.id.tv_nombre_estudiante);
        tvNombre.setText(estudianteNombre);
        
        tvPromedioTotal = view.findViewById(R.id.tv_promedio_total);
        progressBar = view.findViewById(R.id.progress_calificaciones);

        // Lógica de navegación con el botón "Atrás"
        view.findViewById(R.id.btn_atras_detalle).setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        rvEvaluaciones = view.findViewById(R.id.rv_evaluaciones_estudiante);
        rvEvaluaciones.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new EvaluacionesAdapter();
        rvEvaluaciones.setAdapter(adapter);

        cargarCalificaciones();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar calificaciones cada vez que el fragmento se vuelve visible
        // Esto asegura que si se creó una nueva evaluación, aparezca aquí
        if (adapter != null) {
            cargarCalificaciones();
        }
    }

    public void recargarCalificaciones() {
        // Método público para recargar calificaciones desde fuera
        cargarCalificaciones();
    }

    private void cargarCalificaciones() {
        if (alumnoId == null || cursoId == null) {
            Toast.makeText(getContext(), "Error: Datos incompletos", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("DetalleCalificacion", "Cargando calificaciones para alumno: " + alumnoId + ", curso: " + cursoId);
        mostrarCargando(true);

        RetrofitClient.getApiService()
            .getCalificacionesAlumno(alumnoId, cursoId)
            .enqueue(new Callback<ApiResponse<List<Calificacion>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Calificacion>>> call, Response<ApiResponse<List<Calificacion>>> response) {
                    mostrarCargando(false);
                    
                    android.util.Log.d("DetalleCalificacion", "Response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Calificacion> calificaciones = response.body().getData();
                        
                        android.util.Log.d("DetalleCalificacion", "Calificaciones recibidas: " + (calificaciones != null ? calificaciones.size() : 0));
                        
                        if (calificaciones != null && !calificaciones.isEmpty()) {
                            for (Calificacion c : calificaciones) {
                                android.util.Log.d("DetalleCalificacion", "  - " + c.getNombreEvaluacion() + ": " + c.getCalificacion());
                            }
                            adapter.setCalificaciones(calificaciones);
                            calcularPromedio(calificaciones);
                        } else {
                            Toast.makeText(getContext(), "No hay calificaciones disponibles", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.util.Log.e("DetalleCalificacion", "Error en respuesta");
                        Toast.makeText(getContext(), "Error al cargar calificaciones", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Calificacion>>> call, Throwable t) {
                    mostrarCargando(false);
                    android.util.Log.e("DetalleCalificacion", "Error de conexión: " + t.getMessage(), t);
                    Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void mostrarCargando(boolean mostrar) {
        if (progressBar != null) {
            progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
        if (rvEvaluaciones != null) {
            rvEvaluaciones.setVisibility(mostrar ? View.GONE : View.VISIBLE);
        }
    }

    private void calcularPromedio(List<Calificacion> calificaciones) {
        double suma = 0;
        int contadas = 0;

        for (Calificacion cal : calificaciones) {
            if (cal.getCalificacion() != null) {
                suma += cal.getCalificacion();
                contadas++;
            }
        }

        if (contadas == 0) {
            tvPromedioTotal.setText("--");
        } else {
            double promedio = suma / contadas;
            tvPromedioTotal.setText(String.format(java.util.Locale.US, "%.1f", promedio));
        }
    }

    private void mostrarDialogoEditarNota(Calificacion calificacion, int position) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_nota, null);
        
        TextView tvSubtitle = v.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etNota = v.findViewById(R.id.et_nota);
        
        tvSubtitle.setText("Evaluación: " + calificacion.getNombreEvaluacion());
        
        if (calificacion.getCalificacion() != null) {
            etNota.setText(String.valueOf(calificacion.getCalificacion()));
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_guardar).setOnClickListener(view -> {
            String input = etNota.getText().toString().replace(",", ".").trim();
            
            Double nuevaNota = null;
            // Si el campo está vacío, enviar null (borrar calificación)
            if (!input.isEmpty()) {
                try {
                    double valor = Double.parseDouble(input);
                    if (valor < 0 || valor > 20) {
                        etNota.setError("Rango 0-20");
                        return;
                    }
                    nuevaNota = valor; // Aceptar cualquier valor entre 0 y 20, incluyendo 0.0
                } catch (NumberFormatException e) {
                    etNota.setError("Nota inválida");
                    return;
                }
            }

            actualizarCalificacion(calificacion.getId(), nuevaNota, position);
            dialog.dismiss();
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void actualizarCalificacion(String calificacionId, Double nuevaNota, int position) {
        android.util.Log.d("DetalleCalificacion", "════════════════════════════════════════");
        android.util.Log.d("DetalleCalificacion", "📝 ACTUALIZANDO CALIFICACIÓN");
        android.util.Log.d("DetalleCalificacion", "   ID: " + calificacionId);
        android.util.Log.d("DetalleCalificacion", "   Nota: " + nuevaNota);
        android.util.Log.d("DetalleCalificacion", "   Nota es null: " + (nuevaNota == null));
        
        Map<String, Object> datos = new HashMap<>();
        datos.put("calificacion", nuevaNota);
        
        android.util.Log.d("DetalleCalificacion", "   Map datos: " + datos.toString());
        android.util.Log.d("DetalleCalificacion", "════════════════════════════════════════");

        RetrofitClient.getApiService()
            .actualizarCalificacion(calificacionId, datos)
            .enqueue(new Callback<ApiResponse<Calificacion>>() {
                @Override
                public void onResponse(Call<ApiResponse<Calificacion>> call, Response<ApiResponse<Calificacion>> response) {
                    android.util.Log.d("DetalleCalificacion", "Response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(getContext(), "Calificación actualizada", Toast.LENGTH_SHORT).show();
                        cargarCalificaciones(); // Recargar para actualizar el promedio
                    } else {
                        String errorMsg = "Error al actualizar";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        android.util.Log.e("DetalleCalificacion", "Error: " + errorMsg);
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Calificacion>> call, Throwable t) {
                    android.util.Log.e("DetalleCalificacion", "Error de conexión: " + t.getMessage(), t);
                    Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void configurarAnchoDialogo(android.app.AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private class EvaluacionesAdapter extends RecyclerView.Adapter<EvaluacionesAdapter.ViewHolder> {
        private List<Calificacion> calificaciones = new ArrayList<>();

        void setCalificaciones(List<Calificacion> calificaciones) {
            this.calificaciones = calificaciones;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calificacion_fila, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Calificacion calificacion = calificaciones.get(position);
            
            holder.tvNombre.setText(calificacion.getNombreEvaluacion());
            
            if (calificacion.getCalificacion() != null) {
                holder.tvNota.setText(String.format(java.util.Locale.US, "%.1f", calificacion.getCalificacion()));
            } else {
                holder.tvNota.setText("--");
            }

            holder.itemView.setOnClickListener(v -> {
                mostrarDialogoEditarNota(calificacion, position);
            });
        }

        @Override
        public int getItemCount() {
            return calificaciones.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvNota;
            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tv_nombre_evaluacion);
                tvNota = itemView.findViewById(R.id.tv_nota);
            }
        }
    }
}
