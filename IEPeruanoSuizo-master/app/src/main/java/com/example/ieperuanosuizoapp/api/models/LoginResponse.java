package com.example.ieperuanosuizoapp.api.models;

public class LoginResponse {
    private boolean success;
    private String message;
    private Data data;

    public static class Data {
        private User user;
        private Tokens tokens;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getToken() {
            // Retornar el access token
            return tokens != null ? tokens.getAccess() : null;
        }

        public Tokens getTokens() {
            return tokens;
        }

        public void setTokens(Tokens tokens) {
            this.tokens = tokens;
        }
    }

    public static class Tokens {
        private String access;
        private String refresh;

        public String getAccess() {
            return access;
        }

        public void setAccess(String access) {
            this.access = access;
        }

        public String getRefresh() {
            return refresh;
        }

        public void setRefresh(String refresh) {
            this.refresh = refresh;
        }
    }

    public static class User {
        private String id;
        private String email;
        private String rol;
        private String nombres;
        private String apellidos;
        private String nombre_completo;
        private String seccion;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRol() {
            return rol;
        }

        public void setRol(String rol) {
            this.rol = rol;
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
            return nombre_completo != null ? nombre_completo : (nombres + " " + apellidos);
        }

        public void setNombreCompleto(String nombre_completo) {
            this.nombre_completo = nombre_completo;
        }

        public String getSeccion() {
            return seccion;
        }

        public void setSeccion(String seccion) {
            this.seccion = seccion;
        }

        // Mantener compatibilidad con código que espera objeto persona
        public Persona getPersona() {
            Persona p = new Persona();
            p.setNombres(this.nombres);
            p.setApellidos(this.apellidos);
            return p;
        }
    }

    public static class Persona {
        private String nombres;
        private String apellidos;

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
            return nombres + " " + apellidos;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
