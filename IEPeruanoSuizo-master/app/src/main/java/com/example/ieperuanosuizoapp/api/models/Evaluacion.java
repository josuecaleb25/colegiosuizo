package com.example.ieperuanosuizoapp.api.models;

import com.google.gson.annotations.SerializedName;

public class Evaluacion {
    @SerializedName("id")
    private String id;

    @SerializedName("asignacion_id")
    private String asignacionId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("peso")
    private double peso;

    @SerializedName("orden")
    private int orden;

    @SerializedName("activo")
    private boolean activo;

    @SerializedName("created_at")
    private String createdAt;

    // Constructor vacío
    public Evaluacion() {
    }

    // Constructor para crear nueva evaluación
    public Evaluacion(String asignacionId, String nombre, double peso, int orden) {
        this.asignacionId = asignacionId;
        this.nombre = nombre;
        this.peso = peso;
        this.orden = orden;
        this.activo = true;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAsignacionId() {
        return asignacionId;
    }

    public void setAsignacionId(String asignacionId) {
        this.asignacionId = asignacionId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
