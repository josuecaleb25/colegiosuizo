package com.example.ieperuanosuizoapp.models;

public class AlumnoAsistencia {
    private int id;
    private String nombre_completo;
    private String salon;
    private String hora_entrada;
    private String estado_entrada;
    
    // Constructor
    public AlumnoAsistencia() {}
    
    public AlumnoAsistencia(String nombre_completo, String salon, String hora_entrada, String estado_entrada) {
        this.nombre_completo = nombre_completo;
        this.salon = salon;
        this.hora_entrada = hora_entrada;
        this.estado_entrada = estado_entrada;
    }
    
    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNombre_completo() { return nombre_completo; }
    public void setNombre_completo(String nombre_completo) { this.nombre_completo = nombre_completo; }
    
    public String getSalon() { return salon; }
    public void setSalon(String salon) { this.salon = salon; }
    
    public String getHora_entrada() { return hora_entrada; }
    public void setHora_entrada(String hora_entrada) { this.hora_entrada = hora_entrada; }
    
    public String getEstado_entrada() { return estado_entrada; }
    public void setEstado_entrada(String estado_entrada) { this.estado_entrada = estado_entrada; }
}