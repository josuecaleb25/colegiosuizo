package com.example.ieperuanosuizoapp.config;

public class AppConfig {
    // ============================================
    // BACKEND: Express.js + TypeScript + PostgreSQL (Supabase)
    // ============================================
    
    // DESARROLLO LOCAL:
    // - Para emulador Android Studio usar: "http://10.0.2.2:8000/"
    // - Para dispositivo físico usar tu IP local: "http://192.168.101.9:8000/"
    
    // PRODUCCIÓN (Railway):
    // - Cuando despliegues a Railway, cambiar a: "https://tu-app.railway.app/"
    
    // CONFIGURACIÓN ACTUAL: Dispositivo físico (desarrollo local)
    public static final String BASE_URL = "http://192.168.101.9:8000/";  // Tu dispositivo físico
    // public static final String BASE_URL = "http://10.0.2.2:8000/";  // Emulador
    // public static final String BASE_URL = "https://tu-app.railway.app/";  // Producción Railway
    
    // Configuración de timeouts
    public static final int CONNECT_TIMEOUT = 30; // segundos
    public static final int READ_TIMEOUT = 30; // segundos
    public static final int WRITE_TIMEOUT = 30; // segundos
    
    // Configuración de refresh
    public static final int AUTO_REFRESH_INTERVAL = 10000; // 10 segundos
    
    // Información del backend
    public static final String BACKEND_TYPE = "Express.js + TypeScript";
    public static final String DATABASE_TYPE = "PostgreSQL (Supabase)";
}