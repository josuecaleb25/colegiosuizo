package com.example.ieperuanosuizoapp.api.models;

import com.google.gson.annotations.SerializedName;

public class LeaderboardEntry {

    @SerializedName("persona_id")
    private int personaId;

    @SerializedName("nombres")
    private String nombres;

    @SerializedName("apellidos")
    private String apellidos;

    @SerializedName("total_dias")
    private int totalDias;

    @SerializedName("puntual")
    private int puntual;

    @SerializedName("tardanza")
    private int tardanza;

    @SerializedName("puntualidad")
    private int puntualidad;

    @SerializedName("asistencia_dias")
    private int asistenciaDias;

    @SerializedName("asistencia")
    private int asistencia;

    public int getPersonaId() { return personaId; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getNombreCompleto() { return nombres + " " + apellidos; }
    public int getTotalDias() { return totalDias; }
    public int getPuntual() { return puntual; }
    public int getTardanza() { return tardanza; }
    public int getPuntualidad() { return puntualidad; }
    public int getAsistenciaDias() { return asistenciaDias; }
    public int getAsistencia() { return asistencia; }
}
