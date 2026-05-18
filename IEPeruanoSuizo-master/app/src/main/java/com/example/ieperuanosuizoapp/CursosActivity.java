package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class CursosActivity extends AppCompatActivity {

    private String userMode;
    private String userId;
    private LinearLayout cursosContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursos);

        // Obtener el modo de usuario
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = prefs.getString("user_mode", "ALUMNO");
        userId = prefs.getString("user_id", null);

        // Botón de retroceso
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Obtener contenedor de cursos
        cursosContainer = findViewById(R.id.cursos_container);
        
        cargarCursosDesdeBackend();
        setupBottomNavigation();
    }

    private void cargarCursosDesdeBackend() {
        if (userId == null) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            android.util.Log.e("CursosActivity", "userId es null");
            return;
        }

        android.util.Log.d("CursosActivity", "Cargando cursos para userId: " + userId + ", modo: " + userMode);

        retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call;
        
        if ("PROFESOR".equals(userMode)) {
            call = RetrofitClient.getApiService().getCursosProfesor(userId);
            android.util.Log.d("CursosActivity", "Llamando a getCursosProfesor");
        } else {
            call = RetrofitClient.getApiService().getCursosAlumno(userId);
            android.util.Log.d("CursosActivity", "Llamando a getCursosAlumno");
        }

        call.enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call,
                                 retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                android.util.Log.d("CursosActivity", "Response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("CursosActivity", "Response body: " + new com.google.gson.Gson().toJson(response.body()));
                    
                    if (response.body().isSuccess()) {
                        List<Object> cursosData = response.body().getData();
                        android.util.Log.d("CursosActivity", "Cursos encontrados: " + (cursosData != null ? cursosData.size() : 0));
                        mostrarCursos(cursosData);
                    } else {
                        String mensaje = response.body().getMessage();
                        android.util.Log.e("CursosActivity", "Response no exitoso: " + mensaje);
                        Toast.makeText(CursosActivity.this, "Error: " + mensaje, Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Leer el error del servidor
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                            android.util.Log.e("CursosActivity", "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CursosActivity", "Error leyendo error body", e);
                    }
                    
                    android.util.Log.e("CursosActivity", "Response no exitoso. Code: " + response.code());
                    Toast.makeText(CursosActivity.this, "Error del servidor (código " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                android.util.Log.e("CursosActivity", "Error al cargar cursos: " + t.getMessage(), t);
                Toast.makeText(CursosActivity.this, "Error al cargar cursos: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarCursos(List<Object> cursosData) {
        cursosContainer.removeAllViews();
        
        if (cursosData == null || cursosData.isEmpty()) {
            Toast.makeText(this, "No se encontraron cursos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        
        for (Object obj : cursosData) {
            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
            
            String nombre = jsonObj.has("nombre") ? jsonObj.get("nombre").getAsString() : "";
            String profesor = jsonObj.has("profesor") ? jsonObj.get("profesor").getAsString() : "";
            String promedio = jsonObj.has("promedio") ? jsonObj.get("promedio").getAsString() : "0";
            String seccion = jsonObj.has("seccion") ? jsonObj.get("seccion").getAsString() : "";
            
            View cursoCard = crearCursoCard(nombre, profesor, promedio, seccion);
            cursosContainer.addView(cursoCard);
        }
    }

    private View crearCursoCard(String nombre, String profesor, String promedio, String seccion) {
        // Inflar el layout de la card
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_curso_card, cursosContainer, false);
        
        // Obtener referencias a las vistas
        TextView tvCourseName = cardView.findViewById(R.id.tv_course_name);
        TextView tvSalon = cardView.findViewById(R.id.tv_salon);
        TextView tvProfesorName = cardView.findViewById(R.id.tv_profesor_name);
        TextView tvPromedio = cardView.findViewById(R.id.tv_promedio);
        
        // Establecer los datos
        tvCourseName.setText(nombre);
        tvSalon.setText("Salón: " + seccion);
        tvProfesorName.setText(profesor);
        tvPromedio.setText(promedio + " / 20");
        
        // Ocultar salón si es profesor
        if ("PROFESOR".equals(userMode)) {
            tvSalon.setVisibility(View.GONE);
        }
        
        // Configurar click listener
        cardView.setOnClickListener(v -> handleCursoClick(nombre, profesor, promedio, seccion));
        
        return cardView;
    }

    private void handleCursoClick(String nombreCurso, String nombreProfesor, String promedio, String salon) {
        if ("PROFESOR".equals(userMode)) {
            // Profesor: Abrir la nueva Activity con Tabs
            Intent intent = new Intent(this, CursoDetalleProfesorActivity.class);
            intent.putExtra("nombre_curso", nombreCurso);
            intent.putExtra("salon", salon);
            startActivity(intent);
        } else {
            // Alumno: Mostrar Bottom Sheet con sus notas
            CursoDetalleBottomSheet bottomSheet = CursoDetalleBottomSheet.newInstance(
                    nombreCurso,
                    nombreProfesor,
                    promedio
            );
            bottomSheet.show(getSupportFragmentManager(), "CursoDetalle");
        }
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
            } else if (item.getItemId() == R.id.nav_horarios) {
                android.content.Intent intent = new android.content.Intent(this, HorariosActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}