package com.example.ieperuanosuizoapp.api.models;

public class Estudiante {
    private String id;
    private String nombre;
    private String fotoUrl;
    private boolean esUsted;

    public Estudiante(String id, String nombre, String fotoUrl, boolean esUsted) {
        this.id = id;
        this.nombre = nombre;
        this.fotoUrl = fotoUrl;
        this.esUsted = esUsted;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getFotoUrl() { return fotoUrl; }
    public boolean isEsUsted() { return esUsted; }
}
