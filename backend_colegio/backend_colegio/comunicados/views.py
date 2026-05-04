from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response

from config.permissions import EsAdminOProfesor
from .models import Comunicado, LecturaComunicado
from .serializers import ComunicadoSerializer


class ComunicadoViewSet(viewsets.ModelViewSet):
    serializer_class = ComunicadoSerializer

    def get_queryset(self):
        user = self.request.user
        queryset = Comunicado.objects.filter(publicado=True).prefetch_related('grados', 'secciones')

        if user.rol == 'padre':
            # El padre ve comunicados generales o de la sección de su hijo
            # (esto se expande cuando se agregue el módulo de padres)
            queryset = queryset.filter(tipo='general')

        prioridad = self.request.query_params.get('prioridad')
        if prioridad:
            queryset = queryset.filter(prioridad=prioridad)

        return queryset

    def get_permissions(self):
        if self.action in ('create', 'update', 'partial_update', 'destroy'):
            return [EsAdminOProfesor()]
        return super().get_permissions()

    @action(detail=True, methods=['post'], url_path='marcar-leido')
    def marcar_leido(self, request, pk=None):
        comunicado = self.get_object()
        LecturaComunicado.objects.get_or_create(comunicado=comunicado, usuario=request.user)
        return Response({'success': True, 'message': 'Comunicado marcado como leído'})

    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()
        serializer = self.get_serializer(queryset, many=True)
        return Response({'success': True, 'data': serializer.data})

    def retrieve(self, request, *args, **kwargs):
        instance = self.get_object()
        serializer = self.get_serializer(instance)
        return Response({'success': True, 'data': serializer.data})
