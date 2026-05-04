from rest_framework import generics, status
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from django.utils import timezone

from config.permissions import EsAdmin, EsAdminOProfesor
from usuarios.models import Usuario
from .models import AsistenciaDocente
from .serializers import AsistenciaDocenteSerializer


class AsistenciaDocenteListView(generics.ListAPIView):
    serializer_class = AsistenciaDocenteSerializer
    permission_classes = [EsAdminOProfesor]

    def get_queryset(self):
        qs = AsistenciaDocente.objects.select_related('docente').all()
        fecha = self.request.query_params.get('fecha')
        if fecha:
            qs = qs.filter(fecha=fecha)
        else:
            qs = qs.filter(fecha=timezone.localdate())
        return qs

    def list(self, request, *args, **kwargs):
        # Devuelve todos los docentes con su estado del día
        fecha_str = request.query_params.get('fecha')
        fecha = fecha_str if fecha_str else str(timezone.localdate())

        docentes = Usuario.objects.filter(rol='profesor', is_active=True).order_by('apellidos')
        registros = {
            r.docente_id: r
            for r in AsistenciaDocente.objects.filter(fecha=fecha).select_related('docente')
        }

        data = []
        for d in docentes:
            reg = registros.get(d.id)
            data.append({
                'id': d.id,
                'docente_nombre': d.nombre_completo,
                'docente_email': d.email,
                'fecha': fecha,
                'estado': reg.estado if reg else 'ausente',
                'hora_registro': reg.hora_registro.isoformat() if reg and reg.hora_registro else None,
                'registrado_via_qr': reg.registrado_via_qr if reg else False,
            })

        return Response({'success': True, 'data': data})


class EscanearQRDocenteView(APIView):
    permission_classes = [AllowAny]

    HORA_INICIO    = (7, 0)
    HORA_PUNTUAL   = (7, 31)
    HORA_FIN       = (15, 0)

    def post(self, request):
        qr_token = request.data.get('qr_token')
        if not qr_token:
            return Response({'success': False, 'message': 'Se requiere qr_token'}, status=status.HTTP_400_BAD_REQUEST)

        try:
            docente = Usuario.objects.get(qr_token=qr_token, rol='profesor', is_active=True)
        except Usuario.DoesNotExist:
            return Response({'success': False, 'message': 'QR no válido'}, status=status.HTTP_404_NOT_FOUND)

        tz = timezone.get_current_timezone()
        ahora = timezone.now().astimezone(tz)
        hoy = ahora.date()
        hora_actual = (ahora.hour, ahora.minute)

        estado = 'presente' if hora_actual <= self.HORA_PUNTUAL else 'tardanza'

        asistencia, creada = AsistenciaDocente.objects.get_or_create(
            docente=docente,
            fecha=hoy,
            defaults={'estado': estado, 'hora_registro': ahora, 'registrado_via_qr': True}
        )

        if not creada:
            hora_str = asistencia.hora_registro.astimezone(tz).strftime('%H:%M:%S') if asistencia.hora_registro else '—'
            return Response({
                'success': False,
                'message': f'Asistencia ya registrada a las {hora_str}',
                'data': {'docente': docente.nombre_completo, 'estado': asistencia.estado, 'hora_registro': hora_str}
            })

        return Response({
            'success': True,
            'message': 'Asistencia registrada',
            'data': {
                'docente': docente.nombre_completo,
                'email': docente.email,
                'estado': estado,
                'hora_registro': ahora.strftime('%H:%M:%S'),
                'fecha': str(hoy),
            }
        })
