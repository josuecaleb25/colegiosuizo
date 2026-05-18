package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvCelebration, tvDayNumber, tvDayName, tvMonthYear, tvGreeting, tvUserName;
    private ImageView ivDailyIllustration, ivMenuIcon;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View mainContent;
    
    // Vistas de Comunicados
    private LinearLayout containerGlobales, containerSalon, layoutNoComunicados;
    private LinearLayout layoutNoComunicadosGlobales, layoutNoComunicadosSalon;
    private TextView tvTitleSalon;
    private String userMode;
    private boolean comunicadosCargados = false; // Bandera para evitar duplicados
    private boolean esPrimeraVez = true; // Bandera para detectar primera carga

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar tema antes de super.onCreate para evitar parpadeo
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", false);
        int colorScheme = prefs.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Aplicar el esquema de color si es el verde (esquema 2)
        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Obtener el modo de usuario
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = userPrefs.getString("user_mode", "ALUMNO");
        
        // Log para debug del rol
        android.util.Log.d("HomeActivity", "Rol del usuario: '" + userMode + "'");

        // Inicializar vistas del drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        mainContent = findViewById(R.id.main_content);
        ivMenuIcon = findViewById(R.id.iv_menu_icon);
        ivMenuIcon.setTag("closed");

        // Inicializar vistas de comunicados
        containerGlobales = findViewById(R.id.container_comunicados_globales);
        containerSalon = findViewById(R.id.container_comunicados_salon);
        layoutNoComunicados = findViewById(R.id.layout_no_comunicados);
        layoutNoComunicadosGlobales = findViewById(R.id.layout_no_comunicados_globales);
        layoutNoComunicadosSalon = findViewById(R.id.layout_no_comunicados_salon);
        tvTitleSalon = findViewById(R.id.tv_title_salon);

        findViewById(R.id.btn_menu).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        findViewById(R.id.notification_container).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NotificationsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        // Botón "Ver completo" para ir a Horarios
        findViewById(R.id.btn_ver_completo_horario).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HorariosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        setupPerspectiveDrawer();
        setupComunicadosLogic();
        cargarDiasAsistidos(); // Cargar días asistidos desde el backend

        // Configurar navegación del Drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Ya estamos en Home, solo cerrar el drawer
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_cursos) {
                Intent intent = new Intent(HomeActivity.this, CursosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_horarios) {
                Intent intent = new Intent(HomeActivity.this, HorariosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_perfil) {
                Intent intent = new Intent(HomeActivity.this, UserProfile.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_gestion_comunicados) {
                Intent intent = new Intent(HomeActivity.this, GestionComunicadosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_asistencia) {
                Intent intent = new Intent(HomeActivity.this, PanelAsistencia.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_identificacion) {
                // Navegación a identificación (puedes añadir la actividad aquí luego)
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_panel_admin) {
                Intent intent = new Intent(HomeActivity.this, AdminPanelActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_switch_role) {
                switchUserRole();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            // Cerrar el drawer para otros ítems si es necesario
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Inicializar vistas del header dinámico
        tvCelebration = findViewById(R.id.tv_celebration);
        tvDayNumber = findViewById(R.id.tv_day_number);
        tvDayName = findViewById(R.id.tv_day_name);
        tvMonthYear = findViewById(R.id.tv_month_year);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvUserName = findViewById(R.id.tv_user_name);
        ivDailyIllustration = findViewById(R.id.iv_daily_illustration);

        updateDailyInfo();
        actualizarDatosUsuario();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 1. Obtener color del Drawer (Verde Oscuro en Scheme Green)
        int colorSeleccionado;
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(R.attr.drawerBackground, typedValue, true)) {
            colorSeleccionado = typedValue.data;
        } else {
            colorSeleccionado = Color.parseColor("#BA1924"); // Fallback rojo
        }

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                colorSeleccionado,
                Color.parseColor("#5E5F60")
        };
        ColorStateList navTint = new ColorStateList(states, colors);

        // 2. Estado inicial
        bottomNav.setItemIconTintList(navTint);
        bottomNav.setItemTextColor(navTint);
        bottomNav.getMenu().setGroupCheckable(0, false, true);
        bottomNav.getMenu().getItem(0).setChecked(false);

        bottomNav.setOnItemSelectedListener(item -> {
            bottomNav.getMenu().setGroupCheckable(0, true, true);
            bottomNav.getMenu().findItem(R.id.nav_home).setIcon(R.drawable.ic_home);

            if (item.getItemId() == R.id.nav_home) {
                // Para el Home presionado, aplicamos el tinte dinámico si es verde
                if (colorScheme == 2) {
                    bottomNav.setItemIconTintList(navTint);
                } else {
                    bottomNav.setItemIconTintList(null);
                }
                item.setIcon(R.drawable.ic_homepress);
            } else if (item.getItemId() == R.id.nav_homework) {
                android.content.Intent intent = new android.content.Intent(this, CursosActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_horarios) {
                android.content.Intent intent = new android.content.Intent(this, HorariosActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else {
                bottomNav.setItemIconTintList(navTint);
            }

            return true;
        });
    }

    private void switchUserRole() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentRole = prefs.getString("user_mode", "ALUMNO");
        String nextRole;

        if ("ALUMNO".equals(currentRole)) {
            nextRole = "PROFESOR";
        } else if ("PROFESOR".equals(currentRole)) {
            nextRole = "ADMIN";
        } else {
            nextRole = "ALUMNO";
        }

        prefs.edit().putString("user_mode", nextRole).apply();
        userMode = nextRole;

        android.widget.Toast.makeText(this, "Rol cambiado a: " + nextRole, android.widget.Toast.LENGTH_SHORT).show();

        // Refrescar la UI y el menú
        actualizarVisibilidadMenuLateral();
        actualizarDatosUsuario();
        comunicadosCargados = false;
        cargarComunicados();
    }

    private void actualizarDatosUsuario() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "Usuario");
        String role = prefs.getString("user_mode", "ALUMNO");

        // Obtener solo el primer nombre
        String primerNombre = name.split(" ")[0];

        // Actualizar saludo con solo el primer nombre
        if (tvUserName != null) {
            tvUserName.setText(primerNombre);
        }

        // Actualizar Drawer Header con nombre completo
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvNameDrawer = headerView.findViewById(R.id.tv_user_name_drawer);
            TextView tvRoleDrawer = headerView.findViewById(R.id.tv_user_role_drawer);

            if (tvNameDrawer != null) tvNameDrawer.setText(name); // Nombre completo en el drawer
            if (tvRoleDrawer != null) tvRoleDrawer.setText(role);
        }
    }

    private void setupComunicadosLogic() {
        // Normalizar el rol
        String rolNormalizado = userMode != null ? userMode.toUpperCase().trim() : "ALUMNO";
        
        // Configurar título de la segunda sección según el rol
        if ("ADMIN".equals(rolNormalizado) || "ADMINISTRADOR".equals(rolNormalizado)) {
            tvTitleSalon.setText("Comunicados de Salones");
            tvTitleSalon.setVisibility(View.VISIBLE);
            
        } else if ("PROFESOR".equals(rolNormalizado)) {
            tvTitleSalon.setText("Comunicados de mis Salones");
            tvTitleSalon.setVisibility(View.VISIBLE);
            
        } else if ("AUXILIAR".equals(rolNormalizado)) {
            tvTitleSalon.setText("Comunicados de Auxiliar");
            tvTitleSalon.setVisibility(View.VISIBLE);
            
        } else {
            // ALUMNO - Obtener la sección del usuario desde SharedPreferences
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String seccionAlumno = prefs.getString("user_seccion", "4to A");
            tvTitleSalon.setText("Comunicados de mi Salón (" + seccionAlumno + ")");
            tvTitleSalon.setVisibility(View.VISIBLE);
        }

        cargarComunicados();
        actualizarVisibilidadMenuLateral();
    }

    private void actualizarVisibilidadMenuLateral() {
        android.view.Menu menu = navigationView.getMenu();
        
        // Log para debug
        android.util.Log.d("HomeActivity", "Actualizando menú para rol: '" + userMode + "'");
        
        // Items base (Siempre visibles para todos)
        menu.findItem(R.id.nav_home).setVisible(true);
        menu.findItem(R.id.nav_cursos).setVisible(true);
        menu.findItem(R.id.nav_horarios).setVisible(true);
        menu.findItem(R.id.nav_perfil).setVisible(true);
        menu.findItem(R.id.nav_identificacion).setVisible(true);
        
        // Cambiar de Rol solo visible para administradores
        menu.findItem(R.id.nav_switch_role).setVisible(false);

        // Lógica de visibilidad por Rol
        // Normalizar el rol para comparación (aceptar variantes)
        String rolNormalizado = userMode != null ? userMode.toUpperCase().trim() : "PADRE";
        
        if ("ALUMNO".equals(rolNormalizado) || "PADRE".equals(rolNormalizado)) {
            menu.findItem(R.id.nav_cursos).setVisible(true);
            menu.findItem(R.id.nav_horarios).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(false);
            menu.findItem(R.id.nav_asistencia).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);
            
        } else if ("PROFESOR".equals(rolNormalizado)) {
            menu.findItem(R.id.nav_cursos).setVisible(true);
            menu.findItem(R.id.nav_horarios).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(true);
            menu.findItem(R.id.nav_asistencia).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);
            
        } else if ("AUXILIAR".equals(rolNormalizado)) {
            // Auxiliar: Solo Inicio, Comunicados y Asistencia
            menu.findItem(R.id.nav_home).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(true);
            menu.findItem(R.id.nav_asistencia).setVisible(true);
            
            // Ocultar el resto
            menu.findItem(R.id.nav_cursos).setVisible(false);
            menu.findItem(R.id.nav_horarios).setVisible(false);
            menu.findItem(R.id.nav_perfil).setVisible(false);
            menu.findItem(R.id.nav_identificacion).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);

        } else if ("ADMIN".equals(rolNormalizado) || "ADMINISTRADOR".equals(rolNormalizado)) {
            // Aceptar tanto ADMIN como ADMINISTRADOR
            android.util.Log.d("HomeActivity", "Usuario es ADMINISTRADOR - Mostrando panel admin");
            menu.findItem(R.id.nav_home).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_asistencia).setVisible(true);
            menu.findItem(R.id.nav_switch_role).setVisible(true); // Solo admin puede cambiar de rol
            menu.findItem(R.id.nav_panel_admin).setVisible(true);
            
            // Ocultar Cursos y Horarios para el Admin
            menu.findItem(R.id.nav_cursos).setVisible(false);
            menu.findItem(R.id.nav_horarios).setVisible(false);
        } else {
            // Rol desconocido - mostrar como alumno por defecto
            android.util.Log.w("HomeActivity", "Rol desconocido: '" + userMode + "' - usando permisos de ALUMNO");
            menu.findItem(R.id.nav_cursos).setVisible(true);
            menu.findItem(R.id.nav_horarios).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(false);
            menu.findItem(R.id.nav_asistencia).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);
        }
    }

    private void cargarComunicados() {
        // Limpiar siempre antes de cargar
        containerGlobales.removeAllViews();
        containerSalon.removeAllViews();
        
        // Evitar cargar múltiples veces en la misma sesión
        if (comunicadosCargados) {
            android.util.Log.d("HomeActivity", "Comunicados ya cargados, saltando...");
            return;
        }

        // Cargar comunicados desde el backend
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            layoutNoComunicados.setVisibility(View.VISIBLE);
            layoutNoComunicadosGlobales.setVisibility(View.GONE);
            layoutNoComunicadosSalon.setVisibility(View.GONE);
            return;
        }

        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .getComunicados(null, null, userId)
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, 
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Object> comunicadosData = response.body().getData();
                        
                        List<Comunicado> globales = new ArrayList<>();
                        List<Comunicado> salon = new ArrayList<>();
                        
                        // Parsear comunicados del backend
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        for (Object obj : comunicadosData) {
                            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                            
                            String titulo = jsonObj.has("titulo") ? jsonObj.get("titulo").getAsString() : "";
                            String contenido = jsonObj.has("contenido") ? jsonObj.get("contenido").getAsString() : "";
                            String emisor = jsonObj.has("emisor") ? jsonObj.get("emisor").getAsString() : "Administración";
                            
                            // Obtener información del salón si existe
                            String salonInfo = jsonObj.has("seccion") ? jsonObj.get("seccion").getAsString() : null;
                            
                            // Obtener fecha de publicación
                            String fechaPublicacion = jsonObj.has("fecha_publicacion") ? jsonObj.get("fecha_publicacion").getAsString() : "";
                            
                            // Usar destinatario_tipo para clasificar correctamente
                            String destinatarioTipo = jsonObj.has("destinatario_tipo") ? 
                                jsonObj.get("destinatario_tipo").getAsString() : "";
                            
                            // Normalizar el destinatario_tipo
                            destinatarioTipo = destinatarioTipo.toUpperCase().trim();
                            
                            // Clasificar según destinatario_tipo
                            boolean esGlobal = "GLOBAL".equals(destinatarioTipo);
                            
                            // Log para debug
                            android.util.Log.d("HomeActivity", "Comunicado: " + titulo + 
                                " | Destinatario: '" + destinatarioTipo + "' | Emisor: '" + emisor + "'");
                            
                            Comunicado comunicado = new Comunicado(titulo, contenido, null, salonInfo, emisor);
                            comunicado.fechaPublicacion = fechaPublicacion;
                            
                            if (esGlobal) {
                                globales.add(comunicado);
                                android.util.Log.d("HomeActivity", "  -> Agregado a GLOBALES");
                            } else {
                                salon.add(comunicado);
                                android.util.Log.d("HomeActivity", "  -> Agregado a SALÓN");
                            }
                        }
                        
                        // Ordenar comunicados del más reciente al más antiguo
                        globales.sort((c1, c2) -> {
                            if (c1.fechaPublicacion == null) return 1;
                            if (c2.fechaPublicacion == null) return -1;
                            return c2.fechaPublicacion.compareTo(c1.fechaPublicacion);
                        });
                        
                        salon.sort((c1, c2) -> {
                            if (c1.fechaPublicacion == null) return 1;
                            if (c2.fechaPublicacion == null) return -1;
                            return c2.fechaPublicacion.compareTo(c1.fechaPublicacion);
                        });
                        
                        // Ocultar el mensaje general de "no hay comunicados"
                        layoutNoComunicados.setVisibility(View.GONE);
                        
                        // Manejar comunicados globales
                        if (globales.isEmpty()) {
                            layoutNoComunicadosGlobales.setVisibility(View.VISIBLE);
                        } else {
                            layoutNoComunicadosGlobales.setVisibility(View.GONE);
                            for (Comunicado c : globales) {
                                containerGlobales.addView(crearTarjetaComunicado(c, containerGlobales));
                            }
                            // Deshabilitar scroll si solo hay una tarjeta
                            View scrollGlobales = containerGlobales.getParent() instanceof View ? 
                                (View) containerGlobales.getParent() : null;
                            if (scrollGlobales instanceof android.widget.HorizontalScrollView) {
                                if (globales.size() == 1) {
                                    scrollGlobales.setEnabled(false);
                                    scrollGlobales.setHorizontalScrollBarEnabled(false);
                                } else {
                                    scrollGlobales.setEnabled(true);
                                }
                            }
                        }
                        
                        // Manejar comunicados de salón
                        if (salon.isEmpty()) {
                            layoutNoComunicadosSalon.setVisibility(View.VISIBLE);
                        } else {
                            layoutNoComunicadosSalon.setVisibility(View.GONE);
                            for (Comunicado c : salon) {
                                containerSalon.addView(crearTarjetaComunicado(c, containerSalon));
                            }
                            // Deshabilitar scroll si solo hay una tarjeta
                            View scrollSalon = containerSalon.getParent() instanceof View ? 
                                (View) containerSalon.getParent() : null;
                            if (scrollSalon instanceof android.widget.HorizontalScrollView) {
                                if (salon.size() == 1) {
                                    scrollSalon.setEnabled(false);
                                    scrollSalon.setHorizontalScrollBarEnabled(false);
                                } else {
                                    scrollSalon.setEnabled(true);
                                }
                            }
                        }
                        
                        // Marcar como cargados
                        comunicadosCargados = true;
                        
                    } else {
                        // Error en la respuesta - mostrar mensaje general
                        layoutNoComunicados.setVisibility(View.VISIBLE);
                        layoutNoComunicadosGlobales.setVisibility(View.GONE);
                        layoutNoComunicadosSalon.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    android.widget.Toast.makeText(HomeActivity.this, "Error al cargar comunicados: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    layoutNoComunicados.setVisibility(View.VISIBLE);
                    layoutNoComunicadosGlobales.setVisibility(View.GONE);
                    layoutNoComunicadosSalon.setVisibility(View.GONE);
                }
            });
    }

    private View crearTarjetaComunicado(Comunicado comunicado, android.view.ViewGroup parent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_comunicado, parent, false);
        
        // El ancho ya está fijo en el XML (310dp)
        
        // Configurar título
        ((TextView) view.findViewById(R.id.tv_titulo_comunicado)).setText(comunicado.titulo);
        
        // Configurar hora del comunicado
        TextView tvHora = view.findViewById(R.id.tv_hora_comunicado);
        if (comunicado.fechaPublicacion != null && !comunicado.fechaPublicacion.isEmpty()) {
            try {
                // Formato del backend: "2024-05-14T10:30:00"
                String[] partes = comunicado.fechaPublicacion.split("T");
                if (partes.length > 1) {
                    String[] horaPartes = partes[1].split(":");
                    int hora = Integer.parseInt(horaPartes[0]);
                    int minuto = Integer.parseInt(horaPartes[1]);
                    String ampm = hora >= 12 ? "PM" : "AM";
                    if (hora > 12) hora -= 12;
                    if (hora == 0) hora = 12;
                    tvHora.setText(String.format("%d:%02d %s", hora, minuto, ampm));
                } else {
                    tvHora.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                tvHora.setVisibility(View.GONE);
            }
        } else {
            tvHora.setVisibility(View.GONE);
        }
        
        // Configurar contenido
        TextView tvContenido = view.findViewById(R.id.tv_contenido_comunicado);
        TextView btnVerMas = view.findViewById(R.id.btn_ver_mas_comunicado);
        
        tvContenido.setText(comunicado.contenido);
        tvContenido.setVisibility(View.VISIBLE);

        // Si el texto es largo, mostrar "Ver más"
        if (comunicado.contenido != null && comunicado.contenido.length() > 100) {
            btnVerMas.setVisibility(View.VISIBLE);
        } else {
            btnVerMas.setVisibility(View.GONE);
        }
        
        // Configurar tag de salón
        TextView tvTagSalon = view.findViewById(R.id.tv_tag_salon);
        if (comunicado.salon != null && !comunicado.salon.isEmpty()) {
            tvTagSalon.setText("Salón: " + comunicado.salon);
            tvTagSalon.setVisibility(View.VISIBLE);
        } else {
            tvTagSalon.setText("Enviado por: " + comunicado.emisor);
            tvTagSalon.setVisibility(View.VISIBLE);
        }
        
        // Ocultar elementos que no se usan en Home
        view.findViewById(R.id.tv_estado).setVisibility(View.GONE);
        view.findViewById(R.id.btn_opciones).setVisibility(View.GONE);
        view.findViewById(R.id.tv_fecha_destinatario).setVisibility(View.GONE);

        // Al hacer clic, abrir modal
        view.setOnClickListener(v -> mostrarModalComunicado(comunicado));

        return view;
    }

    private void mostrarModalComunicado(Comunicado comunicado) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View modalView = LayoutInflater.from(this).inflate(R.layout.dialog_comunicado_detalle, null);
        
        // Configurar contenido del diálogo
        ((TextView) modalView.findViewById(R.id.tv_titulo_detalle)).setText(comunicado.titulo);
        ((TextView) modalView.findViewById(R.id.tv_contenido_detalle)).setText(comunicado.contenido);
        
        // Configurar hora
        TextView tvHoraDetalle = modalView.findViewById(R.id.tv_hora_detalle);
        if (comunicado.fechaPublicacion != null && !comunicado.fechaPublicacion.isEmpty()) {
            try {
                String[] partes = comunicado.fechaPublicacion.split("T");
                if (partes.length > 1) {
                    String[] horaPartes = partes[1].split(":");
                    int hora = Integer.parseInt(horaPartes[0]);
                    int minuto = Integer.parseInt(horaPartes[1]);
                    String ampm = hora >= 12 ? "PM" : "AM";
                    if (hora > 12) hora -= 12;
                    if (hora == 0) hora = 12;
                    tvHoraDetalle.setText(String.format("%d:%02d %s", hora, minuto, ampm));
                } else {
                    tvHoraDetalle.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                tvHoraDetalle.setVisibility(View.GONE);
            }
        } else {
            tvHoraDetalle.setVisibility(View.GONE);
        }
        
        // Configurar tag
        TextView tvTagDetalle = modalView.findViewById(R.id.tv_tag_detalle);
        if (comunicado.salon != null && !comunicado.salon.isEmpty()) {
            tvTagDetalle.setText("Salón: " + comunicado.salon);
        } else {
            tvTagDetalle.setText("Enviado por: " + comunicado.emisor);
        }

        builder.setView(modalView);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void mostrarDialogoEnviarComunicado() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enviar_comunicado, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        // Configurar el Spinner con salones
        android.widget.Spinner spinner = dialogView.findViewById(R.id.spinner_salon);
        String[] salones = {"Global (Toda la Institución)", "1ro A", "2do B", "3ro C", "4to A", "5to B"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, salones);
        spinner.setAdapter(adapter);

        dialogView.findViewById(R.id.btn_subir_banner).setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Galería abierta (Simulado)", android.widget.Toast.LENGTH_SHORT).show());

        dialogView.findViewById(R.id.btn_cancelar).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btn_publicar).setOnClickListener(v -> {
            String titulo = ((android.widget.EditText) dialogView.findViewById(R.id.et_titulo)).getText().toString();
            if (titulo.isEmpty()) {
                android.widget.Toast.makeText(this, "Por favor, escribe un título", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "¡Comunicado publicado con éxito!", android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private static class Comunicado {
        String titulo, contenido, salon, emisor, fechaPublicacion;
        Integer bannerRes;
        Comunicado(String t, String c, Integer b, String s, String e) { 
            titulo = t; contenido = c; bannerRes = b; salon = s; emisor = e; 
        }
    }

    private void setupPerspectiveDrawer() {
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        mainContent.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = 80f;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                float scaleFactor = 1f - (slideOffset * 0.15f);
                mainContent.setScaleX(scaleFactor);
                mainContent.setScaleY(scaleFactor);

                float xOffset = drawerView.getWidth() * slideOffset * 0.7f;
                mainContent.setTranslationX(xOffset);

                mainContent.setClipToOutline(slideOffset > 0);
                mainContent.setElevation(slideOffset * 20);

                float iconAlpha = Math.abs(slideOffset - 0.5f) * 2;
                ivMenuIcon.setAlpha(iconAlpha);
                ivMenuIcon.setScaleX(0.8f + (iconAlpha * 0.2f));
                ivMenuIcon.setScaleY(0.8f + (iconAlpha * 0.2f));

                if (slideOffset > 0.5f) {
                    if (!"open".equals(ivMenuIcon.getTag())) {
                        ivMenuIcon.setImageResource(R.drawable.hamburgicon);
                        ivMenuIcon.setTag("open");
                    }
                } else {
                    if (!"closed".equals(ivMenuIcon.getTag())) {
                        ivMenuIcon.setImageResource(R.drawable.hamburgiconnew);
                        ivMenuIcon.setTag("closed");
                    }
                }
            }
        });
    }

    private void updateDailyInfo() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 6 && hour < 12) {
            greeting = "Buenos días, ";
        } else if (hour >= 12 && hour < 19) {
            greeting = "Buenas tardes, ";
        } else {
            greeting = "Buenas noches, ";
        }
        tvGreeting.setText(greeting);

        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE", new Locale("es", "PE"));
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));

        String dayNumber = dayFormat.format(calendar.getTime());
        String dayName = dayNameFormat.format(calendar.getTime());
        String monthYear = monthYearFormat.format(calendar.getTime());

        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
        monthYear = monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1);

        tvDayNumber.setText(dayNumber);
        tvDayName.setText(dayName);
        tvMonthYear.setText(monthYear);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;

        String celebrationText = "";
        int illustrationRes = R.drawable.diamundial;

        if (day == 22 && month == 4) {
            celebrationText = "Día Mundial de la Tierra";
        } else if (day == 1 && month == 5) {
            celebrationText = "Día del Trabajador";
        } else if (day == 7 && month == 6) {
            celebrationText = "Día de la Bandera";
        }

        if (celebrationText.isEmpty()) {
            tvCelebration.setVisibility(View.GONE);
        } else {
            tvCelebration.setVisibility(View.VISIBLE);
            tvCelebration.setText(celebrationText);
        }

        if (celebrationText.isEmpty()) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeek) {
                case Calendar.MONDAY: illustrationRes = R.drawable.pibblepruebaportada; break;
                case Calendar.TUESDAY: illustrationRes = R.drawable.diamundial; break;
                case Calendar.WEDNESDAY: illustrationRes = R.drawable.diamundial; break;
                case Calendar.THURSDAY: illustrationRes = R.drawable.diamundial; break;
                case Calendar.FRIDAY: illustrationRes = R.drawable.diamundial; break;
                default: illustrationRes = R.drawable.portadaimagenprueba; break;
            }
        }

        ivDailyIllustration.setImageResource(illustrationRes);
    }
    
    private void cargarDiasAsistidos() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            android.util.Log.w("HomeActivity", "No se encontró user_id para cargar asistencia");
            return;
        }
        
        // Llamar al endpoint de días asistidos
        com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
            .getDiasAsistidos(userId, "actual")
            .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call,
                                     retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Object data = response.body().getData();
                        
                        // Parsear la respuesta
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonObject jsonData = gson.toJsonTree(data).getAsJsonObject();
                        
                        int rachaActual = jsonData.has("racha_actual") ? jsonData.get("racha_actual").getAsInt() : 0;
                        
                        // Obtener asistencias de la semana
                        com.google.gson.JsonObject asistenciasSemana = jsonData.has("asistencias_semana") ? 
                            jsonData.getAsJsonObject("asistencias_semana") : new com.google.gson.JsonObject();
                        
                        boolean lunes = asistenciasSemana.has("lunes") && asistenciasSemana.get("lunes").getAsBoolean();
                        boolean martes = asistenciasSemana.has("martes") && asistenciasSemana.get("martes").getAsBoolean();
                        boolean miercoles = asistenciasSemana.has("miercoles") && asistenciasSemana.get("miercoles").getAsBoolean();
                        boolean jueves = asistenciasSemana.has("jueves") && asistenciasSemana.get("jueves").getAsBoolean();
                        boolean viernes = asistenciasSemana.has("viernes") && asistenciasSemana.get("viernes").getAsBoolean();
                        
                        // Actualizar UI
                        actualizarUIAsistencia(rachaActual, lunes, martes, miercoles, jueves, viernes);
                        
                    } else {
                        android.util.Log.w("HomeActivity", "Error al cargar días asistidos: " + response.message());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                    android.util.Log.e("HomeActivity", "Error al cargar días asistidos: " + t.getMessage());
                }
            });
    }
    
    private void actualizarUIAsistencia(int racha, boolean lunes, boolean martes, boolean miercoles, boolean jueves, boolean viernes) {
        // Actualizar racha (días seguidos)
        TextView tvRacha = findViewById(R.id.tv_streak_number);
        if (tvRacha != null) {
            tvRacha.setText(String.valueOf(racha));
        }
        
        // Actualizar checkmarks de días de la semana
        ImageView checkLunes = findViewById(R.id.check_lunes);
        ImageView checkMartes = findViewById(R.id.check_martes);
        ImageView checkMiercoles = findViewById(R.id.check_miercoles);
        ImageView checkJueves = findViewById(R.id.check_jueves);
        ImageView checkViernes = findViewById(R.id.check_viernes);
        
        // Actualizar visibilidad según asistencia
        if (checkLunes != null) checkLunes.setVisibility(lunes ? View.VISIBLE : View.INVISIBLE);
        if (checkMartes != null) checkMartes.setVisibility(martes ? View.VISIBLE : View.INVISIBLE);
        if (checkMiercoles != null) checkMiercoles.setVisibility(miercoles ? View.VISIBLE : View.INVISIBLE);
        if (checkJueves != null) checkJueves.setVisibility(jueves ? View.VISIBLE : View.INVISIBLE);
        if (checkViernes != null) checkViernes.setVisibility(viernes ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        // Actualizar rol del usuario por si cambió
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = prefs.getString("user_mode", "ALUMNO");
        
        // Actualizar visibilidad del menú y datos del usuario
        actualizarVisibilidadMenuLateral();
        actualizarDatosUsuario();
        
        // Solo recargar si NO es la primera vez (onCreate ya cargó)
        if (!esPrimeraVez) {
            comunicadosCargados = false;
            cargarComunicados();
        } else {
            esPrimeraVez = false;
        }
    }
}
