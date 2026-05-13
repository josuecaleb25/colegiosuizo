package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HorariosActivity extends AppCompatActivity {

    private TextView tvDayNumber, tvDayName, tvMonthYear;
    private LinearLayout calendarStrip, coursesContainer;
    private HorizontalScrollView calendarScroll;
    private Calendar selectedDate;
    private Calendar today;
    private int selectedDayIndex = -1;
    private String userMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", false);
        int colorScheme = prefs.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horarios);

        // Obtener el modo de usuario
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userMode = userPrefs.getString("user_mode", "ALUMNO");

        // Inicializar calendarios
        today = Calendar.getInstance();
        selectedDate = (Calendar) today.clone();

        // Referencias a vistas
        tvDayNumber = findViewById(R.id.tv_day_number);
        tvDayName = findViewById(R.id.tv_day_name);
        tvMonthYear = findViewById(R.id.tv_month_year);
        calendarScroll = findViewById(R.id.calendar_scroll);
        calendarStrip = findViewById(R.id.calendar_strip);
        coursesContainer = findViewById(R.id.courses_container);

        updateHeaderDate();
        generateCalendarStrip();
        loadCoursesForSelectedDate();

        findViewById(R.id.btn_hoy).setOnClickListener(v -> {
            selectedDate = (Calendar) today.clone();
            updateHeaderDate();
            generateCalendarStrip();
            loadCoursesForSelectedDate();
        });

        setupBottomNavigation();
    }

    private void updateHeaderDate() {
        tvDayNumber.setText(String.valueOf(selectedDate.get(Calendar.DAY_OF_MONTH)));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("es", "ES"));
        String dayName = dayFormat.format(selectedDate.getTime());
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
        tvDayName.setText(dayName);
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String monthYear = monthYearFormat.format(selectedDate.getTime());
        monthYear = monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1);
        tvMonthYear.setText(monthYear);
    }

    private void generateCalendarStrip() {
        calendarStrip.removeAllViews();
        Calendar cal = (Calendar) selectedDate.clone();
        cal.add(Calendar.DAY_OF_MONTH, -15);

        for (int i = 0; i < 30; i++) {
            final Calendar dayCalendar = (Calendar) cal.clone();
            boolean isSelected = isSameDay(dayCalendar, selectedDate);
            if (isSelected) selectedDayIndex = i;
            View dayView = createDayView(dayCalendar, isSelected);
            calendarStrip.addView(dayView);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendarScroll.post(() -> {
            float density = getResources().getDisplayMetrics().density;
            int paddingStart = (int) (24 * density);
            int width = (int) (getResources().getDisplayMetrics().widthPixels / 7.0f);
            int scrollX = (selectedDayIndex * width) + paddingStart - (getResources().getDisplayMetrics().widthPixels / 2) + (width / 2);
            calendarScroll.smoothScrollTo(Math.max(0, scrollX), 0);
        });
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private View createDayView(Calendar calendar, boolean isSelected) {
        LinearLayout dayLayout = new LinearLayout(this);
        int width = (int) (getResources().getDisplayMetrics().widthPixels / 7.0f);
        dayLayout.setLayoutParams(new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT));
        dayLayout.setOrientation(LinearLayout.VERTICAL);
        dayLayout.setGravity(android.view.Gravity.CENTER);
        dayLayout.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        dayLayout.setClickable(true);
        dayLayout.setFocusable(true);

        TextView tvLetter = new TextView(this);
        tvLetter.setText(new SimpleDateFormat("E", new Locale("es", "ES")).format(calendar.getTime()).substring(0, 1).toUpperCase());
        tvLetter.setTextSize(12);
        tvLetter.setTypeface(ResourcesCompat.getFont(this, R.font.poppinsmedium));
        tvLetter.setGravity(android.view.Gravity.CENTER);

        TextView tvNum = new TextView(this);
        tvNum.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        tvNum.setTextSize(16);
        tvNum.setTypeface(ResourcesCompat.getFont(this, R.font.poppinssemibold));
        tvNum.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(-1, -2);
        numParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        tvNum.setLayoutParams(numParams);

        if (isSelected) {
            dayLayout.setBackgroundResource(R.drawable.bg_selected_day);
            tvLetter.setTextColor(Color.WHITE);
            tvNum.setTextColor(Color.WHITE);
        } else {
            tvLetter.setTextColor(Color.parseColor("#BCC1CD"));
            tvNum.setTextColor(Color.parseColor("#141A1E"));
        }

        dayLayout.addView(tvLetter);
        dayLayout.addView(tvNum);
        dayLayout.setOnClickListener(v -> {
            selectedDate = (Calendar) calendar.clone();
            updateHeaderDate();
            generateCalendarStrip();
            loadCoursesForSelectedDate();
        });
        return dayLayout;
    }

    private void loadCoursesForSelectedDate() {
        coursesContainer.removeAllViews();
        
        // Obtener fecha seleccionada en formato YYYY-MM-DD
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaSeleccionada = sdf.format(selectedDate.getTime());
        
        // Obtener user_id de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            mostrarMensajeVacio();
            return;
        }
        
        // Llamar al backend según el rol
        retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call;
        
        if ("PROFESOR".equals(userMode)) {
            call = com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
                .getHorariosProfesor(userId, fechaSeleccionada);
        } else {
            call = com.example.ieperuanosuizoapp.api.RetrofitClient.getApiService()
                .getHorariosAlumno(userId, fechaSeleccionada);
        }
        
        call.enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call,
                                 retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Object> horariosData = response.body().getData();
                    
                    if (horariosData.isEmpty()) {
                        mostrarMensajeVacio();
                        return;
                    }
                    
                    mostrarHorarios(horariosData);
                } else {
                    mostrarMensajeVacio();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<List<Object>>> call, Throwable t) {
                android.widget.Toast.makeText(HorariosActivity.this, "Error al cargar horarios: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                mostrarMensajeVacio();
            }
        });
    }
    
    private void mostrarMensajeVacio() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View emptyView = inflater.inflate(android.R.layout.simple_list_item_1, coursesContainer, false);
        TextView tv = emptyView.findViewById(android.R.id.text1);
        tv.setText("No hay actividades programadas para este día");
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextColor(Color.parseColor("#BCC1CD"));
        tv.setTypeface(ResourcesCompat.getFont(this, R.font.poppinsmedium));
        tv.setPadding(0, (int) (40 * getResources().getDisplayMetrics().density), 0, 0);
        coursesContainer.addView(emptyView);
        
        ImageView iv = new ImageView(this);
        iv.setImageResource(R.drawable.buhope);
        iv.setAlpha(0.3f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)(120*getResources().getDisplayMetrics().density), (int)(120*getResources().getDisplayMetrics().density));
        lp.gravity = android.view.Gravity.CENTER;
        lp.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
        iv.setLayoutParams(lp);
        coursesContainer.addView(iv);
    }
    
    private void mostrarHorarios(List<Object> horariosData) {
        LayoutInflater inflater = LayoutInflater.from(this);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        
        for (int i = 0; i < horariosData.size(); i++) {
            com.google.gson.JsonObject jsonObj = gson.toJsonTree(horariosData.get(i)).getAsJsonObject();
            
            String curso = jsonObj.has("curso") ? jsonObj.get("curso").getAsString() : "";
            String horaInicio = jsonObj.has("hora_inicio") ? jsonObj.get("hora_inicio").getAsString() : "";
            String horaFin = jsonObj.has("hora_fin") ? jsonObj.get("hora_fin").getAsString() : "";
            String profesor = jsonObj.has("profesor") ? jsonObj.get("profesor").getAsString() : "";
            String aula = jsonObj.has("aula") ? jsonObj.get("aula").getAsString() : "";
            String seccion = jsonObj.has("seccion") ? jsonObj.get("seccion").getAsString() : "";
            
            View itemView = inflater.inflate(R.layout.item_horario, coursesContainer, false);

            ((TextView) itemView.findViewById(R.id.tv_start_time)).setText(horaInicio);
            ((TextView) itemView.findViewById(R.id.tv_end_time)).setText(horaFin);
            ((TextView) itemView.findViewById(R.id.tv_course_name)).setText(curso);
            
            View layoutLocation = (View) itemView.findViewById(R.id.iv_loc_icon).getParent();
            TextView tvPerson = itemView.findViewById(R.id.tv_person_name);

            if ("PROFESOR".equals(userMode)) {
                // Profesor: Ve salón y Grado/Sección
                ((TextView) itemView.findViewById(R.id.tv_location)).setText(aula);
                tvPerson.setText(seccion);
                layoutLocation.setVisibility(View.VISIBLE);
            } else {
                // Alumno: Solo ve curso y nombre del profesor
                tvPerson.setText(profesor);
                layoutLocation.setVisibility(View.GONE);
            }

            if (i == 0) {
                View card = itemView.findViewById(R.id.card_content);
                card.setBackgroundResource(R.drawable.bg_course_card_red);
                ((TextView) itemView.findViewById(R.id.tv_course_name)).setTextColor(Color.WHITE);
                ((TextView) itemView.findViewById(R.id.tv_location)).setTextColor(Color.WHITE);
                tvPerson.setTextColor(Color.WHITE);
                ((ImageView) itemView.findViewById(R.id.iv_options)).setColorFilter(Color.WHITE);
                ((ImageView) itemView.findViewById(R.id.iv_loc_icon)).setColorFilter(Color.WHITE);
            }

            coursesContainer.addView(itemView);
        }
    }

    private List<Course> getMockCourses(Calendar date) {
        List<Course> list = new ArrayList<>();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);

        // Simulación más completa de horario escolar
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                list.add(new Course("Matemática", "08:00", "09:30", "Salón 4A", "Ricardo Huaman", "4to A"));
                list.add(new Course("Comunicación", "09:30", "11:00", "Salón 4A", "Ana Lima", "4to A"));
                list.add(new Course("Arte y Cultura", "11:30", "13:00", "Patio Central", "Carmen Rosa", "4to A"));
                list.add(new Course("Tutoría", "13:00", "13:45", "Salón 4A", "Ricardo Huaman", "4to A"));
                break;
            case Calendar.TUESDAY:
                list.add(new Course("Inglés", "08:00", "09:30", "Lab Idiomas", "Rosa Condori", "4to A"));
                list.add(new Course("Historia", "09:30", "11:00", "Salón 4A", "Carlos Ramos", "4to A"));
                list.add(new Course("Educación Física", "11:00", "12:30", "Campo Deportivo", "Jorge Chavez", "4to A"));
                list.add(new Course("Ciencia y Tecnología", "12:30", "14:00", "Laboratorio", "Ana Lima", "4to A"));
                break;
            case Calendar.WEDNESDAY:
                list.add(new Course("Matemática", "08:00", "10:00", "Salón 4A", "Ricardo Huaman", "4to A"));
                list.add(new Course("Inglés", "10:00", "11:00", "Salón 4A", "Rosa Condori", "4to A"));
                list.add(new Course("Computación", "11:30", "13:00", "Lab Cómputo", "Josue Ochoa", "4to A"));
                list.add(new Course("Religión", "13:00", "14:00", "Salón 4A", "Carlos Ramos", "4to A"));
                break;
            case Calendar.THURSDAY:
                list.add(new Course("Ciencias Sociales", "08:00", "09:30", "Salón 4A", "Carlos Ramos", "4to A"));
                list.add(new Course("Comunicación", "09:30", "11:00", "Salón 4A", "Ana Lima", "4to A"));
                list.add(new Course("Ciencia y Tecnología", "11:30", "13:00", "Laboratorio", "Ana Lima", "4to A"));
                list.add(new Course("Personal Social", "13:00", "14:00", "Salón 4A", "Carlos Ramos", "4to A"));
                break;
            case Calendar.FRIDAY:
                list.add(new Course("Comunicación", "08:00", "10:00", "Salón 4A", "Ana Lima", "4to A"));
                list.add(new Course("Matemática", "10:00", "12:00", "Salón 4A", "Ricardo Huaman", "4to A"));
                list.add(new Course("Música", "12:30", "14:00", "Salón de Música", "Carmen Rosa", "4to A"));
                break;
            case Calendar.SATURDAY:
                // Sábado con una actividad especial como pediste
                list.add(new Course("Talleres Extraescolares", "09:00", "12:00", "Coliseo", "Varios Profesores", "Multigrado"));
                break;
            default:
                // Domingo u otros casos (vacío)
                break;
        }
        return list;
    }

    private static class Course {
        String name, startTime, endTime, location, teacher, section;
        Course(String n, String s, String e, String l, String t, String sec) {
            name = n; startTime = s; endTime = e; location = l; teacher = t; section = sec;
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}};
        int[] colors = new int[]{Color.parseColor("#BA1924"), Color.parseColor("#5E5F60")};
        bottomNav.setItemIconTintList(new ColorStateList(states, colors));
        bottomNav.setSelectedItemId(R.id.nav_horarios);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            } else if (id == R.id.nav_homework) {
                startActivity(new Intent(this, CursosActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            }
            return id == R.id.nav_horarios;
        });
    }
}