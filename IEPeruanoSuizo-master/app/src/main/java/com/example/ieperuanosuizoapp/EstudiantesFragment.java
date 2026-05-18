package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.Estudiante;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudiantesFragment extends Fragment {

    private static final String ARG_CURSO_ID = "curso_id";
    private static final String ARG_SALON = "salon";

    private RecyclerView rvEstudiantes;
    private EstudiantesAdapter adapter;
    private TextView tvContador;
    private ImageButton btnList, btnGrid;
    private String cursoId;
    private String salon;

    public static EstudiantesFragment newInstance(String cursoId, String salon) {
        EstudiantesFragment fragment = new EstudiantesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURSO_ID, cursoId);
        args.putString(ARG_SALON, salon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cursoId = getArguments().getString(ARG_CURSO_ID);
            salon = getArguments().getString(ARG_SALON);
        }
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
        cargarAlumnosReales();

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

    private void cargarAlumnosReales() {
        if (getContext() == null) return;
        
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String nombreProfesor = prefs.getString("user_name", "Profesor").toUpperCase();

        // Obtener alumnos del backend
        RetrofitClient.getApiService().getAlumnosAsistencia(null).enqueue(new Callback<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>> call, Response<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno> alumnos = response.body().getData();
                    
                    List<Estudiante> list = new ArrayList<>();
                    // El profesor siempre aparece primero como "Usted"
                    list.add(new Estudiante("prof-1", nombreProfesor, null, true));
                    
                    // Agregar alumnos reales
                    if (alumnos != null) {
                        for (com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno alumno : alumnos) {
                            list.add(new Estudiante(
                                alumno.getId(),
                                alumno.getNombre_completo(),
                                null,
                                false
                            ));
                        }
                    }
                    
                    adapter.setEstudiantes(list);
                    tvContador.setText("Miembros del curso (" + list.size() + ")");
                } else {
                    Log.e("EstudiantesFragment", "Error al cargar alumnos: " + response.code());
                    Toast.makeText(getContext(), "Error al cargar estudiantes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno>>> call, Throwable t) {
                Log.e("EstudiantesFragment", "Error de red: " + t.getMessage());
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
