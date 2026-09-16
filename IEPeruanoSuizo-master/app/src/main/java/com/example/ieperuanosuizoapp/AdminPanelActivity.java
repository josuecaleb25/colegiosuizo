package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.os.Bundle;
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
        CardView cardGestionAlumnos = findViewById(R.id.card_gestion_alumnos);
        CardView cardAsistencia = findViewById(R.id.card_asistencia);

        cardGestionAlumnos.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsuariosActivity.class);
            startActivity(intent);
        });
        
        cardAsistencia.setOnClickListener(v -> {
            Intent intent = new Intent(this, GestionAsistenciaActivity.class);
            startActivity(intent);
        });
        
    }

}
