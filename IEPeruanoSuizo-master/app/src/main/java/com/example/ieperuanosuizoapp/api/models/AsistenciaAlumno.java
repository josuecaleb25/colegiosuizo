package com.example.ieperuanosuizoapp.api.models;

public class AsistenciaAlumno {
    private String id;
    private String persona_id;
    private String nombre_completo;
    private String salon;
    private String hora_registro;
    private String estado_entrada;

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

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getSalon() {
        return salon;
    }

    public void setSalon(String salon) {
        this.salon = salon;
    }

    public String getHora_registro() {
        return hora_registro;
    }

    public void setHora_registro(String hora_registro) {
        this.hora_registro = hora_registro;
    }

    public String getEstado_entrada() {
        return estado_entrada;
    }

    public void setEstado_entrada(String estado_entrada) {
        this.estado_entrada = estado_entrada;
    }
}
