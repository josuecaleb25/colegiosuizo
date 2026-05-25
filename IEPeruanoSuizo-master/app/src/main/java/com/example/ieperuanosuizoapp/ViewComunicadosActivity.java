package com.example.ieperuanosuizoapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ViewComunicadosActivity extends AppCompatActivity {

    private LinearLayout containerComunicados;
    private String userMode;
    private String tipoFiltro; // GLOBAL, MI_SALON, SALONES

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        int colorScheme = themePrefs.getInt("colorScheme", 0);

        if (isDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (colorScheme == 2) setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_comunicados);

        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = userPrefs.getString("user_mode", "ALUMNO");

        // Obtener el tipo de filtro desde el Intent
        tipoFiltro = getIntent().getStringExtra("tipo");
        if (tipoFiltro == null) tipoFiltro = "TODOS";

        containerComunicados = findViewById(R.id.container_comunicados);

        // Actualizar título en el header según el filtro
        TextView tvTitle = findViewById(R.id.tv_title);
        if ("GLOBAL".equals(tipoFiltro)) {
            tvTitle.setText("Comunicados");
        } else if ("MI_SALON".equals(tipoFiltro)) {
            String seccion = userPrefs.getString("user_seccion", "");
            android.util.Log.d("ViewComunicados", "Sección del usuario: '" + seccion + "'");
            android.util.Log.d("ViewComunicados", "Todas las preferencias: " + userPrefs.getAll().toString());
            if (seccion != null && !seccion.isEmpty()) {
                tvTitle.setText("Comunicados - " + seccion);
            } else {
                // Fallback: intentar construir desde otras fuentes
                tvTitle.setText("Comunicados de mi Salón");
            }
        } else if ("SALONES".equals(tipoFiltro)) {
            tvTitle.setText("Comunicados de Salones");
        } else {
            tvTitle.setText("Todos los Comunicados");
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        cargarTodosComunicados();
        setupBottomNavigation();
    }

    private void cargarTodosComunicados() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId == null) {
            mostrarMensajeVacio();
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

                        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        String miSeccion = userPrefs.getString("user_seccion", "");
                        String rolNormalizado = userMode != null ? userMode.toUpperCase().trim() : "ALUMNO";

                        boolean esProfesor = "PROFESOR".equals(rolNormalizado);
                        boolean esAdmin    = "ADMIN".equals(rolNormalizado) || "ADMINISTRADOR".equals(rolNormalizado);
                        boolean esAlumno   = !esProfesor && !esAdmin;

                        // Parsear comunicados
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
                                    // Si la sección está vacía o coincide (ignora mayúsculas y espacios)
                                    if (miSeccion.isEmpty() || (salonInfo != null && 
                                        salonInfo.replaceAll("\\s+", "").equalsIgnoreCase(miSeccion.replaceAll("\\s+", "")))) {
                                        miSalon.add(comunicado);
                                    }
                                } else {
                                    otrosSalones.add(comunicado);
                                }
                            }
                        }

                        // Ordenar por fecha (más reciente primero)
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

                        mostrarComunicados(globales, miSalon, otrosSalones);

                    } else {
                        mostrarMensajeVacio();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                    Toast.makeText(ViewComunicadosActivity.this, "Error al cargar comunicados: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    mostrarMensajeVacio();
                }
            });
    }

    private void mostrarComunicados(List<Comunicado> globales, List<Comunicado> miSalon, List<Comunicado> otrosSalones) {
        containerComunicados.removeAllViews();

        String rolNormalizado = userMode != null ? userMode.toUpperCase().trim() : "ALUMNO";
        boolean esProfesor = "PROFESOR".equals(rolNormalizado);
        boolean esAdmin    = "ADMIN".equals(rolNormalizado) || "ADMINISTRADOR".equals(rolNormalizado);
        boolean esAlumno   = !esProfesor && !esAdmin;

        if ("GLOBAL".equals(tipoFiltro)) {
            // El header de la actividad ya dice "Comunicados" — NO se repite abajo
            if (!globales.isEmpty()) {
                String fechaAnterior = "";
                for (Comunicado c : globales) {
                    String fechaFormateada = obtenerFechaFormateada(c.fechaPublicacion);
                    if (!fechaFormateada.equals(fechaAnterior)) {
                        agregarHeaderFecha(c.fechaPublicacion, fechaFormateada);
                        fechaAnterior = fechaFormateada;
                    }
                    agregarCardComunicado(c);
                }
            } else {
                mostrarMensajeVacio();
            }

        } else if ("MI_SALON".equals(tipoFiltro)) {
            // El header ya dice "Comunicados de mi Salón (X)" — NO se repite abajo
            if (!miSalon.isEmpty()) {
                String fechaAnterior = "";
                for (Comunicado c : miSalon) {
                    String fechaFormateada = obtenerFechaFormateada(c.fechaPublicacion);
                    if (!fechaFormateada.equals(fechaAnterior)) {
                        agregarHeaderFecha(c.fechaPublicacion, fechaFormateada);
                        fechaAnterior = fechaFormateada;
                    }
                    agregarCardComunicado(c);
                }
            } else {
                mostrarMensajeVacio();
            }

        } else if ("SALONES".equals(tipoFiltro)) {
            // El header ya dice "Comunicados por Salón" — NO se repite abajo
            if (!otrosSalones.isEmpty()) {
                String fechaAnterior = "";
                for (Comunicado c : otrosSalones) {
                    String fechaFormateada = obtenerFechaFormateada(c.fechaPublicacion);
                    if (!fechaFormateada.equals(fechaAnterior)) {
                        agregarHeaderFecha(c.fechaPublicacion, fechaFormateada);
                        fechaAnterior = fechaFormateada;
                    }
                    agregarCardComunicado(c);
                }
            } else {
                mostrarMensajeVacio();
            }

        } else {
            // TODOS — se usan headers de sección para separar
            boolean hayAlgo = false;

            if (!globales.isEmpty()) {
                hayAlgo = true;
                agregarSectionHeader("Comunicados");
                String fechaAnterior = "";
                for (Comunicado c : globales) {
                    String fechaFormateada = obtenerFechaFormateada(c.fechaPublicacion);
                    if (!fechaFormateada.equals(fechaAnterior)) {
                        agregarHeaderFecha(c.fechaPublicacion, fechaFormateada);
                        fechaAnterior = fechaFormateada;
                    }
                    agregarCardComunicado(c);
                }
            }

            if (esAlumno && !miSalon.isEmpty()) {
                hayAlgo = true;
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String seccionAlumno = prefs.getString("user_seccion", "");
                String tituloSalon = (seccionAlumno != null && !seccionAlumno.isEmpty())
                    ? "Comunicados - " + seccionAlumno
                    : "Comunicados de mi Salón";
                agregarSectionHeader(tituloSalon);
                String fechaAnterior = "";
                for (Comunicado c : miSalon) {
                    String fechaFormateada = obtenerFechaFormateada(c.fechaPublicacion);
                    if (!fechaFormateada.equals(fechaAnterior)) {
                        agregarHeaderFecha(c.fechaPublicacion, fechaFormateada);
                        fechaAnterior = fechaFormateada;
                    }
                    agregarCardComunicado(c);
                }
            }

            if (!otrosSalones.isEmpty() && !esAlumno) {
                hayAlgo = true;
                String tituloSalones = esAdmin ? "Comunicados por Salón" : "Comunicados de mis Salones";
                agregarSectionHeader(tituloSalones);
                String fechaAnterior = "";
                for (Comunicado c : otrosSalones) {
                    String fechaFormateada = obtenerFechaFormateada(c.fechaPublicacion);
                    if (!fechaFormateada.equals(fechaAnterior)) {
                        agregarHeaderFecha(c.fechaPublicacion, fechaFormateada);
                        fechaAnterior = fechaFormateada;
                    }
                    agregarCardComunicado(c);
                }
            }

            if (!hayAlgo) mostrarMensajeVacio();
        }
    }

    /**
     * Header de sección (solo para vista "TODOS" donde hay múltiples grupos).
     */
    private void agregarSectionHeader(String titulo) {
        TextView header = new TextView(this);
        header.setText(titulo);
        header.setTextSize(16);
        header.setTextColor(getResources().getColor(android.R.color.black, null));
        header.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.londrinasolidregular));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
        params.bottomMargin = (int) (4 * getResources().getDisplayMetrics().density);
        params.leftMargin = (int) (16 * getResources().getDisplayMetrics().density);
        header.setLayoutParams(params);
        containerComunicados.addView(header);
    }

    /**
     * Header de fecha uniforme: "Hoy, 19 de Mayo" / "Ayer, 19 de Mayo" / "Lunes, 19 de Mayo"
     * Todo en el mismo tamaño de texto.
     */
    private void agregarHeaderFecha(String rawFecha, String fechaFormateada) {
        float density = getResources().getDisplayMetrics().density;

        TextView tvFecha = new TextView(this);
        tvFecha.setText(fechaFormateada);
        tvFecha.setTextSize(14);
        tvFecha.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppinssemibold));
        tvFecha.setTextColor(Color.parseColor("#3D3D3D"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = (int) (18 * density);
        params.bottomMargin = (int) (6 * density);
        params.leftMargin = (int) (16 * density);
        params.rightMargin = (int) (16 * density);
        tvFecha.setLayoutParams(params);
        containerComunicados.addView(tvFecha);
    }

    private void agregarCardComunicado(Comunicado comunicado) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_comunicado, containerComunicados, false);

        // Emisor
        TextView tvEmisor = cardView.findViewById(R.id.tv_emisor_comunicado);
        if (tvEmisor != null) {
            tvEmisor.setText(comunicado.emisor != null ? comunicado.emisor : "Administración");
        }
        
        // Título
        ((TextView) cardView.findViewById(R.id.tv_titulo_comunicado)).setText(comunicado.titulo);

        // Hora
        TextView tvHora = cardView.findViewById(R.id.tv_hora_comunicado);
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

        // Contenido (preview con límite de 150 caracteres)
        TextView tvContenido = cardView.findViewById(R.id.tv_contenido_comunicado);
        TextView btnVerMas = cardView.findViewById(R.id.btn_ver_mas_comunicado);
        tvContenido.setText(comunicado.contenido);
        tvContenido.setVisibility(View.VISIBLE);
        
        // Mostrar "Ver más" si el contenido es largo
        if (comunicado.contenido != null && comunicado.contenido.length() > 150) {
            btnVerMas.setVisibility(View.VISIBLE);
        } else {
            btnVerMas.setVisibility(View.GONE);
        }

        // Tag de salón
        TextView tvTagSalon = cardView.findViewById(R.id.tv_tag_salon);
        if (comunicado.salon != null && !comunicado.salon.isEmpty()) {
            tvTagSalon.setText("Salón: " + comunicado.salon);
            tvTagSalon.setVisibility(View.VISIBLE);
        } else {
            tvTagSalon.setVisibility(View.GONE);
        }

        // Click → abrir modal con contenido completo
        cardView.setOnClickListener(v -> mostrarDetalleModal(comunicado));

        containerComunicados.addView(cardView);
    }

    /**
     * Modal de detalle del comunicado. Se cierra presionando fuera.
     */
    private void mostrarDetalleModal(Comunicado comunicado) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_comunicado_detalle, null);

        // Título
        ((TextView) dialogView.findViewById(R.id.tv_titulo_detalle)).setText(comunicado.titulo);

        // Fecha formateada
        TextView tvFecha = dialogView.findViewById(R.id.tv_fecha_detalle);
        if (comunicado.fechaPublicacion != null && !comunicado.fechaPublicacion.isEmpty()) {
            tvFecha.setText(obtenerFechaFormateada(comunicado.fechaPublicacion));
            tvFecha.setVisibility(View.VISIBLE);
        } else {
            tvFecha.setVisibility(View.GONE);
        }

        // Hora
        TextView tvHora = dialogView.findViewById(R.id.tv_hora_detalle);
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
                    tvHora.setText(String.format("%d:%02d %s", hora, minuto, ampm));
                    tvHora.setVisibility(View.VISIBLE);
                } else {
                    tvHora.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                tvHora.setVisibility(View.GONE);
            }
        } else {
            tvHora.setVisibility(View.GONE);
        }

        // Contenido completo
        ((TextView) dialogView.findViewById(R.id.tv_contenido_detalle)).setText(comunicado.contenido);

        // Tag salón
        TextView tvTag = dialogView.findViewById(R.id.tv_tag_detalle);
        if (comunicado.salon != null && !comunicado.salon.isEmpty()) {
            tvTag.setText("Salón: " + comunicado.salon);
            tvTag.setVisibility(View.VISIBLE);
        } else {
            tvTag.setVisibility(View.GONE);
        }

        // El dialog se cierra al presionar fuera (setCanceledOnTouchOutside = true por defecto)
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String obtenerFechaFormateada(String fechaPublicacion) {
        if (fechaPublicacion == null || fechaPublicacion.isEmpty()) return "Fecha desconocida";
        try {
            // Parsear fecha ISO 8601 con zona horaria UTC
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            // Remover la 'Z' y milisegundos si existen
            String fechaLimpia = fechaPublicacion.replace("Z", "").split("\\.")[0];
            Date fechaUTC = isoFormat.parse(fechaLimpia);
            
            // Convertir a zona horaria de Perú (UTC-5)
            Calendar fechaComunicado = Calendar.getInstance();
            fechaComunicado.setTime(fechaUTC);
            fechaComunicado.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            
            Calendar hoy = Calendar.getInstance();
            hoy.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            
            Calendar ayer = Calendar.getInstance();
            ayer.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            ayer.add(Calendar.DAY_OF_YEAR, -1);

            int dia = fechaComunicado.get(Calendar.DAY_OF_MONTH);
            int mes = fechaComunicado.get(Calendar.MONTH);

            if (esMismoDia(fechaComunicado, hoy)) {
                return "Hoy, " + dia + " de " + obtenerNombreMes(mes);
            } else if (esMismoDia(fechaComunicado, ayer)) {
                return "Ayer, " + dia + " de " + obtenerNombreMes(mes);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
                String resultado = sdf.format(fechaComunicado.getTime());
                return resultado.substring(0, 1).toUpperCase() + resultado.substring(1);
            }
        } catch (Exception e) {
            android.util.Log.e("ViewComunicados", "Error parseando fecha: " + fechaPublicacion, e);
            return "Fecha desconocida";
        }
    }

    private boolean esMismoDia(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String obtenerNombreMes(int mes) {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                         "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return meses[mes];
    }

    private void mostrarMensajeVacio() {
        containerComunicados.removeAllViews();
        TextView tvVacio = new TextView(this);
        tvVacio.setText("No hay comunicados disponibles");
        tvVacio.setTextSize(16);
        tvVacio.setTextColor(Color.parseColor("#BCC1CD"));
        tvVacio.setGravity(Gravity.CENTER);
        tvVacio.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppinsmedium));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        tvVacio.setLayoutParams(params);
        containerComunicados.addView(tvVacio);
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
        bottomNav.setItemIconTintList(new ColorStateList(states, colors));

        // No resaltar ningún ícono (esta pantalla es modal, no está en el nav principal)
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_homework) {
                Intent intent = new Intent(this, CursosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_horarios) {
                Intent intent = new Intent(this, HorariosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    // Clase interna Comunicado
    private static class Comunicado {
        String titulo, contenido, banner, salon, emisor, fechaPublicacion;

        Comunicado(String t, String c, String b, String s, String e) {
            titulo = t;
            contenido = c;
            banner = b;
            salon = s;
            emisor = e;
        }
    }
}
