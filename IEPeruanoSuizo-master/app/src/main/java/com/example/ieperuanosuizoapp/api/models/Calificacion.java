package com.example.ieperuanosuizoapp.api.models;

import com.google.gson.annotations.SerializedName;

public class Calificacion {
    @SerializedName("id")
    private String id;

    @SerializedName("evaluacion_id")
    private String evaluacionId;

    @SerializedName("alumno_id")
    private String alumnoId;

    @SerializedName("calificacion")
    private Double calificacion; // Puede ser null

    @SerializedName("observaciones")
    private String observaciones;

    @SerializedName("evaluaciones")
    private Evaluacion evaluacion;

    @SerializedName("personas")
    private Alumno alumno;

    // Constructor vacío
    public Calificacion() {
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEvaluacionId() {
        return evaluacionId;
    }

    public void setEvaluacionId(String evaluacionId) {
        this.evaluacionId = evaluacionId;
    }

    public String getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(String alumnoId) {
        this.alumnoId = alumnoId;
    }

    public Double getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Double calificacion) {
        this.calificacion = calificacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Evaluacion getEvaluacion() {
        return evaluacion;
    }

    public void setEvaluacion(Evaluacion evaluacion) {
        this.evaluacion = evaluacion;
    }

    public Alumno getAlumno() {
        return alumno;
    }

    public void setAlumno(Alumno alumno) {
        this.alumno = alumno;
    }

    // Método helper para mostrar la calificación
    public String getCalificacionDisplay() {
        if (calificacion == null) {
            return "--";
        }
        return String.format("%.1f", calificacion);
    }

    // Método helper para obtener el nombre de la evaluación
    public String getNombreEvaluacion() {
        if (evaluacion != null) {
            return evaluacion.getNombre();
        }
        return "Sin nombre";
    }
}
