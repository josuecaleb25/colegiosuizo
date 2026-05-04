from django.contrib import admin
from .models import Comunicado, LecturaComunicado


@admin.register(Comunicado)
class ComunicadoAdmin(admin.ModelAdmin):
    list_display = ['titulo', 'tipo', 'prioridad', 'autor', 'publicado', 'fecha_publicacion']
    list_filter = ['tipo', 'prioridad', 'publicado']
    search_fields = ['titulo', 'contenido']
    filter_horizontal = ['grados', 'secciones']
