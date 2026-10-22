package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CursoDetalleProfesorActivity extends AppCompatActivity {

    private String nombreCurso;
    private String salon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curso_detalle_profesor);

        // Obtener datos del intent
        nombreCurso = getIntent().getStringExtra("nombre_curso");
        salon = getIntent().getStringExtra("salon");

        // Configurar UI
        TextView tvTitle = findViewById(R.id.tv_curso_nombre_header);
        
        // Lógica para limpiar el nombre del salón (Opción A + C)
        String tituloFinal = nombreCurso;
        if (salon != null && !salon.trim().isEmpty() && !salon.equalsIgnoreCase("sin asignar")) {
            tituloFinal += " • " + salon;
        }
        
        tvTitle.setText(tituloFinal);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        setupTabs();
        setupBottomNavigation();
    }

    private void setupTabs() {
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new SectionsPagerAdapter(this));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        NavigationRoleHelper.apply(this, bottomNav);

        // Configurar colores del BottomNav
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
        
        // Seleccionamos "Cursos" (asumiendo que nav_homework es la opción que lo activó)
        bottomNav.setSelectedItemId(R.id.nav_homework);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_homework) {
                // Ya estamos dentro de un curso, podríamos volver a la lista general
                finish();
                return true;
            } else if (id == R.id.nav_horarios) {
                startActivity(new Intent(this, HorariosActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private class SectionsPagerAdapter extends FragmentStateAdapter {
        public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new EstudiantesFragment();
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

}
