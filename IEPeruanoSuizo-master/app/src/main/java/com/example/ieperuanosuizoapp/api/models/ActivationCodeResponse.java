package com.example.ieperuanosuizoapp.api.models;

public class ActivationCodeResponse {
    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        private String email;
        private String rol;
        private String activation_code;
        private String expires_at;

        public String getEmail() {
            return email;
        }

        public String getRol() {
            return rol;
        }

        public String getActivationCode() {
            return activation_code;
        }

        public String getExpiresAt() {
            return expires_at;
        }
    }
}
