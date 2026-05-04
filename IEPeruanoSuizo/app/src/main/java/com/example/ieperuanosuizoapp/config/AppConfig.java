package com.example.ieperuanosuizoapp.config;

public class AppConfig {
    // Para emulador Android Studio usar: "http://10.0.2.2:8000/"
    // Para dispositivo físico usar tu IP local: "http://192.168.101.9:8000/"
    // Para producción usar: "https://tu-dominio.com/"
    
    // CONFIGURACIÓN ACTUAL: Dispositivo físico
    public static final String BASE_URL = "http://192.168.101.9:8000/";  // Tu dispositivo físico
    // public static final String BASE_URL = "http://10.0.2.2:8000/";  // Emulador
    
    // Configuración de timeouts
    public static final int CONNECT_TIMEOUT = 30; // segundos
    public static final int READ_TIMEOUT = 30; // segundos
    public static final int WRITE_TIMEOUT = 30; // segundos
    
    // Configuración de refresh
    public static final int AUTO_REFRESH_INTERVAL = 10000; // 10 segundos
}