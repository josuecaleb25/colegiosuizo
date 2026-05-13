package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ListaAlumnosBottomSheet extends BottomSheetDialogFragment {

    private String nombreCurso;
    private String nombreProfesor;
    private String salon;

    public static ListaAlumnosBottomSheet newInstance(String curso, String profesor, String salon) {
        ListaAlumnosBottomSheet fragment = new ListaAlumnosBottomSheet();
        Bundle args = new Bundle();
        args.putString("curso", curso);
        args.putString("profesor", profesor);
        args.putString("salon", salon);
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
            salon = getArguments().getString("salon");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_lista_alumnos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvCurso = view.findViewById(R.id.tv_curso_nombre);
        TextView tvSalon = view.findViewById(R.id.tv_curso_salon);
        TextView tvProfesor = view.findViewById(R.id.tv_curso_profesor);
        LinearLayout containerAlumnos = view.findViewById(R.id.container_alumnos);
        androidx.cardview.widget.CardView btnVerEvaluaciones = view.findViewById(R.id.btn_ver_evaluaciones);

        tvCurso.setText(nombreCurso);
        tvSalon.setText("Salón: " + salon);
        tvProfesor.setText("Profesor: " + nombreProfesor);

        // Configurar botón de ver evaluaciones
        btnVerEvaluaciones.setOnClickListener(v -> {
            // Abrir Activity de Gestión de Evaluaciones
            android.content.Intent intent = new android.content.Intent(requireContext(), GestionEvaluacionesActivity.class);
            intent.putExtra("curso", nombreCurso);
            intent.putExtra("profesor", nombreProfesor);
            intent.putExtra("salon", salon);
            startActivity(intent);
        });

        // Alumnos simulados
        String[][] alumnos = {
                {"Juan Pérez García", "2024001", "17"},
                {"María González López", "2024002", "16"},
                {"Carlos Rodríguez Sánchez", "2024003", "15"},
                {"Ana Martínez Fernández", "2024004", "18"},
                {"Luis Torres Ramírez", "2024005", "14"},
                {"Carmen Flores Díaz", "2024006", "16"},
                {"Pedro Sánchez Morales", "2024007", "15"},
                {"Laura Jiménez Castro", "2024008", "17"}
        };

        for (String[] alumno : alumnos) {
            View alumnoItem = LayoutInflater.from(requireContext()).inflate(R.layout.item_alumno, containerAlumnos, false);
            
            TextView tvNombre = alumnoItem.findViewById(R.id.tv_alumno_nombre);
            TextView tvId = alumnoItem.findViewById(R.id.tv_alumno_id);
            TextView tvPromedio = alumnoItem.findViewById(R.id.tv_alumno_promedio);

            tvNombre.setText(alumno[0]);
            tvId.setText("ID: " + alumno[1]);
            tvPromedio.setText(alumno[2] + "/20");

            final String nombreAlumno = alumno[0];
            final String promedio = alumno[2];

            alumnoItem.setOnClickListener(v -> {
                dismiss(); // Cerrar este bottom sheet
                // Abrir el bottom sheet de notas del alumno CON botón de retroceso
                CursoDetalleBottomSheet bottomSheet = CursoDetalleBottomSheet.newInstanceConRetroceso(
                        nombreCurso,
                        nombreAlumno,
                        promedio,
                        salon,
                        nombreProfesor
                );
                bottomSheet.show(getParentFragmentManager(), "AlumnoDetalle");
            });

            containerAlumnos.addView(alumnoItem);
        }
    }
}
