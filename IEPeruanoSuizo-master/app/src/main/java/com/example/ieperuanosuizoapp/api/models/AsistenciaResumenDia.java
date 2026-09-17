package com.example.ieperuanosuizoapp.api.models;

public class AsistenciaResumenDia {
    private String fecha;
    private int presentes;
    private int tardanzas;
    private int ausentes;

    public String getFecha() { return fecha; }
    public int getPresentes() { return presentes; }
    public int getTardanzas() { return tardanzas; }
    public int getAusentes() { return ausentes; }
}
