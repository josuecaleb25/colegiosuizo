package com.example.ieperuanosuizoapp.models;

public class Curso {
    private int id;
    private String nombre;
    private String codigo;
    private String grado_nombre;
    private String profesor_nombre;
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getGrado_nombre() { return grado_nombre; }
    public void setGrado_nombre(String grado_nombre) { this.grado_nombre = grado_nombre; }
    
    public String getProfesor_nombre() { return profesor_nombre; }
    public void setProfesor_nombre(String profesor_nombre) { this.profesor_nombre = profesor_nombre; }
}