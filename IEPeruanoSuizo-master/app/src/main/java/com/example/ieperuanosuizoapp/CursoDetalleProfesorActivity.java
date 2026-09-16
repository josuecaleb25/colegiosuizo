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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CursoDetalleProfesorActivity extends AppCompatActivity {

    private String cursoId;
    private String nombreCurso;
    private String salon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curso_detalle_profesor);

        // Obtener datos del intent
        cursoId = getIntent().getStringExtra("curso_id");
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
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);

        SectionsPagerAdapter adapter = new SectionsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Estudiantes");
                    break;
                case 1:
                    tab.setText("Evaluaciones");
                    break;
            }
        }).attach();

        // Listener para detectar cambios de pestaña
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Cuando se selecciona la pestaña de Estudiantes, notificar al fragmento
                if (position == 0) {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof ContainerEstudiantesFragment) {
                        ((ContainerEstudiantesFragment) fragment).onTabSelected();
                    }
                }
            }
        });
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
            if (position == 0) {
                return ContainerEstudiantesFragment.newInstance(cursoId, salon);
            } else if (position == 1) {
                return EvaluacionesFragment.newInstance(cursoId); // cursoId es realmente asignacion_id
            }
            return ContainerEstudiantesFragment.newInstance(cursoId, salon);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

}
