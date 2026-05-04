from rest_framework import serializers
from .models import AsistenciaDocente


class AsistenciaDocenteSerializer(serializers.ModelSerializer):
    docente_nombre = serializers.CharField(source='docente.nombre_completo', read_only=True)
    docente_email = serializers.CharField(source='docente.email', read_only=True)

    class Meta:
        model = AsistenciaDocente
        fields = [
            'id', 'docente', 'docente_nombre', 'docente_email',
            'fecha', 'estado', 'hora_registro', 'registrado_via_qr', 'observacion'
        ]
