from rest_framework import serializers
from .models import Comunicado, LecturaComunicado


class ComunicadoSerializer(serializers.ModelSerializer):
    autor_nombre = serializers.CharField(source='autor.nombre_completo', read_only=True)
    leido = serializers.SerializerMethodField()
    total_lecturas = serializers.SerializerMethodField()

    class Meta:
        model = Comunicado
        fields = [
            'id', 'titulo', 'contenido', 'tipo', 'prioridad',
            'autor', 'autor_nombre', 'grados', 'secciones',
            'publicado', 'fecha_publicacion', 'leido', 'total_lecturas'
        ]
        read_only_fields = ['autor']

    def get_leido(self, obj):
        request = self.context.get('request')
        if not request:
            return False
        return LecturaComunicado.objects.filter(comunicado=obj, usuario=request.user).exists()

    def get_total_lecturas(self, obj):
        return obj.lecturas.count()

    def create(self, validated_data):
        validated_data['autor'] = self.context['request'].user
        return super().create(validated_data)
