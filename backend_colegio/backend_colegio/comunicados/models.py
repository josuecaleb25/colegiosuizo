from django.db import models
from django.conf import settings
from asistencia.models import Grado, Seccion


class Comunicado(models.Model):
    TIPO_CHOICES = [
        ('general', 'General'),
        ('grado', 'Por grado'),
        ('seccion', 'Por sección'),
    ]
    PRIORIDAD_CHOICES = [
        ('normal', 'Normal'),
        ('importante', 'Importante'),
        ('urgente', 'Urgente'),
    ]

    titulo = models.CharField(max_length=200)
    contenido = models.TextField()
    tipo = models.CharField(max_length=10, choices=TIPO_CHOICES, default='general')
    prioridad = models.CharField(max_length=12, choices=PRIORIDAD_CHOICES, default='normal')
    autor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='comunicados')
    grados = models.ManyToManyField(Grado, blank=True, related_name='comunicados')
    secciones = models.ManyToManyField(Seccion, blank=True, related_name='comunicados')
    publicado = models.BooleanField(default=True)
    fecha_publicacion = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = 'Comunicado'
        ordering = ['-fecha_publicacion']

    def __str__(self):
        return self.titulo


class LecturaComunicado(models.Model):
    comunicado = models.ForeignKey(Comunicado, on_delete=models.CASCADE, related_name='lecturas')
    usuario = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='lecturas')
    leido_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ['comunicado', 'usuario']
        verbose_name = 'Lectura de comunicado'
