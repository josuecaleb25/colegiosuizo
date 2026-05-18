package com.example.ieperuanosuizoapp.api;

import com.example.ieperuanosuizoapp.api.models.Alumno;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno;
import com.example.ieperuanosuizoapp.api.models.EscanearQrData;
import com.example.ieperuanosuizoapp.api.models.LoginRequest;
import com.example.ieperuanosuizoapp.api.models.LoginResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    
    // Autenticación
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    // Alumnos
    @GET("alumnos")
    Call<ApiResponse<List<Alumno>>> getAlumnos(
        @Query("seccion") String seccion,
        @Query("search") String search,
        @Query("limit") Integer limit
    );
    
    // Asistencia - Listar alumnos para panel de asistencia
    @GET("mobile/asistencia/alumnos")
    Call<ApiResponse<List<AsistenciaAlumno>>> getAlumnosAsistencia(
        @Query("seccion") String seccion
    );

    @POST("mobile/asistencia/escanear-qr")
    Call<ApiResponse<EscanearQrData>> escanearQrAsistencia(@Body Map<String, String> body);
    
    // Comunicados
    @GET("comunicados")
    Call<ApiResponse<List<Object>>> getComunicados(
        @Query("tipo") String tipo,
        @Query("seccion_id") String seccionId,
        @Query("usuario_id") String usuarioId
    );
    
    // Cursos
    @GET("cursos/alumno/{alumno_id}")
    Call<ApiResponse<List<Object>>> getCursosAlumno(
        @retrofit2.http.Path("alumno_id") String alumnoId
    );
    
    @GET("cursos/profesor/{profesor_id}")
    Call<ApiResponse<List<Object>>> getCursosProfesor(
        @retrofit2.http.Path("profesor_id") String profesorId
    );
    
    // Horarios
    @GET("horarios/alumno/{alumno_id}")
    Call<ApiResponse<List<Object>>> getHorariosAlumno(
        @retrofit2.http.Path("alumno_id") String alumnoId,
        @Query("fecha") String fecha
    );
    
    @GET("horarios/profesor/{profesor_id}")
    Call<ApiResponse<List<Object>>> getHorariosProfesor(
        @retrofit2.http.Path("profesor_id") String profesorId,
        @Query("fecha") String fecha
    );
    
    // Gestión de Comunicados
    @POST("comunicados")
    Call<ApiResponse<Object>> crearComunicado(@Body Object comunicado);
    
    @GET("comunicados/historial/enviados")
    Call<ApiResponse<List<Object>>> getHistorialComunicadosEnviados(
        @Query("usuario_id") String usuarioId
    );
    
    @retrofit2.http.PUT("comunicados/{id}")
    Call<ApiResponse<Object>> actualizarComunicado(
        @retrofit2.http.Path("id") String comunicadoId,
        @Body Object comunicado
    );
    
    @retrofit2.http.DELETE("comunicados/{id}")
    Call<ApiResponse<Object>> eliminarComunicado(
        @retrofit2.http.Path("id") String comunicadoId
    );
    
    // Secciones
    @GET("secciones")
    Call<ApiResponse<List<Object>>> getSecciones(
        @Query("usuario_id") String usuarioId,
        @Query("rol") String rol
    );
    
    // Perfil de Usuario
    @GET("usuarios/perfil/{user_id}")
    Call<ApiResponse<Object>> getPerfilUsuario(
        @retrofit2.http.Path("user_id") String userId
    );
    
    @retrofit2.http.PUT("usuarios/perfil/{user_id}")
    Call<ApiResponse<Object>> actualizarPerfil(
        @retrofit2.http.Path("user_id") String userId,
        @Body Object datos
    );
    
    // Gestión de Usuarios (Admin)
    @GET("admin/alumnos")
    Call<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.Usuario>>> getUsuariosAdmin(
        @Query("seccion") String seccion,
        @Query("search") String search,
        @Query("limit") Integer limit
    );

    // Asistencia - Obtener asistencia por fecha (Admin)
    @GET("admin/asistencia/fecha")
    Call<ApiResponse<List<AsistenciaAlumno>>> getAsistenciaPorFecha(
        @Query("fecha") String fecha
    );
    
    // Asistencia - Obtener días asistidos de un alumno
    @GET("mobile/asistencia/dias-asistidos/{persona_id}")
    Call<ApiResponse<Object>> getDiasAsistidos(
        @retrofit2.http.Path("persona_id") String personaId,
        @Query("semana") String semana
    );
}
