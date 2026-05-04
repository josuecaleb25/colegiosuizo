from rest_framework import serializers
from django.utils import timezone
from .models import PermisoSalida


class PermisoSalidaSerializer(serializers.ModelSerializer):
    alumno_nombre = serializers.CharField(source='alumno.nombre_completo', read_only=True)
    solicitante_nombre = serializers.CharField(source='solicitante.nombre_completo', read_only=True)
    revisado_por_nombre = serializers.CharField(source='revisado_por.nombre_completo', read_only=True)

    class Meta:
        model = PermisoSalida
        fields = [
            'id', 'alumno', 'alumno_nombre', 'solicitante', 'solicitante_nombre',
            'tipo', 'motivo', 'fecha', 'estado', 'revisado_por', 'revisado_por_nombre',
            'observacion_revision', 'fecha_solicitud', 'fecha_revision'
        ]
        read_only_fields = ['solicitante', 'estado', 'revisado_por', 'fecha_revision']

    def create(self, validated_data):
        validated_data['solicitante'] = self.context['request'].user
        return super().create(validated_data)


class RevisionPermisoSerializer(serializers.Serializer):
    estado = serializers.ChoiceField(choices=['aprobado', 'rechazado'])
    observacion = serializers.CharField(required=False, allow_blank=True)
