from django.db import models
from django.conf import settings
from asistencia.models import Alumno


class PermisoSalida(models.Model):
    ESTADO_CHOICES = [
        ('pendiente', 'Pendiente'),
        ('aprobado', 'Aprobado'),
        ('rechazado', 'Rechazado'),
    ]
    TIPO_CHOICES = [
        ('salida_anticipada', 'Salida anticipada'),
        ('tardanza_justificada', 'Tardanza justificada'),
        ('ausencia_justificada', 'Ausencia justificada'),
    ]

    alumno = models.ForeignKey(Alumno, on_delete=models.CASCADE, related_name='permisos')
    solicitante = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE,
        related_name='permisos_solicitados'
    )
    tipo = models.CharField(max_length=25, choices=TIPO_CHOICES)
    motivo = models.TextField()
    fecha = models.DateField()
    estado = models.CharField(max_length=10, choices=ESTADO_CHOICES, default='pendiente')
    revisado_por = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL,
        null=True, blank=True, related_name='permisos_revisados'
    )
    observacion_revision = models.TextField(blank=True)
    fecha_solicitud = models.DateTimeField(auto_now_add=True)
    fecha_revision = models.DateTimeField(null=True, blank=True)

    class Meta:
        verbose_name = 'Permiso de salida'
        verbose_name_plural = 'Permisos de salida'
        ordering = ['-fecha_solicitud']

    def __str__(self):
        return f'{self.alumno} - {self.get_tipo_display()} - {self.fecha}'
