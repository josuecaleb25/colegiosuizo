package com.example.ieperuanosuizoapp.api.models;

/**
 * Respuesta {@code data} de {@code POST mobile/asistencia/escanear-qr}.
 */
public class EscanearQrData {
    private String alumno;
    private String estado;
    private String hora;
    private boolean reused;

    public String getAlumno() {
        return alumno;
    }

    public void setAlumno(String alumno) {
        this.alumno = alumno;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public boolean isReused() {
        return reused;
    }

    public void setReused(boolean reused) {
        this.reused = reused;
    }
}
