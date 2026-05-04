package com.example.ieperuanosuizoapp.api;

import com.example.ieperuanosuizoapp.models.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    
    // ============================================
    // BACKEND EXPRESS.JS - NUEVOS ENDPOINTS
    // ============================================
    
    // Autenticación
    @POST("api/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
    
    @POST("api/auth/register")
    Call<ApiResponse<Usuario>> register(@Body Usuario usuario);
    
    @GET("api/auth/profile")
    Call<ApiResponse<Usuario>> getProfile(@Header("Authorization") String token);
    
    // Mobile API - Asistencia
    @GET("api/mobile/asistencia/alumnos")
    Call<ApiResponse<List<AlumnoAsistencia>>> getAsistenciaAlumnos(
        @Header("Authorization") String token,
        @Query("salon") String salon,
        @Query("search") String search
    );
    
    @POST("api/mobile/asistencia/escanear-qr")
    Call<ApiResponse<QrScanResponse>> escanearQr(@Body QrScanRequest request);
    
    // Mobile API - Usuarios con QR
    @GET("api/mobile/usuarios")
    Call<ApiResponse<List<UsuarioQr>>> getUsuarios(@Header("Authorization") String token);
    
    // Mobile API - Test endpoints
    @GET("api/mobile/test")
    Call<ApiResponse<String>> testConnection();
    
    @GET("api/mobile/test-alumnos")
    Call<ApiResponse<List<AlumnoAsistencia>>> testAlumnos();
    
    // Datos generales (endpoints legacy - mantener por compatibilidad)
    @GET("api/mobile/salones")
    Call<ApiResponse<List<Salon>>> getSalones(@Header("Authorization") String token);
    
    @GET("api/mobile/cursos")
    Call<ApiResponse<List<Curso>>> getCursos(@Header("Authorization") String token);
    
    @GET("api/mobile/dashboard/stats")
    Call<ApiResponse<DashboardStats>> getDashboardStats(@Header("Authorization") String token);
}