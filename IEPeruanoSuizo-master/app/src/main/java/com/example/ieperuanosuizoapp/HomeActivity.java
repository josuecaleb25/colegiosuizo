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

    private TextView tvCelebration, tvDayNumber, tvDayName, tvMonthYear, tvGreeting;
    private ImageView ivDailyIllustration, ivMenuIcon;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View mainContent;
    
    // Vistas de Comunicados
    private LinearLayout containerGlobales, containerSalon, layoutNoComunicados;
    private TextView tvTitleSalon;
    private String userMode;

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
        tvTitleSalon = findViewById(R.id.tv_title_salon);

        findViewById(R.id.btn_menu).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
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
            } else if (id == R.id.nav_modo_usuario) {
                mostrarDialogoModoUsuario();
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
        ivDailyIllustration = findViewById(R.id.iv_daily_illustration);

        updateDailyInfo();

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

    private void setupComunicadosLogic() {
        if ("PROFESOR".equals(userMode)) {
            tvTitleSalon.setText("Comunicados de mis Salones");
            tvTitleSalon.setVisibility(View.VISIBLE);
        } else {
            tvTitleSalon.setText("Comunicados de mi Salón (4to A)");
            tvTitleSalon.setVisibility(View.VISIBLE);
        }

        cargarComunicadosSimulados();
        actualizarVisibilidadMenuLateral();
    }

    private void actualizarVisibilidadMenuLateral() {
        android.view.Menu menu = navigationView.getMenu();
        
        // Items base (Siempre visibles para todos)
        menu.findItem(R.id.nav_home).setVisible(true);
        menu.findItem(R.id.nav_cursos).setVisible(true);
        menu.findItem(R.id.nav_horarios).setVisible(true);
        menu.findItem(R.id.nav_perfil).setVisible(true);
        menu.findItem(R.id.nav_identificacion).setVisible(true);

        // Lógica de visibilidad por Rol
        if ("ALUMNO".equals(userMode)) {
            menu.findItem(R.id.nav_cursos).setVisible(true);
            menu.findItem(R.id.nav_horarios).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(false);
            menu.findItem(R.id.nav_asistencia).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);
            
        } else if ("PROFESOR".equals(userMode)) {
            menu.findItem(R.id.nav_cursos).setVisible(true);
            menu.findItem(R.id.nav_horarios).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(true);
            menu.findItem(R.id.nav_asistencia).setVisible(false);
            menu.findItem(R.id.nav_panel_admin).setVisible(false);
            
        } else if ("AUXILIAR".equals(userMode)) {
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

        } else if ("ADMIN".equals(userMode)) {
            menu.findItem(R.id.nav_home).setVisible(true);
            menu.findItem(R.id.nav_gestion_comunicados).setVisible(true);
            menu.findItem(R.id.nav_perfil).setVisible(true);
            menu.findItem(R.id.nav_identificacion).setVisible(true);
            menu.findItem(R.id.nav_asistencia).setVisible(true);
            menu.findItem(R.id.nav_panel_admin).setVisible(true);
            
            // Ocultar Cursos y Horarios para el Admin
            menu.findItem(R.id.nav_cursos).setVisible(false);
            menu.findItem(R.id.nav_horarios).setVisible(false);
        }
        
        // Mantenemos Cambiar Rol visible para pruebas
        menu.findItem(R.id.nav_modo_usuario).setVisible(true);
    }

    private void cargarComunicadosSimulados() {
        containerGlobales.removeAllViews();
        containerSalon.removeAllViews();

        // Cargar comunicados desde el backend
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            layoutNoComunicados.setVisibility(View.VISIBLE);
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
                            String tipo = jsonObj.has("tipo") ? jsonObj.get("tipo").getAsString() : "GLOBAL";
                            String emisor = jsonObj.has("emisor") ? jsonObj.get("emisor").getAsString() : "Administración";
                            
                            Comunicado comunicado = new Comunicado(titulo, contenido, null, tipo, emisor);
                            
                            if ("GLOBAL".equals(tipo)) {
                                globales.add(comunicado);
                            } else {
                                salon.add(comunicado);
                            }
                        }
                        
                        if (globales.isEmpty() && salon.isEmpty()) {
                            layoutNoComunicados.setVisibility(View.VISIBLE);
                        } else {
                            layoutNoComunicados.setVisibility(View.GONE);
                            for (Comunicado c : globales) containerGlobales.addView(crearTarjetaComunicado(c, containerGlobales));
                            for (Comunicado c : salon) containerSalon.addView(crearTarjetaComunicado(c, containerSalon));
                        }
                    } else {
                        layoutNoComunicados.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    android.widget.Toast.makeText(HomeActivity.this, "Error al cargar comunicados: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    layoutNoComunicados.setVisibility(View.VISIBLE);
                }
            });
    }

    private View crearTarjetaComunicado(Comunicado comunicado, android.view.ViewGroup parent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_comunicado, parent, false);
        
        // Configurar título
        ((TextView) view.findViewById(R.id.tv_titulo_comunicado)).setText(comunicado.titulo);
        
        // Configurar fecha y destinatario
        TextView tvFechaDestinatario = view.findViewById(R.id.tv_fecha_destinatario);
        tvFechaDestinatario.setText("Enviado por: " + comunicado.emisor);
        tvFechaDestinatario.setVisibility(View.VISIBLE);
        
        // Configurar contenido
        TextView tvContenido = view.findViewById(R.id.tv_contenido_comunicado);
        TextView btnVerMas = view.findViewById(R.id.btn_ver_mas_comunicado);
        
        tvContenido.setText(comunicado.contenido);
        tvContenido.setVisibility(View.VISIBLE);

        // Si el texto es largo, mostrar "Ver más"
        if (comunicado.contenido != null && comunicado.contenido.length() > 150) {
            btnVerMas.setVisibility(View.VISIBLE);
        } else {
            btnVerMas.setVisibility(View.GONE);
        }
        
        // Ocultar estado en la vista de Home
        TextView tvEstado = view.findViewById(R.id.tv_estado);
        tvEstado.setVisibility(View.GONE);
        
        // Ocultar botón de opciones en Home
        ImageView btnOpciones = view.findViewById(R.id.btn_opciones);
        btnOpciones.setVisibility(View.GONE);

        // Al hacer clic, abrir modal
        view.setOnClickListener(v -> mostrarModalComunicado(comunicado));

        return view;
    }

    private void mostrarModalComunicado(Comunicado comunicado) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View modalView = LayoutInflater.from(this).inflate(R.layout.item_comunicado, null);
        
        androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) modalView;
        card.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Forzar fondo blanco para el modal
        card.setCardBackgroundColor(Color.WHITE);
        
        android.view.ViewGroup.MarginLayoutParams cardParams = (android.view.ViewGroup.MarginLayoutParams) card.getLayoutParams();
        cardParams.setMargins(0, 0, 0, 0);
        card.setLayoutParams(cardParams);

        // Configurar contenido
        ((TextView) modalView.findViewById(R.id.tv_titulo_comunicado)).setText(comunicado.titulo);
        
        TextView tvFechaDestinatario = modalView.findViewById(R.id.tv_fecha_destinatario);
        tvFechaDestinatario.setText("Enviado por: " + comunicado.emisor);
        tvFechaDestinatario.setVisibility(View.VISIBLE);
        
        TextView tvContenido = modalView.findViewById(R.id.tv_contenido_comunicado);
        tvContenido.setText(comunicado.contenido);
        tvContenido.setMaxLines(100);
        tvContenido.setVisibility(View.VISIBLE);
        
        modalView.findViewById(R.id.btn_ver_mas_comunicado).setVisibility(View.GONE);
        modalView.findViewById(R.id.tv_estado).setVisibility(View.GONE);
        modalView.findViewById(R.id.btn_opciones).setVisibility(View.GONE);

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
        String titulo, contenido, salon, emisor;
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

    private void mostrarDialogoModoUsuario() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String modoActual = prefs.getString("user_mode", "ALUMNO");

        String[] modos = {"Alumno", "Profesor", "Auxiliar", "Administrador"};
        int seleccionActual = modoActual.equals("PROFESOR") ? 1 : 
                             modoActual.equals("AUXILIAR") ? 2 :
                             modoActual.equals("ADMIN") ? 3 : 0;

        new android.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar Modo de Usuario")
                .setSingleChoiceItems(modos, seleccionActual, (dialog, which) -> {
                    String nuevoModo = which == 1 ? "PROFESOR" : 
                                      which == 2 ? "AUXILIAR" :
                                      which == 3 ? "ADMIN" : "ALUMNO";
                    
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_mode", nuevoModo);
                    editor.apply();

                    // Actualizar el título del menú
                    actualizarTituloModoUsuario(nuevoModo);

                    // Mostrar mensaje
                    String nombreModo = modos[which];
                    android.widget.Toast.makeText(this, "Modo cambiado a: " + nombreModo, android.widget.Toast.LENGTH_SHORT).show();

                    dialog.dismiss();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    
                    // Forzar refresco de interfaz
                    userMode = nuevoModo;
                    actualizarVisibilidadMenuLateral();
                    setupComunicadosLogic();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                    drawerLayout.closeDrawer(GravityCompat.START);
                })
                .show();
    }

    private void actualizarTituloModoUsuario(String modo) {
        String titulo = "Modo: " + modo.substring(0, 1).toUpperCase() + modo.substring(1).toLowerCase();
        navigationView.getMenu().findItem(R.id.nav_modo_usuario).setTitle(titulo);
        
        // Mostrar/ocultar Panel de Administración según el modo
        navigationView.getMenu().findItem(R.id.nav_panel_admin).setVisible("ADMIN".equals(modo));
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        // Actualizar el título del modo y comunicados al volver a la actividad
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = prefs.getString("user_mode", "ALUMNO");
        actualizarTituloModoUsuario(userMode);
        setupComunicadosLogic();
    }
}
