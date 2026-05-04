from rest_framework import serializers
from usuarios.models import Usuario
from asistencia.models import Alumno, Seccion, Grado, Curso, Asistencia, SesionClase


class MobileLoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField()


class MobileUserSerializer(serializers.ModelSerializer):
    nombre_completo = serializers.ReadOnlyField()
    
    class Meta:
        model = Usuario
        fields = ['id', 'email', 'nombres', 'apellidos', 'nombre_completo', 'rol', 'telefono']


class MobileAlumnoAsistenciaSerializer(serializers.ModelSerializer):
    salon = serializers.SerializerMethodField()
    hora_entrada = serializers.SerializerMethodField()
    estado_entrada = serializers.SerializerMethodField()
    
    class Meta:
        model = Alumno
        fields = ['id', 'nombre_completo', 'salon', 'hora_entrada', 'estado_entrada']
    
    def get_salon(self, obj):
        # Retornar formato simple: "1ro A"
        if obj.seccion:
            return f"{obj.seccion.grado.nombre} {obj.seccion.nombre}"
        return ""
    
    def get_hora_entrada(self, obj):
        # Buscar asistencia de hoy
        from django.utils import timezone
        hoy = timezone.localdate()
        try:
            asistencia = Asistencia.objects.filter(
                alumno=obj,
                sesion__fecha=hoy,
                estado__in=['presente', 'tardanza']
            ).first()
            if asistencia and asistencia.hora_registro:
                return asistencia.hora_registro.strftime('%I:%M %p').lower()
            return None
        except:
            return None
    
    def get_estado_entrada(self, obj):
        from django.utils import timezone
        hoy = timezone.localdate()
        try:
            asistencia = Asistencia.objects.filter(
                alumno=obj,
                sesion__fecha=hoy
            ).first()
            return asistencia.estado if asistencia else 'ausente'
        except:
            return 'ausente'


class MobileCursoSerializer(serializers.ModelSerializer):
    grado_nombre = serializers.CharField(source='grado.nombre', read_only=True)
    profesor_nombre = serializers.CharField(source='profesor.nombre_completo', read_only=True)
    
    class Meta:
        model = Curso
        fields = ['id', 'nombre', 'codigo', 'grado_nombre', 'profesor_nombre']


class MobileSeccionSerializer(serializers.ModelSerializer):
    grado_nombre = serializers.CharField(source='grado.nombre', read_only=True)
    
    class Meta:
        model = Seccion
        fields = ['id', 'nombre', 'grado_nombre']