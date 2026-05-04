from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils import timezone

from config.permissions import EsAdminOProfesor
from .models import PermisoSalida
from .serializers import PermisoSalidaSerializer, RevisionPermisoSerializer


class PermisoSalidaViewSet(viewsets.ModelViewSet):
    serializer_class = PermisoSalidaSerializer

    def get_queryset(self):
        user = self.request.user
        queryset = PermisoSalida.objects.select_related('alumno', 'solicitante', 'revisado_por')

        if user.rol == 'padre':
            queryset = queryset.filter(solicitante=user)
        
        estado = self.request.query_params.get('estado')
        if estado:
            queryset = queryset.filter(estado=estado)

        return queryset

    def get_permissions(self):
        if self.action == 'revisar':
            return [EsAdminOProfesor()]
        return super().get_permissions()

    @action(detail=True, methods=['post'])
    def revisar(self, request, pk=None):
        permiso = self.get_object()

        if permiso.estado != 'pendiente':
            return Response(
                {'success': False, 'message': 'Este permiso ya fue revisado'},
                status=status.HTTP_400_BAD_REQUEST
            )

        serializer = RevisionPermisoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        permiso.estado = serializer.validated_data['estado']
        permiso.revisado_por = request.user
        permiso.observacion_revision = serializer.validated_data.get('observacion', '')
        permiso.fecha_revision = timezone.now()
        permiso.save()

        return Response({
            'success': True,
            'message': f'Permiso {permiso.get_estado_display().lower()} correctamente'
        })

    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()
        serializer = self.get_serializer(queryset, many=True)
        return Response({'success': True, 'data': serializer.data})
