package com.example.ieperuanosuizoapp.api;

import com.example.ieperuanosuizoapp.models.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    
    // Autenticación
    @POST("api/mobile/auth/login/")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
    
    @POST("api/mobile/auth/register/")
    Call<ApiResponse<Usuario>> register(@Body Usuario usuario);
    
    @GET("api/mobile/profile/")
    Call<ApiResponse<Usuario>> getProfile(@Header("Authorization") String token);
    
    // Asistencia
    @GET("api/mobile/asistencia/alumnos/")
    Call<ApiResponse<List<AlumnoAsistencia>>> getAsistenciaAlumnos(
        @Header("Authorization") String token,
        @Query("salon") String salon,
        @Query("search") String search
    );
    
    @POST("api/mobile/asistencia/escanear-qr/")
    Call<ApiResponse<QrScanResponse>> escanearQr(@Body QrScanRequest request);
    
    // Datos generales
    @GET("api/mobile/salones/")
    Call<ApiResponse<List<Salon>>> getSalones(@Header("Authorization") String token);
    
    @GET("api/mobile/cursos/")
    Call<ApiResponse<List<Curso>>> getCursos(@Header("Authorization") String token);
    
    @GET("api/mobile/usuarios/")
    Call<ApiResponse<List<UsuarioQr>>> getUsuarios(@Header("Authorization") String token);
    
    // Dashboard
    @GET("api/mobile/dashboard/stats/")
    Call<ApiResponse<DashboardStats>> getDashboardStats(@Header("Authorization") String token);
}