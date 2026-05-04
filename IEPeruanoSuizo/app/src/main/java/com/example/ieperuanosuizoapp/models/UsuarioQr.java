package com.example.ieperuanosuizoapp.models;

public class UsuarioQr {
    private String id;  // Cambiado de int a String para UUID
    private String codigo;
    private String nombre_completo;
    private String salon;  // Cambiado de seccion a salon
    private String qr_token;
    private String qr_image;
    private String email;
    
    // Constructor
    public UsuarioQr() {}
    
    public UsuarioQr(String id, String codigo, String nombre_completo, String salon, String qr_token, String qr_image, String email) {
        this.id = id;
        this.codigo = codigo;
        this.nombre_completo = nombre_completo;
        this.salon = salon;
        this.qr_token = qr_token;
        this.qr_image = qr_image;
        this.email = email;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getNombre_completo() { return nombre_completo; }
    public void setNombre_completo(String nombre_completo) { this.nombre_completo = nombre_completo; }
    
    public String getSalon() { return salon; }
    public void setSalon(String salon) { this.salon = salon; }
    
    // Mantener compatibilidad con código antiguo
    public String getSeccion() { return salon; }
    public void setSeccion(String seccion) { this.salon = seccion; }
    
    public String getQr_token() { return qr_token; }
    public void setQr_token(String qr_token) { this.qr_token = qr_token; }
    
    public String getQr_image() { return qr_image; }
    public void setQr_image(String qr_image) { this.qr_image = qr_image; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}