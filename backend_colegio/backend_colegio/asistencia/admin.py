from django.contrib import admin
from .models import Grado, Seccion, Alumno, Curso, SesionClase, TokenQR, Asistencia


@admin.register(Grado)
class GradoAdmin(admin.ModelAdmin):
    list_display = ['nombre', 'nivel', 'activo']
    list_filter = ['nivel', 'activo']


@admin.register(Seccion)
class SeccionAdmin(admin.ModelAdmin):
    list_display = ['__str__', 'tutor', 'activa']
    list_filter = ['grado', 'activa']
    search_fields = ['nombre', 'grado__nombre']


@admin.register(Alumno)
class AlumnoAdmin(admin.ModelAdmin):
    list_display = ['codigo', 'apellidos', 'nombres', 'seccion', 'activo']
    list_filter = ['seccion__grado', 'activo']
    search_fields = ['nombres', 'apellidos', 'codigo', 'dni']


@admin.register(Curso)
class CursoAdmin(admin.ModelAdmin):
    list_display = ['nombre', 'codigo', 'grado', 'profesor', 'activo']
    list_filter = ['grado', 'activo']
    search_fields = ['nombre', 'codigo']


@admin.register(SesionClase)
class SesionClaseAdmin(admin.ModelAdmin):
    list_display = ['curso', 'seccion', 'profesor', 'fecha', 'hora_inicio', 'cerrada']
    list_filter = ['cerrada', 'fecha', 'curso']
    search_fields = ['curso__nombre', 'seccion__nombre']
    date_hierarchy = 'fecha'


@admin.register(Asistencia)
class AsistenciaAdmin(admin.ModelAdmin):
    list_display = ['alumno', 'sesion', 'estado', 'hora_registro', 'registrado_via_qr']
    list_filter = ['estado', 'registrado_via_qr', 'sesion__fecha']
    search_fields = ['alumno__nombres', 'alumno__apellidos', 'alumno__codigo']
