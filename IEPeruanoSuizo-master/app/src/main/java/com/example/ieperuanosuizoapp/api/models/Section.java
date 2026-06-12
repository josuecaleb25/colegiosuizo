package com.example.ieperuanosuizoapp.api.models;

import com.google.gson.annotations.SerializedName;

public class Section {

    @SerializedName("id")
    private String id;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("grado")
    private String grado;

    @SerializedName("seccion")
    private String seccion;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getGrado() { return grado; }
    public void setGrado(String grado) { this.grado = grado; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }

    @Override
    public String toString() { return nombre; }
}
