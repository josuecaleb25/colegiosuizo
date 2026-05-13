package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.Usuario;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuariosActivity extends AppCompatActivity implements UsuariosAdapter.OnUsuarioClickListener {

    private RecyclerView rvUsuarios;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmptyState;
    private TextView tvTotalUsuarios;
    private TextInputEditText searchEditText;
    private ChipGroup chipGroupFilters;
    
    private UsuariosAdapter adapter;
    private List<Usuario> todosLosUsuarios = new ArrayList<>();
    private String filtroSeccionActual = "Todos";
    private String busquedaActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupBottomNavigation();
        
        cargarUsuarios();
    }

    private void initViews() {
        CardView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvUsuarios = findViewById(R.id.rv_usuarios);
        progressLoading = findViewById(R.id.progress_loading);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvTotalUsuarios = findViewById(R.id.tv_total_usuarios);
        searchEditText = findViewById(R.id.search_edit_text);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
    }

    private void setupRecyclerView() {
        adapter = new UsuariosAdapter(this);
        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        rvUsuarios.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                busquedaActual = s.toString();
                filtrarUsuarios();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                filtroSeccionActual = "Todos";
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    filtroSeccionActual = chip.getText().toString();
                }
            }
            filtrarUsuarios();
        });
    }

    private void cargarUsuarios() {
        mostrarCargando(true);

        RetrofitClient.getApiService()
                .getUsuariosAdmin(null, null, 500)
                .enqueue(new Callback<ApiResponse<List<Usuario>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Usuario>>> call, 
                                         Response<ApiResponse<List<Usuario>>> response) {
                        mostrarCargando(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            todosLosUsuarios = response.body().getData();
                            
                            // Debug: Verificar datos QR
                            if (!todosLosUsuarios.isEmpty()) {
                                Usuario primerUsuario = todosLosUsuarios.get(0);
                                android.util.Log.d("UsuariosActivity", "Primer usuario: " + primerUsuario.getNombreCompleto());
                                android.util.Log.d("UsuariosActivity", "QR Code: " + primerUsuario.getQrCode());
                                android.util.Log.d("UsuariosActivity", "QR Image length: " + 
                                    (primerUsuario.getQrImage() != null ? primerUsuario.getQrImage().length() : 0));
                            }
                            
                            filtrarUsuarios();
                        } else {
                            mostrarError("No se pudieron cargar los usuarios");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Usuario>>> call, Throwable t) {
                        mostrarCargando(false);
                        mostrarError("Error de conexión: " + t.getMessage());
                    }
                });
    }

    private void filtrarUsuarios() {
        List<Usuario> usuariosFiltrados = new ArrayList<>();

        for (Usuario usuario : todosLosUsuarios) {
            boolean cumpleFiltroSeccion = filtroSeccionActual.equals("Todos") || 
                    usuario.getSeccion().contains(filtroSeccionActual);

            boolean cumpleBusqueda = busquedaActual.isEmpty() ||
                    usuario.getNombreCompleto().toLowerCase().contains(busquedaActual.toLowerCase()) ||
                    usuario.getCodigoAlumno().toLowerCase().contains(busquedaActual.toLowerCase()) ||
                    usuario.getSeccion().toLowerCase().contains(busquedaActual.toLowerCase());

            if (cumpleFiltroSeccion && cumpleBusqueda) {
                usuariosFiltrados.add(usuario);
            }
        }

        adapter.setUsuarios(usuariosFiltrados);
        actualizarContador(usuariosFiltrados.size());
        
        if (usuariosFiltrados.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvUsuarios.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvUsuarios.setVisibility(View.VISIBLE);
        }
    }

    private void actualizarContador(int total) {
        tvTotalUsuarios.setText(total + " estudiante" + (total != 1 ? "s" : ""));
    }

    private void mostrarCargando(boolean mostrar) {
        progressLoading.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        rvUsuarios.setVisibility(mostrar ? View.GONE : View.VISIBLE);
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvUsuarios.setVisibility(View.GONE);
    }

    @Override
    public void onUsuarioClick(Usuario usuario) {
        // TODO: Abrir detalle del usuario o bottom sheet con opciones
        Toast.makeText(this, "Usuario: " + usuario.getNombreCompleto(), Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

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

        bottomNav.getMenu().setGroupCheckable(0, false, true);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_homework) {
                Intent intent = new Intent(this, CursosActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_horarios) {
                Intent intent = new Intent(this, HorariosActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }
}
