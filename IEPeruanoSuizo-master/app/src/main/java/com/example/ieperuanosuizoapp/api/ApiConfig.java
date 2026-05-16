package com.example.ieperuanosuizoapp.api;

public class ApiConfig {
    // MODO DE DESARROLLO: true para usar backend local
    private static final boolean USE_LOCAL = false;
    
    // URL del backend en producción (Render)
    private static final String PRODUCTION_URL = "https://colegiosuizo-yl5x.onrender.com/api/";
    
    // URL del backend local (tu PC en la red local)
    // Tu celular debe estar en la misma red WiFi (192.168.101.x)
    private static final String LOCAL_URL = "http://192.168.101.7:8000/api/";
    
    // URL activa según el modo
    public static final String BASE_URL = USE_LOCAL ? LOCAL_URL : PRODUCTION_URL;
    
    // Timeouts más largos para desarrollo local
    public static final int CONNECT_TIMEOUT = 60; // segundos
    public static final int READ_TIMEOUT = 60; // segundos
    public static final int WRITE_TIMEOUT = 60; // segundos
}
