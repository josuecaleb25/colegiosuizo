package com.example.ieperuanosuizoapp.api.models;

public class SesionAsistencia {
    private String id;
    private String fecha;
    private String estado;
    private String creado_por;
    private String creado_en;
    private String cerrado_en;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCreado_por() {
        return creado_por;
    }

    public void setCreado_por(String creado_por) {
        this.creado_por = creado_por;
    }

    public String getCreado_en() {
        return creado_en;
    }

    public void setCreado_en(String creado_en) {
        this.creado_en = creado_en;
    }

    public String getCerrado_en() {
        return cerrado_en;
    }

    public void setCerrado_en(String cerrado_en) {
        this.cerrado_en = cerrado_en;
    }
}
