package com.example.ieperuanosuizoapp.api.models;

public class AsistenciaVivo {
    private String id;
    private String persona_id;
    private String fecha;
    private String hora_entrada;
    private String estado;
    private String sesion_id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPersona_id() {
        return persona_id;
    }

    public void setPersona_id(String persona_id) {
        this.persona_id = persona_id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora_entrada() {
        return hora_entrada;
    }

    public void setHora_entrada(String hora_entrada) {
        this.hora_entrada = hora_entrada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getSesion_id() {
        return sesion_id;
    }

    public void setSesion_id(String sesion_id) {
        this.sesion_id = sesion_id;
    }
}
