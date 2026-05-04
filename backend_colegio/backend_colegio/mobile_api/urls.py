from django.urls import path
from . import views

urlpatterns = [
    # Test sin autenticación
    path('test/', views.test_endpoint, name='test_endpoint'),
    path('test-alumnos/', views.test_alumnos, name='test_alumnos'),
    
    # Autenticación
    path('auth/login/', views.mobile_login, name='mobile_login'),
    path('auth/register/', views.mobile_register, name='mobile_register'),
    
    # Perfil
    path('profile/', views.mobile_profile, name='mobile_profile'),
    
    # Asistencia
    path('asistencia/alumnos/', views.mobile_asistencia_alumnos, name='mobile_asistencia_alumnos'),
    path('asistencia/escanear-qr/', views.mobile_escanear_qr, name='mobile_escanear_qr'),
    
    # Datos generales
    path('salones/', views.mobile_salones, name='mobile_salones'),
    path('cursos/', views.mobile_cursos, name='mobile_cursos'),
    path('usuarios/', views.mobile_alumnos_all, name='mobile_alumnos_all'),
    
    # Dashboard
    path('dashboard/stats/', views.mobile_dashboard_stats, name='mobile_dashboard_stats'),
    
    # Historial de asistencia
    path('asistencia/historial/<int:alumno_id>/', views.mobile_historial_asistencia, name='mobile_historial_asistencia'),
    
    # Reportes
    path('reportes/asistencia/', views.mobile_reporte_asistencia, name='mobile_reporte_asistencia'),
]