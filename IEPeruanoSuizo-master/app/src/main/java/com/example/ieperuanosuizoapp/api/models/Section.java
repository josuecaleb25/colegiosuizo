package com.example.ieperuanosuizoapp.api.models;

import com.google.gson.annotations.SerializedName;

public class Section {

    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("grado")
    private String grado;

    @SerializedName("seccion")
    private String seccion;

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getGrado() { return grado; }
    public String getSeccion() { return seccion; }

    @Override
    public String toString() { return nombre; }
}
