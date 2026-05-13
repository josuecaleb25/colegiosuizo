package com.example.ieperuanosuizoapp.api.models;

public class Alumno {
    private String id;
    private String codigo;
    private String dni;
    private String nombres;
    private String apellidos;
    private String nombre_completo;
    private String fecha_nacimiento;
    private String email_padre;
    private String qr_token;
    private boolean activo;
    private String seccion;
    private String seccion_id;
    private String grado_nombre;
    private String seccion_nombre;

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getFecha_nacimiento() {
        return fecha_nacimiento;
    }

    public void setFecha_nacimiento(String fecha_nacimiento) {
        this.fecha_nacimiento = fecha_nacimiento;
    }

    public String getEmail_padre() {
        return email_padre;
    }

    public void setEmail_padre(String email_padre) {
        this.email_padre = email_padre;
    }

    public String getQr_token() {
        return qr_token;
    }

    public void setQr_token(String qr_token) {
        this.qr_token = qr_token;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public String getSeccion_id() {
        return seccion_id;
    }

    public void setSeccion_id(String seccion_id) {
        this.seccion_id = seccion_id;
    }

    public String getGrado_nombre() {
        return grado_nombre;
    }

    public void setGrado_nombre(String grado_nombre) {
        this.grado_nombre = grado_nombre;
    }

    public String getSeccion_nombre() {
        return seccion_nombre;
    }

    public void setSeccion_nombre(String seccion_nombre) {
        this.seccion_nombre = seccion_nombre;
    }
}
