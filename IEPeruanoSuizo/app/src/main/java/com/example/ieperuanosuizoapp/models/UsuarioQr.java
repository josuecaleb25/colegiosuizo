package com.example.ieperuanosuizoapp.models;

public class UsuarioQr {
    private int id;
    private String codigo;
    private String nombre_completo;
    private String seccion;
    private String qr_token;
    private String qr_image;
    private String email;
    
    // Constructor
    public UsuarioQr() {}
    
    public UsuarioQr(int id, String codigo, String nombre_completo, String seccion, String qr_token, String qr_image, String email) {
        this.id = id;
        this.codigo = codigo;
        this.nombre_completo = nombre_completo;
        this.seccion = seccion;
        this.qr_token = qr_token;
        this.qr_image = qr_image;
        this.email = email;
    }
    
    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getNombre_completo() { return nombre_completo; }
    public void setNombre_completo(String nombre_completo) { this.nombre_completo = nombre_completo; }
    
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    
    public String getQr_token() { return qr_token; }
    public void setQr_token(String qr_token) { this.qr_token = qr_token; }
    
    public String getQr_image() { return qr_image; }
    public void setQr_image(String qr_image) { this.qr_image = qr_image; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}