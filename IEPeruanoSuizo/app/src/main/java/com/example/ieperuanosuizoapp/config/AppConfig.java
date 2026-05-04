package com.example.ieperuanosuizoapp.config;

public class AppConfig {
    // ============================================
    // BACKEND: Express.js + TypeScript + PostgreSQL (Supabase)
    // ============================================
    
    // DESARROLLO LOCAL:
    // - Para emulador Android Studio usar: "http://10.0.2.2:8000/"
    // - Para dispositivo físico usar tu IP local: "http://192.168.101.9:8000/"
    
    // PRODUCCIÓN (Render):
    // - Backend desplegado en Render.com
    
    // CONFIGURACIÓN ACTUAL: PRODUCCIÓN
    public static final String BASE_URL = "https://colegiosuizo.onrender.com/api/";  // Producción Render
    // public static final String BASE_URL = "http://192.168.101.9:8000/api/";  // Dispositivo físico (desarrollo)
    // public static final String BASE_URL = "http://10.0.2.2:8000/api/";  // Emulador (desarrollo)
    
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