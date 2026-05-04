import uuid
from django.db import models
from django.conf import settings
from django.utils import timezone


class Grado(models.Model):
    nombre = models.CharField(max_length=50)
    nivel = models.CharField(max_length=20, choices=[('primaria', 'Primaria'), ('secundaria', 'Secundaria')])
    activo = models.BooleanField(default=True)

    class Meta:
        verbose_name = 'Grado'
        ordering = ['nivel', 'nombre']

    def __str__(self):
        return f'{self.nombre} - {self.get_nivel_display()}'


class Seccion(models.Model):
    grado = models.ForeignKey(Grado, on_delete=models.CASCADE, related_name='secciones')
    nombre = models.CharField(max_length=10)
    tutor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True, blank=True,
        limit_choices_to={'rol': 'profesor'},
        related_name='secciones_tutor'
    )
    activa = models.BooleanField(default=True)

    class Meta:
        verbose_name = 'Sección'
        unique_together = ['grado', 'nombre']
        ordering = ['grado', 'nombre']

    def __str__(self):
        return f'{self.grado} - {self.nombre}'


class Alumno(models.Model):
    codigo = models.CharField(max_length=20, unique=True)
    nombres = models.CharField(max_length=100)
    apellidos = models.CharField(max_length=100)
    dni = models.CharField(max_length=8, unique=True)
    seccion = models.ForeignKey(Seccion, on_delete=models.PROTECT, related_name='alumnos')
    fecha_nacimiento = models.DateField()
    qr_token = models.UUIDField(default=uuid.uuid4, unique=True, editable=False)
    email_padre = models.EmailField(unique=True, null=True, blank=True)
    activo = models.BooleanField(default=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name = 'Alumno'
        ordering = ['apellidos', 'nombres']

    def __str__(self):
        return f'{self.apellidos}, {self.nombres} ({self.codigo})'

    @property
    def nombre_completo(self):
        return f'{self.nombres} {self.apellidos}'


class Curso(models.Model):
    nombre = models.CharField(max_length=100)
    codigo = models.CharField(max_length=10, unique=True)
    grado = models.ForeignKey(Grado, on_delete=models.CASCADE, related_name='cursos')
    profesor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        limit_choices_to={'rol': 'profesor'},
        related_name='cursos'
    )
    activo = models.BooleanField(default=True)

    class Meta:
        verbose_name = 'Curso'
        ordering = ['nombre']

    def __str__(self):
        return f'{self.nombre} - {self.grado}'


class SesionClase(models.Model):
    curso = models.ForeignKey(Curso, on_delete=models.CASCADE, related_name='sesiones')
    seccion = models.ForeignKey(Seccion, on_delete=models.CASCADE, related_name='sesiones')
    profesor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='sesiones_dictadas'
    )
    fecha = models.DateField()
    hora_inicio = models.TimeField()
    hora_fin = models.TimeField(null=True, blank=True)
    descripcion = models.CharField(max_length=200, blank=True)
    cerrada = models.BooleanField(default=False)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name = 'Sesión de clase'
        ordering = ['-fecha', '-hora_inicio']

    def __str__(self):
        return f'{self.curso} - {self.seccion} - {self.fecha}'


class TokenQR(models.Model):
    token = models.UUIDField(default=uuid.uuid4, unique=True, editable=False)
    sesion = models.OneToOneField(SesionClase, on_delete=models.CASCADE, related_name='token_qr')
    creado_en = models.DateTimeField(auto_now_add=True)
    expira_en = models.DateTimeField()
    activo = models.BooleanField(default=True)

    class Meta:
        verbose_name = 'Token QR'

    def __str__(self):
        return f'QR {self.token} - {self.sesion}'

    @property
    def esta_vigente(self):
        return self.activo and timezone.now() <= self.expira_en

    def invalidar(self):
        self.activo = False
        self.save(update_fields=['activo'])


class Asistencia(models.Model):
    ESTADO_CHOICES = [
        ('presente', 'Presente'),
        ('tardanza', 'Tardanza'),
        ('ausente', 'Ausente'),
        ('justificado', 'Justificado'),
    ]

    sesion = models.ForeignKey(SesionClase, on_delete=models.CASCADE, related_name='asistencias')
    alumno = models.ForeignKey(Alumno, on_delete=models.CASCADE, related_name='asistencias')
    estado = models.CharField(max_length=15, choices=ESTADO_CHOICES, default='ausente')
    hora_registro = models.DateTimeField(null=True, blank=True)
    registrado_via_qr = models.BooleanField(default=False)
    observacion = models.TextField(blank=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = 'Asistencia'
        unique_together = ['sesion', 'alumno']
        ordering = ['-sesion__fecha']

    def __str__(self):
        return f'{self.alumno} - {self.sesion} - {self.get_estado_display()}'
