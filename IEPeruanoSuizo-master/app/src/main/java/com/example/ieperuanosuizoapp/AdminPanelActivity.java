package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AdminPanelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        // Botón de retroceso
        CardView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Cards de gestión
        CardView cardGestionCursos = findViewById(R.id.card_gestion_cursos);
        CardView cardGestionAlumnos = findViewById(R.id.card_gestion_alumnos);
        CardView cardGestionProfesores = findViewById(R.id.card_gestion_profesores);
        CardView cardCalificaciones = findViewById(R.id.card_calificaciones);
        CardView cardContenido = findViewById(R.id.card_contenido);
        CardView cardReportes = findViewById(R.id.card_reportes);
        CardView cardAsistencia = findViewById(R.id.card_asistencia);
        CardView cardPlanificacion = findViewById(R.id.card_planificacion);
        CardView cardComunicacion = findViewById(R.id.card_comunicacion);
        CardView cardAcceso = findViewById(R.id.card_acceso);

        // Listeners para cada card (simulado)
        cardGestionCursos.setOnClickListener(v -> 
            Toast.makeText(this, "Gestión de Cursos - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardGestionAlumnos.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsuariosActivity.class);
            startActivity(intent);
        });
        
        cardGestionProfesores.setOnClickListener(v -> 
            Toast.makeText(this, "Gestión de Profesores - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardCalificaciones.setOnClickListener(v -> 
            Toast.makeText(this, "Registro de Calificaciones - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardContenido.setOnClickListener(v -> 
            Toast.makeText(this, "Gestión de Contenido - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardReportes.setOnClickListener(v -> 
            Toast.makeText(this, "Reportes y Estadísticas - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardAsistencia.setOnClickListener(v -> {
            Intent intent = new Intent(this, GestionAsistenciaActivity.class);
            startActivity(intent);
        });
        
        cardPlanificacion.setOnClickListener(v -> 
            Toast.makeText(this, "Planificación Académica - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardComunicacion.setOnClickListener(v -> 
            Toast.makeText(this, "Comunicación Masiva - Próximamente", Toast.LENGTH_SHORT).show());
        
        cardAcceso.setOnClickListener(v -> 
            Toast.makeText(this, "Control de Acceso - Próximamente", Toast.LENGTH_SHORT).show());
    }
}
