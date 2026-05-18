package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.Evaluacion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EvaluacionesFragment extends Fragment {

    private String asignacionId;
    private RecyclerView rvEvaluaciones;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private EvaluacionesAdapter adapter;

    public static EvaluacionesFragment newInstance(String asignacionId) {
        EvaluacionesFragment fragment = new EvaluacionesFragment();
        Bundle args = new Bundle();
        args.putString("asignacion_id", asignacionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            asignacionId = getArguments().getString("asignacion_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_evaluaciones_profesor, container, false);

        rvEvaluaciones = view.findViewById(R.id.rv_evaluaciones);
        progressBar = view.findViewById(R.id.progress_evaluaciones);
        layoutEmpty = view.findViewById(R.id.layout_empty_evaluaciones);
        
        rvEvaluaciones.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new EvaluacionesAdapter();
        rvEvaluaciones.setAdapter(adapter);

        view.findViewById(R.id.btn_add_evaluacion).setOnClickListener(v -> {
            mostrarDialogoAgregar();
        });

        cargarEvaluaciones();

        return view;
    }

    private void cargarEvaluaciones() {
        if (asignacionId == null) {
            Toast.makeText(getContext(), "Error: No se encontró la asignación", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarCargando(true);

        RetrofitClient.getApiService()
            .getEvaluacionesCurso(asignacionId)
            .enqueue(new Callback<ApiResponse<List<Evaluacion>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Evaluacion>>> call, Response<ApiResponse<List<Evaluacion>>> response) {
                    mostrarCargando(false);
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Evaluacion> evaluaciones = response.body().getData();
                        
                        if (evaluaciones != null && !evaluaciones.isEmpty()) {
                            adapter.setItems(evaluaciones);
                            rvEvaluaciones.setVisibility(View.VISIBLE);
                            layoutEmpty.setVisibility(View.GONE);
                        } else {
                            rvEvaluaciones.setVisibility(View.GONE);
                            layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al cargar evaluaciones", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Evaluacion>>> call, Throwable t) {
                    mostrarCargando(false);
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

    private void mostrarDialogoAgregar() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_evaluacion, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        android.widget.EditText etNombre = v.findViewById(R.id.et_nombre_evaluacion);

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_aceptar).setOnClickListener(view -> {
            String nombre = etNombre.getText().toString().trim();
            if (!nombre.isEmpty()) {
                crearEvaluacion(nombre);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Ingrese un nombre", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void crearEvaluacion(String nombre) {
        // Verificar que tenemos asignacionId
        if (asignacionId == null || asignacionId.isEmpty()) {
            android.util.Log.e("EvaluacionesFragment", "❌ asignacionId es null o vacío");
            Toast.makeText(getContext(), "Error: No se encontró el ID de la asignación", Toast.LENGTH_LONG).show();
            return;
        }

        // Obtener el siguiente orden
        int siguienteOrden = adapter.getItemCount() + 1;

        Evaluacion nuevaEvaluacion = new Evaluacion(asignacionId, nombre, 1.0, siguienteOrden);

        android.util.Log.d("EvaluacionesFragment", "📝 Creando evaluación: " + nombre + ", asignacionId: " + asignacionId + ", orden: " + siguienteOrden);

        RetrofitClient.getApiService()
            .crearEvaluacion(nuevaEvaluacion)
            .enqueue(new Callback<ApiResponse<Evaluacion>>() {
                @Override
                public void onResponse(Call<ApiResponse<Evaluacion>> call, Response<ApiResponse<Evaluacion>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(getContext(), "Evaluación creada exitosamente", Toast.LENGTH_SHORT).show();
                        cargarEvaluaciones(); // Recargar lista
                    } else {
                        String errorMsg = "Error al crear evaluación";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        android.util.Log.e("EvaluacionesFragment", "Error: " + errorMsg + ", Code: " + response.code());
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Evaluacion>> call, Throwable t) {
                    android.util.Log.e("EvaluacionesFragment", "Error de conexión: " + t.getMessage(), t);
                    Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void mostrarDialogoEditar(Evaluacion evaluacion) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_evaluacion, null);
        
        TextView tvTitle = v.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = v.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etNombre = v.findViewById(R.id.et_nombre_evaluacion);
        
        tvTitle.setText("Editar Evaluacion");
        tvSubtitle.setText("Nuevo nombre de " + evaluacion.getNombre());
        etNombre.setText(evaluacion.getNombre());

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_aceptar).setOnClickListener(view -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            if (!nuevoNombre.isEmpty()) {
                actualizarEvaluacion(evaluacion.getId(), nuevoNombre);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Ingrese un nombre", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void actualizarEvaluacion(String evaluacionId, String nuevoNombre) {
        // Usar Map para enviar solo el campo que queremos actualizar
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nuevoNombre);

        RetrofitClient.getApiService()
            .actualizarEvaluacion(evaluacionId, datos)
            .enqueue(new Callback<ApiResponse<Evaluacion>>() {
                @Override
                public void onResponse(Call<ApiResponse<Evaluacion>> call, Response<ApiResponse<Evaluacion>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(getContext(), "Evaluación actualizada", Toast.LENGTH_SHORT).show();
                        cargarEvaluaciones();
                    } else {
                        String errorMsg = "Error al actualizar";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        android.util.Log.e("EvaluacionesFragment", "Error actualizando: " + errorMsg);
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Evaluacion>> call, Throwable t) {
                    android.util.Log.e("EvaluacionesFragment", "Error de conexión al actualizar: " + t.getMessage(), t);
                    Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void mostrarDialogoEliminar(Evaluacion evaluacion) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_eliminar_evaluacion, null);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_aceptar).setOnClickListener(view -> {
            eliminarEvaluacion(evaluacion.getId());
            dialog.dismiss();
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void eliminarEvaluacion(String evaluacionId) {
        android.util.Log.d("EvaluacionesFragment", "Eliminando evaluación: " + evaluacionId);
        
        RetrofitClient.getApiService()
            .eliminarEvaluacion(evaluacionId)
            .enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    android.util.Log.d("EvaluacionesFragment", "Response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(getContext(), "Evaluación eliminada", Toast.LENGTH_SHORT).show();
                        cargarEvaluaciones();
                    } else {
                        String errorMsg = "Error al eliminar";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                        android.util.Log.e("EvaluacionesFragment", "Error eliminando: " + errorMsg);
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    android.util.Log.e("EvaluacionesFragment", "Error de conexión al eliminar: " + t.getMessage(), t);
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
        private List<Evaluacion> items = new ArrayList<>();

        void setItems(List<Evaluacion> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evaluacion_profesor, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Evaluacion evaluacion = items.get(position);
            holder.tvNombre.setText(evaluacion.getNombre());
            
            holder.btnEdit.setOnClickListener(v -> {
                mostrarDialogoEditar(evaluacion);
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                mostrarDialogoEliminar(evaluacion);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre;
            View btnEdit, btnDelete;
            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tv_nombre_eval);
                btnEdit = itemView.findViewById(R.id.btn_edit_eval);
                btnDelete = itemView.findViewById(R.id.btn_delete_eval);
            }
        }
    }
}
