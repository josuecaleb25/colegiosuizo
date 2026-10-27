package com.example.ieperuanosuizoapp.api;

import com.example.ieperuanosuizoapp.api.models.Alumno;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno;
import com.example.ieperuanosuizoapp.api.models.EscanearQrData;
import com.example.ieperuanosuizoapp.api.models.LoginRequest;
import com.example.ieperuanosuizoapp.api.models.LoginResponse;
import com.example.ieperuanosuizoapp.api.models.ActivationCodeResponse;
import com.example.ieperuanosuizoapp.api.models.Notificacion;
import com.example.ieperuanosuizoapp.api.models.NotificacionesNoLeidasResponse;
import com.example.ieperuanosuizoapp.api.models.NotificacionesResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    // Autenticación
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/activate-account")
    Call<okhttp3.ResponseBody> activateAccount(@Body Map<String, String> body);

    @POST("auth/change-password")
    Call<ApiResponse<Object>> changePassword(@Body Map<String, String> body);

    @POST("admin/accounts/activation-code")
    Call<ActivationCodeResponse> issueActivationCode(@Body Map<String, String> body);
    
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

    // ============================================
    // SESIONES DE ASISTENCIA (tiempo real compartido)
    // ============================================

    // Obtener la sesión activa del día
    @GET("asistencia/sesiones/activa")
    Call<ApiResponse<com.example.ieperuanosuizoapp.api.models.SesionAsistencia>> getSesionActiva();

    // Obtener la sesión del día, abierta o cerrada
    @GET("asistencia/sesiones/hoy")
    Call<ApiResponse<com.example.ieperuanosuizoapp.api.models.SesionAsistencia>> getSesionDelDia();

    // Crear una sesión de asistencia
    @POST("asistencia/sesiones")
    Call<ApiResponse<com.example.ieperuanosuizoapp.api.models.SesionAsistencia>> crearSesion(@Body Map<String, String> body);

    // Cerrar una sesión de asistencia
    @retrofit2.http.PUT("asistencia/sesiones/{id}/cerrar")
    Call<ApiResponse<com.example.ieperuanosuizoapp.api.models.SesionAsistencia>> cerrarSesion(@retrofit2.http.Path("id") String id);

    // Obtener asistencias del día (liviano, para polling en vivo)
    @GET("asistencia/del-dia")
    Call<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaVivo>>> getAsistenciasDelDia(
        @Query("fecha") String fecha,
        @Query("sesion_id") String sesionId
    );
    
    // Comunicados
    @GET("comunicados")
    Call<ApiResponse<List<Object>>> getComunicados(
        @Query("tipo") String tipo,
        @Query("seccion_id") String seccionId,
        @Query("usuario_id") String usuarioId
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
        @Query("usuario_id") String usuarioId,
        @Query("rol") String rol
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

    @GET("secciones")
    Call<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.Section>>> getSeccionesTyped(
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

    @GET("admin/asistencia/historial")
    Call<ApiResponse<List<com.example.ieperuanosuizoapp.api.models.AsistenciaResumenDia>>> getResumenAsistencia(
        @Query("desde") String desde,
        @Query("hasta") String hasta
    );
    
    // Asistencia - Obtener días asistidos de un alumno
    @GET("mobile/asistencia/dias-asistidos/{persona_id}")
    Call<ApiResponse<Object>> getDiasAsistidos(
        @retrofit2.http.Path("persona_id") String personaId,
        @Query("semana") String semana
    );
    
    // Asistencia - Registrar múltiples ausentes en batch
    @POST("asistencia/registrar-ausentes-batch")
    Call<ApiResponse<Object>> registrarAusentesBatch(@Body Map<String, Object> body);
    
    // Asistencia - Eliminar múltiples asistencias en batch
    @POST("asistencia/eliminar-batch")
    Call<ApiResponse<Object>> eliminarAsistenciasBatch(@Body Map<String, Object> body);
    
    // ============================================
    // NOTIFICACIONES (Historial)
    // ============================================

    @GET("notificaciones")
    Call<NotificacionesResponse> getNotificaciones(
        @Query("estudiante_id") String estudianteId,
        @Query("page") Integer page,
        @Query("limit") Integer limit,
        @Query("solo_no_leidas") Boolean soloNoLeidas
    );

    @GET("notificaciones/no-leidas")
    Call<NotificacionesNoLeidasResponse> getNotificacionesNoLeidas(
        @Query("estudiante_id") String estudianteId
    );

    @PUT("notificaciones/{id}/leer")
    Call<ApiResponse<Object>> marcarNotificacionLeida(
        @Path("id") String notificacionId
    );
}
