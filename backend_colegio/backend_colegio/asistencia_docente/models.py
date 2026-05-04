from django.db import models
from django.conf import settings


class AsistenciaDocente(models.Model):
    ESTADO_CHOICES = [
        ('presente',  'Presente'),
        ('tardanza',  'Tardanza'),
        ('ausente',   'Ausente'),
        ('justificado', 'Justificado'),
    ]

    docente = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='asistencias_docente',
        limit_choices_to={'rol': 'profesor'},
    )
    fecha = models.DateField()
    estado = models.CharField(max_length=15, choices=ESTADO_CHOICES, default='ausente')
    hora_registro = models.DateTimeField(null=True, blank=True)
    registrado_via_qr = models.BooleanField(default=False)
    observacion = models.TextField(blank=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ['docente', 'fecha']
        ordering = ['-fecha', 'docente__apellidos']
        verbose_name = 'Asistencia Docente'

    def __str__(self):
        return f'{self.docente.nombre_completo} - {self.fecha} - {self.get_estado_display()}'
