package com.example.ieperuanosuizoapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GestionEvaluacionesActivity extends AppCompatActivity {

    private String nombreCurso;
    private String nombreProfesor;
    private String salon;
    private LinearLayout containerEvaluaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_evaluaciones);

        // Obtener datos del curso
        nombreCurso = getIntent().getStringExtra("curso");
        nombreProfesor = getIntent().getStringExtra("profesor");
        salon = getIntent().getStringExtra("salon");

        // Configurar header - solo título con salón
        TextView tvTitulo = findViewById(R.id.tv_titulo);
        tvTitulo.setText("Evaluaciones de " + salon);

        // Botón de retroceso
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Contenedor de evaluaciones
        containerEvaluaciones = findViewById(R.id.container_evaluaciones);

        // Botón flotante para agregar evaluación
        FloatingActionButton fabAgregar = findViewById(R.id.fab_agregar_evaluacion);
        fabAgregar.setOnClickListener(v -> mostrarDialogoAgregarEvaluacion());

        // Cargar evaluaciones simuladas
        cargarEvaluaciones();
    }

    private void cargarEvaluaciones() {
        String[] evaluaciones = {
                "Actitudes",
                "Participación",
                "Examen lore",
                "Trabajo grupal",
                "Proyecto final",
                "Exposición"
        };

        for (String evaluacion : evaluaciones) {
            agregarCardEvaluacion(evaluacion);
        }
    }

    private void agregarCardEvaluacion(String nombreEvaluacion) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_evaluacion, containerEvaluaciones, false);
        
        TextView tvNombre = cardView.findViewById(R.id.tv_evaluacion_nombre);
        CardView btnEditar = cardView.findViewById(R.id.btn_editar_evaluacion);
        CardView btnEliminar = cardView.findViewById(R.id.btn_eliminar_evaluacion);

        tvNombre.setText(nombreEvaluacion);

        btnEditar.setOnClickListener(v -> {
            mostrarDialogoEditarEvaluacion(nombreEvaluacion);
        });

        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar Evaluación")
                    .setMessage("¿Está seguro de eliminar '" + nombreEvaluacion + "'?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        containerEvaluaciones.removeView(cardView);
                        Toast.makeText(this, "Evaluación eliminada", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        containerEvaluaciones.addView(cardView);
    }

    private void mostrarDialogoAgregarEvaluacion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Evaluación");
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_evaluacion, null);
        EditText etNombreEvaluacion = dialogView.findViewById(R.id.et_nombre_evaluacion);
        
        builder.setView(dialogView);
        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombreEvaluacion = etNombreEvaluacion.getText().toString().trim();
            
            if (nombreEvaluacion.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre de la evaluación", Toast.LENGTH_SHORT).show();
                return;
            }
            
            agregarCardEvaluacion(nombreEvaluacion);
            Toast.makeText(this, "Evaluación agregada", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoEditarEvaluacion(String nombreActual) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Evaluación");
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_evaluacion, null);
        EditText etNombreEvaluacion = dialogView.findViewById(R.id.et_nombre_evaluacion);
        etNombreEvaluacion.setText(nombreActual);
        
        builder.setView(dialogView);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = etNombreEvaluacion.getText().toString().trim();
            
            if (nuevoNombre.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre de la evaluación", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Toast.makeText(this, "Evaluación actualizada", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}
