package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ieperuanosuizoapp.api.models.Estudiante;
import java.util.ArrayList;
import java.util.List;

public class EstudiantesFragment extends Fragment {

    private RecyclerView rvEstudiantes;
    private EstudiantesAdapter adapter;
    private TextView tvContador;
    private ImageButton btnList, btnGrid;

    public static EstudiantesFragment newInstance() {
        return new EstudiantesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_estudiantes, container, false);

        rvEstudiantes = view.findViewById(R.id.rv_estudiantes);
        tvContador = view.findViewById(R.id.tv_contador_miembros);
        btnList = view.findViewById(R.id.btn_view_list);
        btnGrid = view.findViewById(R.id.btn_view_grid);

        adapter = new EstudiantesAdapter();
        adapter.setOnEstudianteClickListener(estudiante -> {
            if (getParentFragment() instanceof ContainerEstudiantesFragment) {
                ((ContainerEstudiantesFragment) getParentFragment()).navegarADetalle(estudiante.getNombre());
            }
        });

        rvEstudiantes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEstudiantes.setAdapter(adapter);

        // Estado inicial de los botones
        btnList.setColorFilter(android.graphics.Color.parseColor("#BA1924"));
        btnGrid.setColorFilter(android.graphics.Color.parseColor("#5E5F60"));

        setupViewToggles();
        cargarDatosPrueba();

        return view;
    }

    private void setupViewToggles() {
        btnList.setOnClickListener(v -> {
            adapter.setViewType(EstudiantesAdapter.VIEW_TYPE_LIST);
            rvEstudiantes.setLayoutManager(new LinearLayoutManager(getContext()));
            btnList.setColorFilter(android.graphics.Color.parseColor("#BA1924"));
            btnGrid.setColorFilter(android.graphics.Color.parseColor("#5E5F60"));
        });

        btnGrid.setOnClickListener(v -> {
            adapter.setViewType(EstudiantesAdapter.VIEW_TYPE_GRID);
            rvEstudiantes.setLayoutManager(new GridLayoutManager(getContext(), 2));
            btnGrid.setColorFilter(android.graphics.Color.parseColor("#BA1924"));
            btnList.setColorFilter(android.graphics.Color.parseColor("#5E5F60"));
        });
    }

    private void cargarDatosPrueba() {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String nombreProfesor = prefs.getString("user_name", "Profesor").toUpperCase();

        List<Estudiante> list = new ArrayList<>();
        // El profesor siempre aparece primero como "Usted"
        list.add(new Estudiante("prof-1", nombreProfesor, null, true));
        
        // Alumnos de prueba
        list.add(new Estudiante("2", "CARLOS ANDRES SANCHEZ VIL", null, false));
        list.add(new Estudiante("3", "VICTOR HUGO SOTO DIAZ", null, false));
        list.add(new Estudiante("4", "JUAN DIEGO PEREZ REYES", null, false));
        list.add(new Estudiante("5", "MARIA FERNANDA LOPEZ", null, false));
        list.add(new Estudiante("6", "RICARDO HUAMAN QUISPE", null, false));

        adapter.setEstudiantes(list);
        tvContador.setText("Miembros del curso (" + list.size() + ")");
    }
}
