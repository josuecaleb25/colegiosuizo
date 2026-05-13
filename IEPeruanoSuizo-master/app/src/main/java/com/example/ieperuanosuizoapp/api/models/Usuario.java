package com.example.ieperuanosuizoapp.api.models;

public class Usuario {
    private String id;
    private String codigo_alumno;
    private String nombres;
    private String apellidos;
    private String nombre_completo;
    private String seccion;
    private String estado;
    private String dni;
    private String email;
    private String qr_code;
    private String qr_image;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigoAlumno() {
        return codigo_alumno;
    }

    public void setCodigoAlumno(String codigo_alumno) {
        this.codigo_alumno = codigo_alumno;
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

    public String getNombreCompleto() {
        if (nombre_completo != null && !nombre_completo.isEmpty()) {
            return nombre_completo;
        }
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "");
    }

    public void setNombreCompleto(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getSeccion() {
        return seccion != null ? seccion : "";
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public String getEstado() {
        return estado != null ? estado : "activo";
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQrCode() {
        return qr_code;
    }

    public void setQrCode(String qr_code) {
        this.qr_code = qr_code;
    }

    public String getQrImage() {
        return qr_image;
    }

    public void setQrImage(String qr_image) {
        this.qr_image = qr_image;
    }

    public String getInitials() {
        String initials = "";
        if (nombres != null && !nombres.isEmpty()) {
            initials += nombres.charAt(0);
        }
        if (apellidos != null && !apellidos.isEmpty()) {
            initials += apellidos.charAt(0);
        }
        return initials.toUpperCase();
    }
}
