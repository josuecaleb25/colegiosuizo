package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvCelebration, tvDayNumber, tvDayName, tvMonthYear, tvGreeting, tvUserName;
    private ImageView ivDailyIllustration, ivMenuIcon;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View mainContent;
    
    // Vistas de Comunicados
    private View cardComunicadoGlobal, cardComunicadoSalon, cardComunicadoSalones;
    private LinearLayout layoutNoComunicados;
    private LinearLayout layoutNoComunicadosGlobales, layoutNoComunicadosSalon, layoutNoComunicadosSalones;
    private View headerComunicadosSalon, headerComunicadosSalones;
    private TextView tvTitleSalon, tvTitleSalones;
    private String userMode;
    private boolean comunicadosCargados = false; // Bandera para evitar duplicados
    private boolean esPrimeraVez = true; // Bandera para detectar primera carga
    private Handler horarioHandler = new Handler();
    private List<Object> ultimosHorarios;
    private Runnable horarioRunnable;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos

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

        // Solicitar permiso de notificaciones (Android 13+)
        NotificationPermissionHelper.requestNotificationPermission(this);

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
        cardComunicadoGlobal = findViewById(R.id.card_comunicado_global);
        cardComunicadoSalon = findViewById(R.id.card_comunicado_salon);
        cardComunicadoSalones = findViewById(R.id.card_comunicado_salones);
        layoutNoComunicados = findViewById(R.id.layout_no_comunicados);
        layoutNoComunicadosGlobales = findViewById(R.id.layout_no_comunicados_globales);
        layoutNoComunicadosSalon = findViewById(R.id.layout_no_comunicados_salon);
        layoutNoComunicadosSalones = findViewById(R.id.layout_no_comunicados_salones);
        headerComunicadosSalon = findViewById(R.id.header_comunicados_salon);
        headerComunicadosSalones = findViewById(R.id.header_comunicados_salones);
        tvTitleSalon = findViewById(R.id.tv_title_salon);
        tvTitleSalones = findViewById(R.id.tv_title_salones);

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
        
        // Botón "Ver todos" para Comunicados Globales
        findViewById(R.id.btn_ver_mas_comunicados).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ViewComunicadosActivity.class);
            intent.putExtra("tipo", "GLOBAL");
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        // Botón "Ver todos" para Comunicados de mi Salón
        findViewById(R.id.btn_ver_mas_salon).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ViewComunicadosActivity.class);
            intent.putExtra("tipo", "MI_SALON");
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        // Botón "Ver todos" para Comunicados de Salones
        findViewById(R.id.btn_ver_mas_salones).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ViewComunicadosActivity.class);
            intent.putExtra("tipo", "SALONES");
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        setupPerspectiveDrawer();
        setupComunicadosLogic();
        cargarDiasAsistidos(); // Cargar días asistidos desde el backend
        cargarHorarioActual(); // Cargar horario del día actual

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
                Intent intent = new Intent(HomeActivity.this, IdentificacionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
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

        // Obtener solo el primer nombre
        String primerNombre = name.split(" ")[0];

        // Actualizar saludo con solo el primer nombre
        if (tvUserName != null) {
            tvUserName.setText(primerNombre);
        }
    }

    private void setupComunicadosLogic() {
        String rolNormalizado = userMode != null ? userMode.toUpperCase().trim() : "ALUMNO";
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String seccionAlumno = prefs.getString("user_seccion", "");

        android.util.Log.d("HomeActivity", "setupComunicadosLogic - rol: '" + rolNormalizado + "' seccion: '" + seccionAlumno + "'");

        boolean esProfesor = "PROFESOR".equals(rolNormalizado);
        boolean esAdmin    = "ADMIN".equals(rolNormalizado) || "ADMINISTRADOR".equals(rolNormalizado);
        // Todo lo que NO es profesor ni admin se trata como alumno
        boolean esAlumno   = !esProfesor && !esAdmin;

        if (esAlumno) {
            // ALUMNO / ESTUDIANTE / cualquier otro rol
            String tituloSalon = (seccionAlumno != null && !seccionAlumno.isEmpty())
                ? "Comunicados de mi Salón (" + seccionAlumno + ")"
                : "Comunicados de mi Salón";
            tvTitleSalon.setText(tituloSalon);
            headerComunicadosSalon.setVisibility(View.VISIBLE);
            headerComunicadosSalones.setVisibility(View.GONE);

        } else if (esProfesor) {
            tvTitleSalones.setText("Comunicados de mis Salones");
            headerComunicadosSalon.setVisibility(View.GONE);
            headerComunicadosSalones.setVisibility(View.VISIBLE);

        } else {
            // Admin
            tvTitleSalones.setText("Comunicados por Salón");
            headerComunicadosSalon.setVisibility(View.GONE);
            headerComunicadosSalones.setVisibility(View.VISIBLE);
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
        menu.findItem(R.id.nav_identificacion).setVisible(false);
        
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
            menu.findItem(R.id.nav_identificacion).setVisible(false);
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
            menu.findItem(R.id.nav_identificacion).setVisible(false);
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
            menu.findItem(R.id.nav_identificacion).setVisible(false);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(false);
            menu.findItem(R.id.nav_asistencia).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);
        }
    }

    private void cargarComunicados() {
        // Evitar cargar múltiples veces en la misma sesión
        if (comunicadosCargados) {
            android.util.Log.d("HomeActivity", "Comunicados ya cargados, saltando...");
            return;
        }

        // Cargar comunicados desde el backend
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String miSeccion = prefs.getString("user_seccion", "");
        String rolNormalizado = userMode != null ? userMode.toUpperCase().trim() : "ALUMNO";

        boolean esProfesor = "PROFESOR".equals(rolNormalizado);
        boolean esAdmin    = "ADMIN".equals(rolNormalizado) || "ADMINISTRADOR".equals(rolNormalizado);
        boolean esAlumno   = !esProfesor && !esAdmin;

        android.util.Log.d("HomeActivity", "cargarComunicados - rol: '" + rolNormalizado + "' esAlumno: " + esAlumno);

        if (userId == null) {
            layoutNoComunicados.setVisibility(View.VISIBLE);
            layoutNoComunicadosGlobales.setVisibility(View.GONE);
            layoutNoComunicadosSalon.setVisibility(View.GONE);
            layoutNoComunicadosSalones.setVisibility(View.GONE);
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
                        List<Comunicado> miSalon = new ArrayList<>();
                        List<Comunicado> otrosSalones = new ArrayList<>();
                        
                        // Parsear comunicados del backend
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        for (Object obj : comunicadosData) {
                            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
                            
                            String titulo = jsonObj.has("titulo") ? jsonObj.get("titulo").getAsString() : "";
                            String contenido = jsonObj.has("contenido") ? jsonObj.get("contenido").getAsString() : "";
                            String emisor = jsonObj.has("emisor") ? jsonObj.get("emisor").getAsString() : "Administración";
                            String salonInfo = jsonObj.has("seccion") ? jsonObj.get("seccion").getAsString() : null;
                            String fechaPublicacion = jsonObj.has("fecha_publicacion") ? jsonObj.get("fecha_publicacion").getAsString() : "";
                            String destinatarioTipo = jsonObj.has("destinatario_tipo") ? 
                                jsonObj.get("destinatario_tipo").getAsString() : "";
                            
                            destinatarioTipo = destinatarioTipo.toUpperCase().trim();
                            boolean esGlobal = "GLOBAL".equals(destinatarioTipo);
                            
                            Comunicado comunicado = new Comunicado(titulo, contenido, null, salonInfo, emisor);
                            comunicado.fechaPublicacion = fechaPublicacion;
                            
                            if (esGlobal) {
                                globales.add(comunicado);
                            } else {
                                if (esAlumno) {
                                    // Para alumnos/estudiantes: comunicados de SU salón
                                    // Si no tiene sección definida, agregar todos los de salón
                                    // Normalizar espacios antes de comparar
                                    if (miSeccion.isEmpty() || (salonInfo != null && 
                                        salonInfo.replaceAll("\\s+", "").equalsIgnoreCase(miSeccion.replaceAll("\\s+", "")))) {
                                        miSalon.add(comunicado);
                                    }
                                } else {
                                    otrosSalones.add(comunicado);
                                }
                            }
                        }
                        
                        // Ordenar comunicados del más reciente al más antiguo
                        globales.sort((c1, c2) -> {
                            if (c1.fechaPublicacion == null) return 1;
                            if (c2.fechaPublicacion == null) return -1;
                            return c2.fechaPublicacion.compareTo(c1.fechaPublicacion);
                        });
                        
                        miSalon.sort((c1, c2) -> {
                            if (c1.fechaPublicacion == null) return 1;
                            if (c2.fechaPublicacion == null) return -1;
                            return c2.fechaPublicacion.compareTo(c1.fechaPublicacion);
                        });
                        
                        otrosSalones.sort((c1, c2) -> {
                            if (c1.fechaPublicacion == null) return 1;
                            if (c2.fechaPublicacion == null) return -1;
                            return c2.fechaPublicacion.compareTo(c1.fechaPublicacion);
                        });
                        
                        // Ocultar el mensaje general de "no hay comunicados"
                        layoutNoComunicados.setVisibility(View.GONE);
                        
                        // SECCIÓN 1: Comunicados Globales
                        if (globales.isEmpty()) {
                            layoutNoComunicadosGlobales.setVisibility(View.VISIBLE);
                            cardComunicadoGlobal.setVisibility(View.GONE);
                        } else {
                            layoutNoComunicadosGlobales.setVisibility(View.GONE);
                            cardComunicadoGlobal.setVisibility(View.VISIBLE);
                            llenarCardComunicado(cardComunicadoGlobal, globales.get(0));
                        }
                        
                        // SECCIÓN 2: Comunicados de mi Salón (solo para ALUMNO/ESTUDIANTE)
                        if (esAlumno) {
                            headerComunicadosSalon.setVisibility(View.VISIBLE);
                            SharedPreferences up = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            String sec = up.getString("user_seccion", "");
                            tvTitleSalon.setText((sec != null && !sec.isEmpty())
                                ? "Comunicados de mi Salón (" + sec + ")"
                                : "Comunicados de mi Salón");
                            if (miSalon.isEmpty()) {
                                layoutNoComunicadosSalon.setVisibility(View.VISIBLE);
                                cardComunicadoSalon.setVisibility(View.GONE);
                            } else {
                                layoutNoComunicadosSalon.setVisibility(View.GONE);
                                cardComunicadoSalon.setVisibility(View.VISIBLE);
                                llenarCardComunicado(cardComunicadoSalon, miSalon.get(0));
                                cardComunicadoSalon.setOnClickListener(v -> mostrarModalComunicado(miSalon.get(0)));
                            }
                        } else {
                            headerComunicadosSalon.setVisibility(View.GONE);
                            layoutNoComunicadosSalon.setVisibility(View.GONE);
                            cardComunicadoSalon.setVisibility(View.GONE);
                        }

                        // SECCIÓN 3: Comunicados de Salones (solo para PROFESOR/ADMIN)
                        if (esProfesor || esAdmin) {
                            headerComunicadosSalones.setVisibility(View.VISIBLE);
                            if (otrosSalones.isEmpty()) {
                                layoutNoComunicadosSalones.setVisibility(View.VISIBLE);
                                cardComunicadoSalones.setVisibility(View.GONE);
                            } else {
                                layoutNoComunicadosSalones.setVisibility(View.GONE);
                                cardComunicadoSalones.setVisibility(View.VISIBLE);
                                llenarCardComunicado(cardComunicadoSalones, otrosSalones.get(0));
                                cardComunicadoSalones.setOnClickListener(v -> mostrarModalComunicado(otrosSalones.get(0)));
                            }
                        } else {
                            headerComunicadosSalones.setVisibility(View.GONE);
                            layoutNoComunicadosSalones.setVisibility(View.GONE);
                            cardComunicadoSalones.setVisibility(View.GONE);
                        }

                        // Global card también clickeable
                        if (!globales.isEmpty()) {
                            cardComunicadoGlobal.setOnClickListener(v -> mostrarModalComunicado(globales.get(0)));
                        }
                        
                        // Marcar como cargados
                        comunicadosCargados = true;
                        // Guardar timestamp de la última carga exitosa
                        getSharedPreferences("cache_prefs", MODE_PRIVATE)
                            .edit()
                            .putLong("comunicados_time", System.currentTimeMillis())
                            .apply();
                        
                    } else {
                        // Error en la respuesta - mostrar mensaje general
                        layoutNoComunicados.setVisibility(View.VISIBLE);
                        layoutNoComunicadosGlobales.setVisibility(View.GONE);
                        layoutNoComunicadosSalon.setVisibility(View.GONE);
                        layoutNoComunicadosSalones.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    android.widget.Toast.makeText(HomeActivity.this, "Error al cargar comunicados: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    layoutNoComunicados.setVisibility(View.VISIBLE);
                    layoutNoComunicadosGlobales.setVisibility(View.GONE);
                    layoutNoComunicadosSalon.setVisibility(View.GONE);
                    layoutNoComunicadosSalones.setVisibility(View.GONE);
                }
            });
    }
    
    private void llenarCardComunicado(View cardView, Comunicado comunicado) {
        // Configurar emisor
        TextView tvEmisor = cardView.findViewById(R.id.tv_emisor_comunicado);
        if (tvEmisor != null) {
            tvEmisor.setText(comunicado.emisor != null ? comunicado.emisor : "Administración");
        }
        
        // Configurar título
        ((TextView) cardView.findViewById(R.id.tv_titulo_comunicado)).setText(comunicado.titulo);
        
        // Configurar hora del comunicado
        TextView tvHora = cardView.findViewById(R.id.tv_hora_comunicado);
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
        TextView tvContenido = cardView.findViewById(R.id.tv_contenido_comunicado);
        TextView btnVerMas = cardView.findViewById(R.id.btn_ver_mas_comunicado);
        
        tvContenido.setText(comunicado.contenido);
        tvContenido.setVisibility(View.VISIBLE);

        // Si el texto es largo (más de 150 caracteres), mostrar "Ver más"
        if (comunicado.contenido != null && comunicado.contenido.length() > 150) {
            btnVerMas.setVisibility(View.VISIBLE);
        } else {
            btnVerMas.setVisibility(View.GONE);
        }
        
        // Configurar tag de salón
        TextView tvTagSalon = cardView.findViewById(R.id.tv_tag_salon);
        if (comunicado.salon != null && !comunicado.salon.isEmpty()) {
            tvTagSalon.setText("Salón: " + comunicado.salon);
            tvTagSalon.setVisibility(View.VISIBLE);
        } else {
            tvTagSalon.setVisibility(View.GONE);
        }
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
        View modalView = LayoutInflater.from(this).inflate(R.layout.dialog_comunicado_detalle, null);

        // Título
        ((TextView) modalView.findViewById(R.id.tv_titulo_detalle)).setText(comunicado.titulo);

        // Contenido completo
        ((TextView) modalView.findViewById(R.id.tv_contenido_detalle)).setText(comunicado.contenido);

        // Fecha formateada
        TextView tvFecha = modalView.findViewById(R.id.tv_fecha_detalle);
        if (comunicado.fechaPublicacion != null && !comunicado.fechaPublicacion.isEmpty()) {
            tvFecha.setText(obtenerFechaFormateada(comunicado.fechaPublicacion));
            tvFecha.setVisibility(View.VISIBLE);
        } else {
            tvFecha.setVisibility(View.GONE);
        }

        // Hora
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
                    tvHoraDetalle.setVisibility(View.VISIBLE);
                } else {
                    tvHoraDetalle.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                tvHoraDetalle.setVisibility(View.GONE);
            }
        } else {
            tvHoraDetalle.setVisibility(View.GONE);
        }

        // Tag salón
        TextView tvTagDetalle = modalView.findViewById(R.id.tv_tag_detalle);
        if (comunicado.salon != null && !comunicado.salon.isEmpty()) {
            tvTagDetalle.setText("Salón: " + comunicado.salon);
            tvTagDetalle.setVisibility(View.VISIBLE);
        } else if (comunicado.emisor != null && !comunicado.emisor.isEmpty()) {
            tvTagDetalle.setText("Enviado por: " + comunicado.emisor);
            tvTagDetalle.setVisibility(View.VISIBLE);
        } else {
            tvTagDetalle.setVisibility(View.GONE);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setView(modalView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        // Se cierra al presionar fuera
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String obtenerFechaFormateada(String fechaPublicacion) {
        if (fechaPublicacion == null || fechaPublicacion.isEmpty()) return "";
        try {
            // Parsear fecha ISO 8601 con zona horaria UTC
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            // Remover la 'Z' y milisegundos si existen
            String fechaLimpia = fechaPublicacion.replace("Z", "").split("\\.")[0];
            Date fechaUTC = isoFormat.parse(fechaLimpia);
            
            // Convertir a zona horaria de Perú (UTC-5)
            Calendar fechaCom = Calendar.getInstance();
            fechaCom.setTime(fechaUTC);
            fechaCom.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            
            Calendar hoy = Calendar.getInstance();
            hoy.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            
            Calendar ayer = Calendar.getInstance();
            ayer.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            ayer.add(Calendar.DAY_OF_YEAR, -1);
            
            int dia = fechaCom.get(Calendar.DAY_OF_MONTH);
            int mes = fechaCom.get(Calendar.MONTH);
            
            String[] meses = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                              "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
            if (fechaCom.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                fechaCom.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)) {
                return "Hoy, " + dia + " de " + meses[mes];
            } else if (fechaCom.get(Calendar.YEAR) == ayer.get(Calendar.YEAR) &&
                       fechaCom.get(Calendar.DAY_OF_YEAR) == ayer.get(Calendar.DAY_OF_YEAR)) {
                return "Ayer, " + dia + " de " + meses[mes];
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
                String r = sdf.format(fechaCom.getTime());
                return r.substring(0, 1).toUpperCase() + r.substring(1);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Error parseando fecha: " + fechaPublicacion, e);
            return "";
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
    
    // Función auxiliar para convertir hora de 24h a formato 12h AM/PM
    private String convertirHoraAMPM(String hora24) {
        if (hora24 == null || hora24.isEmpty()) {
            return "";
        }
        
        try {
            // Formato esperado: "14:30:00" o "14:30"
            String[] partes = hora24.split(":");
            int hora = Integer.parseInt(partes[0]);
            int minuto = Integer.parseInt(partes[1]);
            
            String ampm = hora >= 12 ? "PM" : "AM";
            if (hora > 12) hora -= 12;
            if (hora == 0) hora = 12;
            
            return String.format("%d:%02d %s", hora, minuto, ampm);
        } catch (Exception e) {
            return hora24; // Si falla, devolver la hora original
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
                        guardarAsistenciaCache(rachaActual, lunes, martes, miercoles, jueves, viernes);
                        
                    } else {
                        android.util.Log.w("HomeActivity", "Error al cargar días asistidos: " + response.message());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                    android.util.Log.e("HomeActivity", "Error al cargar días asistidos: " + t.getMessage());
                    // Resetear valores por defecto si falla la conexión
                    actualizarUIAsistencia(0, false, false, false, false, false);
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
    
    private void cargarHorarioActual() {
        // Obtener fecha de hoy
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaHoy = sdf.format(new Date());
        
        // Obtener user_id de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            android.util.Log.w("HomeActivity", "No se encontró user_id para cargar horario");
            mostrarSinActividad();
            return;
        }
        
        // Llamar al backend según el rol
        retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call;
        
        if ("PROFESOR".equals(userMode)) {
            call = com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
                .getHorariosProfesor(userId, fechaHoy);
        } else {
            call = com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
                .getHorariosAlumno(userId, fechaHoy);
        }
        
        call.enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call,
                                 retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Object> horariosData = response.body().getData();
                    
                    if (!horariosData.isEmpty()) {
                        ultimosHorarios = horariosData;
                        // Encontrar el curso actual según la hora
                        Object cursoActual = encontrarCursoActual(horariosData);
                        if (cursoActual != null) {
                            actualizarUIHorario(cursoActual);
                        } else {
                            mostrarSinActividad();
                        }
                        programarProximaActualizacion();
                        guardarHorarioCache(horariosData);
                    } else {
                        mostrarSinActividad();
                    }
                } else {
                    mostrarSinActividad();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                android.util.Log.e("HomeActivity", "Error al cargar horario: " + t.getMessage());
                mostrarSinActividad();
            }
        });
    }
    
    private Object encontrarCursoActual(List<Object> horarios) {
        // Obtener hora actual
        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);
        int tiempoActualEnMinutos = horaActual * 60 + minutoActual;
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        
        // Buscar el curso que está en progreso AHORA
        for (Object obj : horarios) {
            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
            
            String horaInicio = jsonObj.has("hora_inicio") ? jsonObj.get("hora_inicio").getAsString() : "";
            String horaFin = jsonObj.has("hora_fin") ? jsonObj.get("hora_fin").getAsString() : "";
            String curso = jsonObj.has("curso") ? jsonObj.get("curso").getAsString() : "";
            
            // Saltar recreos
            if ("RECREO".equals(curso)) {
                continue;
            }
            
            try {
                // Parsear hora inicio (formato: "08:00" o "08:00:00")
                String[] partesInicio = horaInicio.split(":");
                int horaIni = Integer.parseInt(partesInicio[0]);
                int minIni = Integer.parseInt(partesInicio[1]);
                int tiempoInicioEnMinutos = horaIni * 60 + minIni;
                
                // Parsear hora fin
                String[] partesFin = horaFin.split(":");
                int horaF = Integer.parseInt(partesFin[0]);
                int minF = Integer.parseInt(partesFin[1]);
                int tiempoFinEnMinutos = horaF * 60 + minF;
                
                // Verificar si estamos dentro del rango de este curso
                if (tiempoActualEnMinutos >= tiempoInicioEnMinutos && tiempoActualEnMinutos < tiempoFinEnMinutos) {
                    return obj; // Este es el curso actual
                }
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error parseando horas: " + e.getMessage());
            }
        }
        
        // Si no hay curso en progreso, devolver el próximo curso
        for (Object obj : horarios) {
            com.google.gson.Gson gson2 = new com.google.gson.Gson();
            com.google.gson.JsonObject jsonObj = gson2.toJsonTree(obj).getAsJsonObject();
            
            String horaInicio = jsonObj.has("hora_inicio") ? jsonObj.get("hora_inicio").getAsString() : "";
            String curso = jsonObj.has("curso") ? jsonObj.get("curso").getAsString() : "";
            
            if ("RECREO".equals(curso)) {
                continue;
            }
            
            try {
                String[] partesInicio = horaInicio.split(":");
                int horaIni = Integer.parseInt(partesInicio[0]);
                int minIni = Integer.parseInt(partesInicio[1]);
                int tiempoInicioEnMinutos = horaIni * 60 + minIni;
                
                if (tiempoActualEnMinutos < tiempoInicioEnMinutos) {
                    return obj; // Este es el próximo curso
                }
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error parseando horas: " + e.getMessage());
            }
        }
        
        // Si no hay próximo curso, todos ya terminaron
        return null;
    }
    private void actualizarUIHorario(Object cursoData) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject jsonObj = gson.toJsonTree(cursoData).getAsJsonObject();
        
        String curso = jsonObj.has("curso") ? jsonObj.get("curso").getAsString() : "";
        String horaInicio = jsonObj.has("hora_inicio") ? jsonObj.get("hora_inicio").getAsString() : "";
        String horaFin = jsonObj.has("hora_fin") ? jsonObj.get("hora_fin").getAsString() : "";
        String profesor = jsonObj.has("profesor") ? jsonObj.get("profesor").getAsString() : "";
        String aula = jsonObj.has("aula") ? jsonObj.get("aula").getAsString() : "";
        String seccion = jsonObj.has("seccion") ? jsonObj.get("seccion").getAsString() : "";
        
        // Actualizar fecha (día de hoy)
        TextView tvFechaAgenda = findViewById(R.id.tv_fecha_agenda);
        if (tvFechaAgenda != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM, yyyy", new Locale("es", "ES"));
            String fechaFormateada = sdf.format(new Date());
            // Capitalizar primera letra
            fechaFormateada = fechaFormateada.substring(0, 1).toUpperCase() + fechaFormateada.substring(1);
            tvFechaAgenda.setText(fechaFormateada);
        }
        
        // Actualizar hora inicio con formato AM/PM
        TextView tvHoraInicioAgenda = findViewById(R.id.tv_hora_inicio_agenda);
        if (tvHoraInicioAgenda != null) {
            tvHoraInicioAgenda.setText(convertirHoraAMPM(horaInicio));
            tvHoraInicioAgenda.setVisibility(View.VISIBLE);
        }
        
        // Actualizar hora fin con formato AM/PM
        TextView tvHoraFinAgenda = findViewById(R.id.tv_hora_fin_agenda);
        if (tvHoraFinAgenda != null) {
            tvHoraFinAgenda.setText(convertirHoraAMPM(horaFin));
            tvHoraFinAgenda.setVisibility(View.VISIBLE);
        }
        
        // Actualizar nombre del curso
        TextView tvCursoAgenda = findViewById(R.id.tv_curso_agenda);
        if (tvCursoAgenda != null) {
            tvCursoAgenda.setText(curso);
            tvCursoAgenda.setVisibility(View.VISIBLE);
        }
        
        // Actualizar salón/sección
        TextView tvSalonAgenda = findViewById(R.id.tv_salon_agenda);
        if (tvSalonAgenda != null) {
            if ("PROFESOR".equals(userMode)) {
                tvSalonAgenda.setText(aula + " - " + seccion);
            } else {
                tvSalonAgenda.setText(aula);
            }
            tvSalonAgenda.setVisibility(View.VISIBLE);
        }
        
        // Actualizar profesor
        TextView tvProfesorAgenda = findViewById(R.id.tv_profesor_agenda);
        if (tvProfesorAgenda != null) {
            if ("PROFESOR".equals(userMode)) {
                tvProfesorAgenda.setVisibility(View.GONE); // Profesores no ven su propio nombre
            } else {
                tvProfesorAgenda.setText(profesor);
                tvProfesorAgenda.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Mostrar mensaje cuando no hay actividades programadas
     */
    private void mostrarSinActividad() {
        // Actualizar fecha (día de hoy)
        TextView tvFechaAgenda = findViewById(R.id.tv_fecha_agenda);
        if (tvFechaAgenda != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM, yyyy", new Locale("es", "ES"));
            String fechaFormateada = sdf.format(new Date());
            // Capitalizar primera letra
            fechaFormateada = fechaFormateada.substring(0, 1).toUpperCase() + fechaFormateada.substring(1);
            tvFechaAgenda.setText(fechaFormateada);
        }
        
        // Ocultar la columna de horas (izquierda)
        TextView tvHoraInicio = findViewById(R.id.tv_hora_inicio_agenda);
        if (tvHoraInicio != null) {
            android.view.ViewParent parent = tvHoraInicio.getParent();
            if (parent instanceof android.view.ViewGroup) {
                android.view.ViewParent grandParent = parent.getParent();
                if (grandParent instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) grandParent).setVisibility(View.GONE);
                }
            }
        }
        
        // Ocultar la línea divisoria vertical
        View divider = findViewById(R.id.divider_agenda);
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }
        
        // Buscar el LinearLayout horizontal principal y agregar padding
        TextView tvCursoAgenda = findViewById(R.id.tv_curso_agenda);
        if (tvCursoAgenda != null) {
            android.view.ViewParent parent = tvCursoAgenda.getParent();
            if (parent instanceof android.view.ViewGroup) {
                android.view.ViewParent grandParent = parent.getParent();
                if (grandParent instanceof android.widget.LinearLayout) {
                    android.widget.LinearLayout layoutHorizontal = (android.widget.LinearLayout) grandParent;
                    // Agregar padding para mantener altura del card
                    layoutHorizontal.setPadding(
                        layoutHorizontal.getPaddingLeft(),
                        (int) (40 * getResources().getDisplayMetrics().density),
                        layoutHorizontal.getPaddingRight(),
                        (int) (40 * getResources().getDisplayMetrics().density)
                    );
                }
            }
            
            // Mostrar mensaje centrado en gris
            tvCursoAgenda.setText("Ninguna actividad programada");
            tvCursoAgenda.setTextColor(0xFF9E9E9E); // Gris
            tvCursoAgenda.setGravity(android.view.Gravity.CENTER);
            tvCursoAgenda.setVisibility(View.VISIBLE);
            
            // Centrar el contenedor del texto
            if (tvCursoAgenda.getParent() instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout parentLayout = (android.widget.LinearLayout) tvCursoAgenda.getParent();
                parentLayout.setGravity(android.view.Gravity.CENTER);
            }
        }
        
        // Ocultar los LinearLayouts con iconos (salón y profesor)
        TextView tvSalonAgenda = findViewById(R.id.tv_salon_agenda);
        if (tvSalonAgenda != null && tvSalonAgenda.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) tvSalonAgenda.getParent()).setVisibility(View.GONE);
        }
        
        TextView tvProfesorAgenda = findViewById(R.id.tv_profesor_agenda);
        if (tvProfesorAgenda != null && tvProfesorAgenda.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) tvProfesorAgenda.getParent()).setVisibility(View.GONE);
        }
    }

    private void programarProximaActualizacion() {
        if (horarioRunnable != null) {
            horarioHandler.removeCallbacks(horarioRunnable);
        }
        horarioRunnable = () -> {
            if (ultimosHorarios != null && !ultimosHorarios.isEmpty()) {
                Object cursoActual = encontrarCursoActual(ultimosHorarios);
                if (cursoActual != null) {
                    actualizarUIHorario(cursoActual);
                } else {
                    mostrarSinActividad();
                }
                programarProximaActualizacion();
            }
        };

        long msParaProximoEvento = calcularMsHastaProximoEvento();
        if (msParaProximoEvento > 0) {
            horarioHandler.postDelayed(horarioRunnable, msParaProximoEvento);
        }
    }

    private long calcularMsHastaProximoEvento() {
        if (ultimosHorarios == null || ultimosHorarios.isEmpty()) {
            return 300000;
        }

        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);
        int segundoActual = ahora.get(Calendar.SECOND);
        int tiempoActualEnMinutos = horaActual * 60 + minutoActual;

        com.google.gson.Gson gson = new com.google.gson.Gson();
        long menorMs = Long.MAX_VALUE;

        for (Object obj : ultimosHorarios) {
            com.google.gson.JsonObject jsonObj = gson.toJsonTree(obj).getAsJsonObject();
            String horaInicio = jsonObj.has("hora_inicio") ? jsonObj.get("hora_inicio").getAsString() : "";
            String horaFin = jsonObj.has("hora_fin") ? jsonObj.get("hora_fin").getAsString() : "";

            try {
                String[] partesInicio = horaInicio.split(":");
                int horaIni = Integer.parseInt(partesInicio[0]);
                int minIni = Integer.parseInt(partesInicio[1]);
                int tiempoInicio = horaIni * 60 + minIni;

                String[] partesFin = horaFin.split(":");
                int horaF = Integer.parseInt(partesFin[0]);
                int minF = Integer.parseInt(partesFin[1]);
                int tiempoFin = horaF * 60 + minF;

                if (tiempoActualEnMinutos >= tiempoInicio && tiempoActualEnMinutos < tiempoFin) {
                    int msHastaFin = ((tiempoFin - tiempoActualEnMinutos) * 60 - segundoActual) * 1000;
                    if (msHastaFin > 0 && msHastaFin < menorMs) {
                        menorMs = msHastaFin;
                    }
                } else if (tiempoActualEnMinutos < tiempoInicio) {
                    int msHastaInicio = ((tiempoInicio - tiempoActualEnMinutos) * 60 - segundoActual) * 1000;
                    if (msHastaInicio > 0 && msHastaInicio < menorMs) {
                        menorMs = msHastaInicio;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (menorMs == Long.MAX_VALUE) {
            return 300000; // Todos los cursos terminaron, revisar en 5 min
        }
        return Math.max(menorMs, 1000);
    }

    private void guardarHorarioCache(List<Object> horarios) {
        SharedPreferences prefs = getSharedPreferences("cache_prefs", MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(horarios);
        prefs.edit()
            .putString("horario_data", json)
            .putLong("horario_time", System.currentTimeMillis())
            .apply();
    }

    private void cargarHorarioDesdeCache() {
        SharedPreferences prefs = getSharedPreferences("cache_prefs", MODE_PRIVATE);
        String json = prefs.getString("horario_data", null);
        if (json == null) return;

        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Object>>(){}.getType();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        List<Object> horarios = gson.fromJson(json, type);
        if (horarios == null || horarios.isEmpty()) return;

        ultimosHorarios = horarios;
        Object cursoActual = encontrarCursoActual(horarios);
        if (cursoActual != null) {
            actualizarUIHorario(cursoActual);
        } else {
            mostrarSinActividad();
        }
        programarProximaActualizacion();
    }

    private void guardarAsistenciaCache(int racha, boolean lunes, boolean martes, boolean miercoles, boolean jueves, boolean viernes) {
        SharedPreferences prefs = getSharedPreferences("cache_prefs", MODE_PRIVATE);
        prefs.edit()
            .putInt("asistencia_racha", racha)
            .putBoolean("asistencia_lunes", lunes)
            .putBoolean("asistencia_martes", martes)
            .putBoolean("asistencia_miercoles", miercoles)
            .putBoolean("asistencia_jueves", jueves)
            .putBoolean("asistencia_viernes", viernes)
            .putLong("asistencia_time", System.currentTimeMillis())
            .apply();
    }

    private void cargarAsistenciaDesdeCache() {
        SharedPreferences prefs = getSharedPreferences("cache_prefs", MODE_PRIVATE);
        if (!prefs.contains("asistencia_racha")) return;
        int racha = prefs.getInt("asistencia_racha", 0);
        boolean lunes = prefs.getBoolean("asistencia_lunes", false);
        boolean martes = prefs.getBoolean("asistencia_martes", false);
        boolean miercoles = prefs.getBoolean("asistencia_miercoles", false);
        boolean jueves = prefs.getBoolean("asistencia_jueves", false);
        boolean viernes = prefs.getBoolean("asistencia_viernes", false);
        actualizarUIAsistencia(racha, lunes, martes, miercoles, jueves, viernes);
    }

    private boolean comunicadosCacheExpirados() {
        SharedPreferences prefs = getSharedPreferences("cache_prefs", MODE_PRIVATE);
        long time = prefs.getLong("comunicados_time", 0);
        return System.currentTimeMillis() - time >= CACHE_TTL_MS;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (horarioHandler != null && horarioRunnable != null) {
            horarioHandler.removeCallbacks(horarioRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (horarioHandler != null && horarioRunnable != null) {
            horarioHandler.removeCallbacks(horarioRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Cargar datos cacheados al instante antes de cualquier API
        cargarHorarioDesdeCache();
        cargarAsistenciaDesdeCache();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        // Actualizar rol del usuario por si cambió
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = prefs.getString("user_mode", "ALUMNO");
        
        // Actualizar visibilidad del menú y datos del usuario
        actualizarVisibilidadMenuLateral();
        actualizarDatosUsuario();
        
        // Recargar horario y asistencia para actualizar si cambió mientras estaba fuera
        if (!esPrimeraVez) {
            cargarHorarioActual();
            cargarDiasAsistidos();
        }
        
        // Solo recargar comunicados si NO es la primera vez y pasó suficiente tiempo
        if (!esPrimeraVez) {
            if (comunicadosCacheExpirados()) {
                comunicadosCargados = false;
                cargarComunicados();
            }
        } else {
            esPrimeraVez = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        NotificationPermissionHelper.handlePermissionResult(requestCode, permissions, grantResults);
    }
}
