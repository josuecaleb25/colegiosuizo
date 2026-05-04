from django.contrib import admin
from .models import PermisoSalida


@admin.register(PermisoSalida)
class PermisoSalidaAdmin(admin.ModelAdmin):
    list_display = ['alumno', 'tipo', 'fecha', 'estado', 'solicitante', 'revisado_por']
    list_filter = ['estado', 'tipo', 'fecha']
    search_fields = ['alumno__nombres', 'alumno__apellidos']
    date_hierarchy = 'fecha'
