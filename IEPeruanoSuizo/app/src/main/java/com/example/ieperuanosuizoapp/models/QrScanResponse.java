package com.example.ieperuanosuizoapp.models;

public class QrScanResponse {
    private String alumno;
    private String docente;
    private String salon;
    private String estado;
    private String hora;
    
    public String getAlumno() { return alumno; }
    public void setAlumno(String alumno) { this.alumno = alumno; }
    
    public String getDocente() { return docente; }
    public void setDocente(String docente) { this.docente = docente; }
    
    public String getSalon() { return salon; }
    public void setSalon(String salon) { this.salon = salon; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
}