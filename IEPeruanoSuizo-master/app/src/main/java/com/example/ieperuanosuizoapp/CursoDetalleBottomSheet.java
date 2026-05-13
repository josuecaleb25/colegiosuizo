package com.example.ieperuanosuizoapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CursoDetalleBottomSheet extends BottomSheetDialogFragment {

    private String nombreCurso;
    private String nombreProfesor;
    private String promedio;
    private String salon;
    private String userMode;
    private boolean mostrarBotonRetroceso = false;

    public static CursoDetalleBottomSheet newInstance(String curso, String profesor, String promedio) {
        CursoDetalleBottomSheet fragment = new CursoDetalleBottomSheet();
        Bundle args = new Bundle();
        args.putString("curso", curso);
        args.putString("profesor", profesor);
        args.putString("promedio", promedio);
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
            mostrarBotonRetroceso = getArguments().getBoolean("mostrar_retroceso", false);
        }
        
        // Obtener el modo de usuario
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userMode = prefs.getString("user_mode", "ALUMNO");
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
        TextView tvVerMas = view.findViewById(R.id.tv_ver_mas);
        View layoutAdicionales = view.findViewById(R.id.layout_elementos_adicionales);
        Button btnAgregarEvaluacion = view.findViewById(R.id.btn_agregar_evaluacion);
        ImageView btnBack = view.findViewById(R.id.btn_back_to_list);

        tvCurso.setText(nombreCurso);
        
        // Si es profesor, mostrar "Alumno:", si es alumno mostrar "Profesor:"
        if ("PROFESOR".equals(userMode)) {
            tvProfesor.setText("Alumno: " + nombreProfesor);
        } else {
            tvProfesor.setText("Profesor: " + nombreProfesor);
        }
        
        tvPromedio.setText(promedio);

        // Mostrar botón de retroceso si viene desde lista de alumnos
        if (mostrarBotonRetroceso) {
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setOnClickListener(v -> {
                dismiss();
                // Volver a mostrar la lista de alumnos
                String profesorReal = getArguments().getString("profesor_real", "Ricardo Huaman");
                ListaAlumnosBottomSheet listaBottomSheet = ListaAlumnosBottomSheet.newInstance(
                        nombreCurso,
                        profesorReal,
                        salon
                );
                listaBottomSheet.show(getParentFragmentManager(), "ListaAlumnos");
            });
        }

        // Configurar visibilidad y funcionalidad según el modo de usuario
        // IMPORTANTE: Solo mostrar "Agregar Evaluación" si NO viene desde lista de alumnos
        if ("PROFESOR".equals(userMode) && !mostrarBotonRetroceso) {
            // Mostrar botón de agregar evaluación solo en vista general
            btnAgregarEvaluacion.setVisibility(View.VISIBLE);
            btnAgregarEvaluacion.setOnClickListener(v -> mostrarDialogoAgregarEvaluacion());
            
            // Hacer los badges clickeables para editar
            configurarBadgeEditable(view, R.id.badge_actitudes, "Actitudes");
            configurarBadgeEditable(view, R.id.badge_participacion, "Participacion");
            configurarBadgeEditable(view, R.id.badge_examen1, "Examen lore");
            configurarBadgeEditable(view, R.id.badge_examen2, "Examen lore");
            configurarBadgeEditable(view, R.id.badge_trabajo_grupal, "Trabajo grupal");
            configurarBadgeEditable(view, R.id.badge_proyecto_final, "Proyecto final");
            configurarBadgeEditable(view, R.id.badge_exposicion, "Exposición");
        } else if ("PROFESOR".equals(userMode) && mostrarBotonRetroceso) {
            // Cuando viene desde lista de alumnos, solo permitir editar badges
            btnAgregarEvaluacion.setVisibility(View.GONE);
            
            configurarBadgeEditable(view, R.id.badge_actitudes, "Actitudes");
            configurarBadgeEditable(view, R.id.badge_participacion, "Participacion");
            configurarBadgeEditable(view, R.id.badge_examen1, "Examen lore");
            configurarBadgeEditable(view, R.id.badge_examen2, "Examen lore");
            configurarBadgeEditable(view, R.id.badge_trabajo_grupal, "Trabajo grupal");
            configurarBadgeEditable(view, R.id.badge_proyecto_final, "Proyecto final");
            configurarBadgeEditable(view, R.id.badge_exposicion, "Exposición");
        } else {
            // Modo alumno: ocultar botón de agregar
            btnAgregarEvaluacion.setVisibility(View.GONE);
        }

        // Funcionalidad "Ver mas"
        tvVerMas.setOnClickListener(v -> {
            if (layoutAdicionales.getVisibility() == View.GONE) {
                // Expandir
                layoutAdicionales.setVisibility(View.VISIBLE);
                tvVerMas.setText("Ver menos");
                expandBottomSheet();
            } else {
                // Colapsar
                layoutAdicionales.setVisibility(View.GONE);
                tvVerMas.setText("Ver mas");
            }
        });
    }

    private void configurarBadgeEditable(View parentView, int badgeId, String nombreElemento) {
        TextView badge = parentView.findViewById(badgeId);
        if (badge != null) {
            badge.setClickable(true);
            badge.setFocusable(true);
            badge.setOnClickListener(v -> mostrarDialogoEditarCalificacion(badge, nombreElemento));
        }
    }

    private void mostrarDialogoEditarCalificacion(TextView badge, String nombreElemento) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar Calificación");
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_calificacion, null);
        EditText etCalificacion = dialogView.findViewById(R.id.et_calificacion);
        TextView tvNombreElemento = dialogView.findViewById(R.id.tv_nombre_elemento);
        
        tvNombreElemento.setText(nombreElemento);
        
        // Si ya tiene una calificación, mostrarla
        String calificacionActual = badge.getText().toString();
        if (!calificacionActual.equals("- -")) {
            etCalificacion.setText(calificacionActual);
        }
        
        builder.setView(dialogView);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevaCalificacion = etCalificacion.getText().toString().trim();
            if (!nuevaCalificacion.isEmpty()) {
                try {
                    int nota = Integer.parseInt(nuevaCalificacion);
                    if (nota >= 0 && nota <= 20) {
                        badge.setText(nuevaCalificacion);
                        Toast.makeText(requireContext(), "Calificación actualizada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "La nota debe estar entre 0 y 20", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Ingrese un número válido", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoAgregarEvaluacion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Agregar Evaluación");
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_evaluacion, null);
        EditText etNombreEvaluacion = dialogView.findViewById(R.id.et_nombre_evaluacion);
        EditText etCalificacion = dialogView.findViewById(R.id.et_calificacion_nueva);
        
        builder.setView(dialogView);
        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombreEvaluacion = etNombreEvaluacion.getText().toString().trim();
            String calificacion = etCalificacion.getText().toString().trim();
            
            if (nombreEvaluacion.isEmpty()) {
                Toast.makeText(requireContext(), "Ingrese el nombre de la evaluación", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!calificacion.isEmpty()) {
                try {
                    int nota = Integer.parseInt(calificacion);
                    if (nota < 0 || nota > 20) {
                        Toast.makeText(requireContext(), "La nota debe estar entre 0 y 20", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Ingrese un número válido", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Agregar la nueva evaluación a la lista (simulado)
            Toast.makeText(requireContext(), "Evaluación '" + nombreEvaluacion + "' agregada", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void expandBottomSheet() {
        if (getDialog() != null) {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                (com.google.android.material.bottomsheet.BottomSheetDialog) getDialog();
            android.widget.FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
            
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = 
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
}
