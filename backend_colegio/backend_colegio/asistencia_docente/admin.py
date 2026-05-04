from django.contrib import admin
from .models import AsistenciaDocente


@admin.register(AsistenciaDocente)
class AsistenciaDocenteAdmin(admin.ModelAdmin):
    list_display = ['docente', 'fecha', 'estado', 'hora_registro', 'registrado_via_qr']
    list_filter = ['estado', 'fecha']
    search_fields = ['docente__nombres', 'docente__apellidos']
    date_hierarchy = 'fecha'
