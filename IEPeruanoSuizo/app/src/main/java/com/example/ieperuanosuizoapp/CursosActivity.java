package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.ieperuanosuizoapp.api.ApiClient;
import com.example.ieperuanosuizoapp.api.ApiResponse;
import com.example.ieperuanosuizoapp.models.Curso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CursosActivity extends AppCompatActivity {

    private RecyclerView rvCursos;
    private CursosAdapter adapter;
    private List<Curso> listaCursos = new ArrayList<>();
    private View layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursos);

        // Verificar autenticación
        if (!ApiClient.isLoggedIn(this)) {
            Intent intent = new Intent(this, AuthLogin.class);
            startActivity(intent);
            finish();
            return;
        }

        // Inicializar vistas
        rvCursos = findViewById(R.id.rv_cursos);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        
        rvCursos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CursosAdapter(listaCursos);
        rvCursos.setAdapter(adapter);

        // Botón de retroceso
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Cargar cursos
        loadCursos();

        setupBottomNavigation();
    }

    private void loadCursos() {
        String token = ApiClient.getAuthToken(this);
        ApiClient.getApiService().getCursos(token).enqueue(new Callback<ApiResponse<List<Curso>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Curso>>> call, Response<ApiResponse<List<Curso>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    listaCursos = response.body().getData();
                    adapter.updateList(listaCursos);
                    
                    if (listaCursos.isEmpty()) {
                        rvCursos.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvCursos.setVisibility(View.VISIBLE);
                        layoutEmptyState.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Curso>>> call, Throwable t) {
                Toast.makeText(CursosActivity.this, "Error al cargar cursos", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }
    
    private void showEmptyState() {
        rvCursos.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Definir colores: Rojo para seleccionado, Gris para normal
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                Color.parseColor("#BA1924"),
                Color.parseColor("#5E5F60")
        };
        ColorStateList navTint = new ColorStateList(states, colors);

        bottomNav.setItemIconTintList(navTint);
        
        // Marcar "Cursos" (nav_homework) como seleccionado
        bottomNav.setSelectedItemId(R.id.nav_homework);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish(); // Volver a la Home
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_homework) {
                return true; // Ya estamos aquí
            }
            return false;
        });
    }
    
    // Adapter para la lista de cursos
    class CursosAdapter extends RecyclerView.Adapter<CursosAdapter.ViewHolder> {
        private List<Curso> cursos;
        
        CursosAdapter(List<Curso> cursos) { 
            this.cursos = cursos; 
        }
        
        void updateList(List<Curso> newList) { 
            this.cursos = newList; 
            notifyDataSetChanged(); 
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curso, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Curso curso = cursos.get(position);
            holder.tvNombreCurso.setText(curso.getNombre());
            holder.tvCodigoCurso.setText(curso.getCodigo());
            holder.tvGradoCurso.setText(curso.getGrado_nombre());
            holder.tvProfesorCurso.setText(curso.getProfesor_nombre());
        }

        @Override
        public int getItemCount() { 
            return cursos.size(); 
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombreCurso, tvCodigoCurso, tvGradoCurso, tvProfesorCurso;
            
            ViewHolder(View v) {
                super(v);
                tvNombreCurso = v.findViewById(R.id.tv_nombre_curso);
                tvCodigoCurso = v.findViewById(R.id.tv_codigo_curso);
                tvGradoCurso = v.findViewById(R.id.tv_grado_curso);
                tvProfesorCurso = v.findViewById(R.id.tv_profesor_curso);
            }
        }
    }
}