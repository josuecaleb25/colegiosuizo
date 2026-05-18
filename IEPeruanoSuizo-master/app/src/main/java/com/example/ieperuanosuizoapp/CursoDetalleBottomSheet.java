package com.example.ieperuanosuizoapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.Calificacion;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CursoDetalleBottomSheet extends BottomSheetDialogFragment {

    private String nombreCurso;
    private String nombreProfesor;
    private String promedio;
    private String salon;
    private String cursoId; // Realmente es asignacion_id
    private String alumnoId;
    private String userMode;
    private boolean mostrarBotonRetroceso = false;
    private LinearLayout layoutEvaluaciones;

    public static CursoDetalleBottomSheet newInstance(String curso, String profesor, String promedio) {
        CursoDetalleBottomSheet fragment = new CursoDetalleBottomSheet();
        Bundle args = new Bundle();
        args.putString("curso", curso);
        args.putString("profesor", profesor);
        args.putString("promedio", promedio);
        // NOTA: Este método no tiene cursoId ni alumnoId, así que no cargará calificaciones reales
        // Se mantiene para compatibilidad, pero debería usarse el método con IDs
        fragment.setArguments(args);
        return fragment;
    }

    public static CursoDetalleBottomSheet newInstanceConIds(String curso, String profesor, String promedio, String cursoId, String alumnoId) {
        CursoDetalleBottomSheet fragment = new CursoDetalleBottomSheet();
        Bundle args = new Bundle();
        args.putString("curso", curso);
        args.putString("profesor", profesor);
        args.putString("promedio", promedio);
        args.putString("curso_id", cursoId);
        args.putString("alumno_id", alumnoId);
        fragment.setArguments(args);
        return fragment;
    }

    public static CursoDetalleBottomSheet newInstanceConRetroceso(String curso, String alumno, String promedio, String salon, String profesor) {
        CursoDetalleBottomSheet fragment = new CursoDetalleBottomSheet();
        Bundle args = new Bundle();
        args.putString("curso", curso);
        args.putString("profesor", alumno);
        args.putString("promedio", promedio);
        args.putString("salon", salon);
        args.putString("profesor_real", profesor);
        args.putBoolean("mostrar_retroceso", true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        if (getArguments() != null) {
            nombreCurso = getArguments().getString("curso");
            nombreProfesor = getArguments().getString("profesor");
            promedio = getArguments().getString("promedio");
            salon = getArguments().getString("salon");
            cursoId = getArguments().getString("curso_id");
            alumnoId = getArguments().getString("alumno_id");
            mostrarBotonRetroceso = getArguments().getBoolean("mostrar_retroceso", false);
        }
        
        // Obtener el modo de usuario y alumno_id si no se proporcionó
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userMode = prefs.getString("user_mode", "ALUMNO");
        
        // Si es alumno y no se proporcionó alumno_id, obtenerlo de SharedPreferences
        if ("ALUMNO".equals(userMode) && alumnoId == null) {
            alumnoId = prefs.getString("user_id", null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_curso_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvCurso = view.findViewById(R.id.tv_curso_nombre);
        TextView tvProfesor = view.findViewById(R.id.tv_curso_profesor);
        TextView tvPromedio = view.findViewById(R.id.tv_promedio_badge);
        Button btnAgregarEvaluacion = view.findViewById(R.id.btn_agregar_evaluacion);
        ImageView btnBack = view.findViewById(R.id.btn_back_to_list);
        layoutEvaluaciones = view.findViewById(R.id.layout_evaluaciones_container);

        tvCurso.setText(nombreCurso);
        
        // Si es profesor, mostrar "Alumno:", si es alumno mostrar "Profesor:"
        if ("PROFESOR".equals(userMode)) {
            tvProfesor.setText("Alumno: " + nombreProfesor);
        } else {
            tvProfesor.setText("Profesor: " + nombreProfesor);
        }
        
        tvPromedio.setText(promedio);

        // Cargar calificaciones reales si tenemos los IDs necesarios
        if (cursoId != null && alumnoId != null) {
            android.util.Log.d("CursoDetalleBottomSheet", "🔍 Cargando calificaciones para alumno: " + alumnoId + ", curso: " + cursoId);
            cargarCalificacionesReales();
        } else {
            android.util.Log.w("CursoDetalleBottomSheet", "⚠️ No se pueden cargar calificaciones: cursoId=" + cursoId + ", alumnoId=" + alumnoId);
        }

        // Configurar visibilidad y funcionalidad según el modo de usuario
        if ("PROFESOR".equals(userMode)) {
            btnAgregarEvaluacion.setVisibility(View.GONE); // Ocultar por ahora, las evaluaciones se manejan en la pestaña
        } else {
            // Modo alumno: ocultar botón de agregar
            btnAgregarEvaluacion.setVisibility(View.GONE);
        }
    }

    private void cargarCalificacionesReales() {
        android.util.Log.d("CursoDetalleBottomSheet", "════════════════════════════════════════");
        android.util.Log.d("CursoDetalleBottomSheet", "🔍 INICIANDO CARGA DE CALIFICACIONES");
        android.util.Log.d("CursoDetalleBottomSheet", "   alumnoId: " + alumnoId);
        android.util.Log.d("CursoDetalleBottomSheet", "   cursoId (asignacion_id): " + cursoId);
        android.util.Log.d("CursoDetalleBottomSheet", "   URL: /api/evaluaciones/calificaciones/alumno/" + alumnoId + "/curso/" + cursoId);
        android.util.Log.d("CursoDetalleBottomSheet", "════════════════════════════════════════");
        
        RetrofitClient.getApiService()
            .getCalificacionesAlumno(alumnoId, cursoId)
            .enqueue(new Callback<ApiResponse<List<Calificacion>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Calificacion>>> call, Response<ApiResponse<List<Calificacion>>> response) {
                    android.util.Log.d("CursoDetalleBottomSheet", "📡 RESPUESTA RECIBIDA");
                    android.util.Log.d("CursoDetalleBottomSheet", "   HTTP Code: " + response.code());
                    android.util.Log.d("CursoDetalleBottomSheet", "   isSuccessful: " + response.isSuccessful());
                    
                    if (response.isSuccessful() && response.body() != null) {
                        android.util.Log.d("CursoDetalleBottomSheet", "   body != null: true");
                        android.util.Log.d("CursoDetalleBottomSheet", "   body.isSuccess(): " + response.body().isSuccess());
                        
                        if (response.body().isSuccess()) {
                            List<Calificacion> calificaciones = response.body().getData();
                            android.util.Log.d("CursoDetalleBottomSheet", "✅ Calificaciones recibidas: " + (calificaciones != null ? calificaciones.size() : "NULL"));
                            
                            if (calificaciones != null && calificaciones.size() > 0) {
                                android.util.Log.d("CursoDetalleBottomSheet", "📋 Primera calificación:");
                                Calificacion primera = calificaciones.get(0);
                                android.util.Log.d("CursoDetalleBottomSheet", "   - Nombre: " + primera.getNombreEvaluacion());
                                android.util.Log.d("CursoDetalleBottomSheet", "   - Nota: " + primera.getCalificacion());
                            }
                            
                            mostrarCalificaciones(calificaciones);
                        } else {
                            android.util.Log.e("CursoDetalleBottomSheet", "❌ body.isSuccess() = false");
                            android.util.Log.e("CursoDetalleBottomSheet", "   Message: " + response.body().getMessage());
                        }
                    } else {
                        android.util.Log.e("CursoDetalleBottomSheet", "❌ Error al cargar calificaciones");
                        android.util.Log.e("CursoDetalleBottomSheet", "   response.body() = " + response.body());
                        if (response.errorBody() != null) {
                            try {
                                android.util.Log.e("CursoDetalleBottomSheet", "   errorBody: " + response.errorBody().string());
                            } catch (Exception e) {
                                android.util.Log.e("CursoDetalleBottomSheet", "   No se pudo leer errorBody");
                            }
                        }
                        Toast.makeText(getContext(), "Error al cargar calificaciones", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Calificacion>>> call, Throwable t) {
                    android.util.Log.e("CursoDetalleBottomSheet", "💥 ERROR DE CONEXIÓN");
                    android.util.Log.e("CursoDetalleBottomSheet", "   Mensaje: " + t.getMessage());
                    android.util.Log.e("CursoDetalleBottomSheet", "   Clase: " + t.getClass().getName());
                    t.printStackTrace();
                    Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void mostrarCalificaciones(List<Calificacion> calificaciones) {
        android.util.Log.d("CursoDetalleBottomSheet", "════════════════════════════════════════");
        android.util.Log.d("CursoDetalleBottomSheet", "📊 MOSTRANDO CALIFICACIONES EN UI");
        android.util.Log.d("CursoDetalleBottomSheet", "   layoutEvaluaciones != null: " + (layoutEvaluaciones != null));
        android.util.Log.d("CursoDetalleBottomSheet", "   calificaciones != null: " + (calificaciones != null));
        android.util.Log.d("CursoDetalleBottomSheet", "   calificaciones.size(): " + (calificaciones != null ? calificaciones.size() : "NULL"));
        
        if (layoutEvaluaciones == null) {
            android.util.Log.e("CursoDetalleBottomSheet", "❌ layoutEvaluaciones es NULL - no se encontró R.id.layout_evaluaciones_container");
            return;
        }
        
        if (calificaciones == null) {
            android.util.Log.e("CursoDetalleBottomSheet", "❌ calificaciones es NULL");
            return;
        }

        layoutEvaluaciones.removeAllViews();
        android.util.Log.d("CursoDetalleBottomSheet", "🧹 Views anteriores eliminados");

        if (calificaciones.size() == 0) {
            android.util.Log.w("CursoDetalleBottomSheet", "⚠️ No hay calificaciones para mostrar");
            TextView tvVacio = new TextView(getContext());
            tvVacio.setText("No hay evaluaciones disponibles");
            tvVacio.setTextColor(0xFF757575);
            tvVacio.setPadding(0, 32, 0, 32);
            layoutEvaluaciones.addView(tvVacio);
            return;
        }

        for (int i = 0; i < calificaciones.size(); i++) {
            Calificacion calif = calificaciones.get(i);
            android.util.Log.d("CursoDetalleBottomSheet", "➕ Agregando calificación " + (i + 1) + "/" + calificaciones.size());
            
            View badgeView = LayoutInflater.from(getContext()).inflate(R.layout.item_badge_calificacion, layoutEvaluaciones, false);
            TextView tvNombre = badgeView.findViewById(R.id.tv_badge_nombre);
            TextView tvNota = badgeView.findViewById(R.id.tv_badge_nota);

            String nombreEval = calif.getNombreEvaluacion();
            tvNombre.setText(nombreEval != null ? nombreEval : "Sin nombre");
            
            Double nota = calif.getCalificacion();
            if (nota != null) {
                tvNota.setText(String.format("%.0f", nota));
            } else {
                tvNota.setText("--");
            }

            android.util.Log.d("CursoDetalleBottomSheet", "   ✓ " + nombreEval + " = " + (nota != null ? nota : "sin nota"));

            layoutEvaluaciones.addView(badgeView);
        }
        
        android.util.Log.d("CursoDetalleBottomSheet", "✅ " + calificaciones.size() + " calificaciones agregadas al layout");
        android.util.Log.d("CursoDetalleBottomSheet", "════════════════════════════════════════");
    }
}
