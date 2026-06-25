package com.example.ieperuanosuizoapp.api.models;

import com.google.gson.annotations.SerializedName;

public class Notificacion {
    @SerializedName("id")
    private String id;

    @SerializedName("estudiante_id")
    private String estudianteId;

    @SerializedName("tipo")
    private String tipo;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("datos")
    private Object datos;

    @SerializedName("leida")
    private boolean leida;

    @SerializedName("fecha_envio")
    private String fechaEnvio;

    @SerializedName("fecha_lectura")
    private String fechaLectura;

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public boolean isLeida() { return leida; }
    public String getFechaEnvio() { return fechaEnvio; }
    public void setLeida(boolean leida) { this.leida = leida; }
}
