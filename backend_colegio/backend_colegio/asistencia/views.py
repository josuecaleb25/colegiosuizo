from rest_framework import viewsets, status, generics
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.views import APIView
from django.utils import timezone
from datetime import timedelta

from config.permissions import EsAdminOProfesor, EsAdmin
from .models import Grado, Seccion, Alumno, Curso, SesionClase, Asistencia
from .serializers import (
    GradoSerializer, SeccionSerializer, AlumnoSerializer, CursoSerializer,
    SesionClaseSerializer, AsistenciaSerializer, AsistenciaUpdateSerializer
)
from .services import generar_token_qr, generar_imagen_qr, registrar_asistencia_qr, crear_asistencias_pendientes

VENTANA_HORAS = 8  # horas que dura visible una sesión


class GradoViewSet(viewsets.ModelViewSet):
    queryset = Grado.objects.filter(activo=True)
    serializer_class = GradoSerializer
    permission_classes = [EsAdmin]


class SeccionViewSet(viewsets.ModelViewSet):
    queryset = Seccion.objects.select_related('grado', 'tutor').filter(activa=True)
    serializer_class = SeccionSerializer
    permission_classes = [EsAdminOProfesor]


class AlumnoViewSet(viewsets.ModelViewSet):
    serializer_class = AlumnoSerializer
    permission_classes = [EsAdminOProfesor]

    def get_queryset(self):
        queryset = Alumno.objects.select_related('seccion__grado').filter(activo=True)
        seccion_id = self.request.query_params.get('seccion')
        if seccion_id:
            queryset = queryset.filter(seccion_id=seccion_id)
        return queryset


class CursoViewSet(viewsets.ModelViewSet):
    serializer_class = CursoSerializer
    permission_classes = [EsAdminOProfesor]

    def get_queryset(self):
        queryset = Curso.objects.select_related('grado', 'profesor').filter(activo=True)
        if self.request.user.rol == 'profesor':
            queryset = queryset.filter(profesor=self.request.user)
        return queryset


class SesionClaseViewSet(viewsets.ModelViewSet):
    serializer_class = SesionClaseSerializer
    permission_classes = [EsAdminOProfesor]

    def get_queryset(self):
        queryset = SesionClase.objects.select_related('curso', 'seccion', 'profesor')
        if self.request.user.rol == 'profesor':
            queryset = queryset.filter(profesor=self.request.user)
        fecha = self.request.query_params.get('fecha')
        seccion_id = self.request.query_params.get('seccion')
        if fecha:
            queryset = queryset.filter(fecha=fecha)
        if seccion_id:
            queryset = queryset.filter(seccion_id=seccion_id)
        return queryset

    @action(detail=True, methods=['post'], url_path='generar-qr')
    def generar_qr(self, request, pk=None):
        sesion = self.get_object()
        if sesion.cerrada:
            return Response(
                {'success': False, 'message': 'No se puede generar QR para una sesión cerrada'},
                status=status.HTTP_400_BAD_REQUEST
            )
        token_qr = generar_token_qr(sesion)
        imagen_base64 = generar_imagen_qr(token_qr)
        return Response({
            'success': True,
            'data': {
                'token': str(token_qr.token),
                'expira_en': token_qr.expira_en,
                'qr_image': f'data:image/png;base64,{imagen_base64}',
            }
        })

    @action(detail=True, methods=['post'], url_path='cerrar')
    def cerrar_sesion(self, request, pk=None):
        sesion = self.get_object()
        if sesion.cerrada:
            return Response({'success': False, 'message': 'La sesión ya está cerrada'}, status=status.HTTP_400_BAD_REQUEST)
        crear_asistencias_pendientes(sesion)
        try:
            sesion.token_qr.invalidar()
        except Exception:
            pass
        sesion.cerrada = True
        sesion.save(update_fields=['cerrada'])
        return Response({'success': True, 'message': 'Sesión cerrada correctamente'})

    @action(detail=True, methods=['get'], url_path='asistencias')
    def lista_asistencias(self, request, pk=None):
        sesion = self.get_object()
        asistencias = Asistencia.objects.select_related('alumno').filter(sesion=sesion)
        serializer = AsistenciaSerializer(asistencias, many=True)
        return Response({'success': True, 'data': serializer.data})


class RegistrarAsistenciaQRView(generics.GenericAPIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        token = request.data.get('token')
        alumno_id = request.data.get('alumno_id')

        if not token or not alumno_id:
            return Response(
                {'success': False, 'message': 'Se requiere token y alumno_id'},
                status=status.HTTP_400_BAD_REQUEST
            )

        resultado = registrar_asistencia_qr(token, alumno_id)
        code = status.HTTP_200_OK if resultado['success'] else status.HTTP_400_BAD_REQUEST
        return Response(resultado, status=code)


class AsistenciaViewSet(viewsets.ModelViewSet):
    serializer_class = AsistenciaSerializer
    permission_classes = [EsAdminOProfesor]

    def get_queryset(self):
        return Asistencia.objects.select_related('alumno', 'sesion__curso').all()

    def get_serializer_class(self):
        if self.action in ('update', 'partial_update'):
            return AsistenciaUpdateSerializer
        return AsistenciaSerializer

    def update(self, request, *args, **kwargs):
        kwargs['partial'] = True
        return super().update(request, *args, **kwargs)


class EscanearQRAlumnoView(APIView):
    permission_classes = [AllowAny]

    # Ventana de entrada: 7:00 - 7:31 = presente, después = tardanza
    HORA_INICIO_ENTRADA = (7, 0)
    HORA_LIMITE_PUNTUAL = (7, 31)
    HORA_FIN_ENTRADA    = (17, 0)  # hasta las 5pm

    def post(self, request):
        qr_token = request.data.get('qr_token')
        if not qr_token:
            return Response({'success': False, 'message': 'Se requiere qr_token'}, status=status.HTTP_400_BAD_REQUEST)

        try:
            alumno = Alumno.objects.select_related('seccion').get(qr_token=qr_token, activo=True)
        except Alumno.DoesNotExist:
            return Response({'success': False, 'message': 'QR no válido'}, status=status.HTTP_404_NOT_FOUND)

        tz = timezone.get_current_timezone()
        ahora = timezone.now().astimezone(tz)
        hoy = ahora.date()
        hora_actual = (ahora.hour, ahora.minute)

        # Determinar estado según hora (sin restricción de horario por ahora)
        if hora_actual <= self.HORA_LIMITE_PUNTUAL:
            estado = 'presente'
        else:
            estado = 'tardanza'

        sesion = SesionClase.objects.filter(
            seccion=alumno.seccion,
            fecha=hoy,
            cerrada=False
        ).order_by('hora_inicio').first()

        if not sesion:
            return Response({'success': False, 'message': 'No hay sesión activa hoy para este alumno'}, status=status.HTTP_404_NOT_FOUND)

        asistencia, creada = Asistencia.objects.get_or_create(
            sesion=sesion,
            alumno=alumno,
            defaults={'estado': estado, 'hora_registro': ahora, 'registrado_via_qr': True}
        )

        if not creada:
            hora_str = asistencia.hora_registro.astimezone(tz).strftime('%H:%M:%S') if asistencia.hora_registro else '—'
            return Response({
                'success': False,
                'message': f'Asistencia ya registrada a las {hora_str}',
                'data': {
                    'alumno': alumno.nombre_completo,
                    'codigo': alumno.codigo,
                    'estado': asistencia.estado,
                    'hora_registro': hora_str,
                }
            }, status=status.HTTP_200_OK)

        hora_str = ahora.strftime('%H:%M:%S')
        return Response({
            'success': True,
            'message': 'Asistencia registrada',
            'data': {
                'alumno': alumno.nombre_completo,
                'codigo': alumno.codigo,
                'grado': str(alumno.seccion),
                'curso': sesion.curso.nombre,
                'estado': estado,
                'hora_registro': hora_str,
                'fecha': str(hoy),
            }
        }, status=status.HTTP_200_OK)

        if not creada:
            hora_str = asistencia.hora_registro.astimezone(tz).strftime('%H:%M:%S') if asistencia.hora_registro else '—'
            return Response({
                'success': False,
                'message': f'Asistencia ya registrada a las {hora_str}',
                'data': {
                    'alumno': alumno.nombre_completo,
                    'codigo': alumno.codigo,
                    'estado': asistencia.estado,
                    'hora_registro': hora_str,
                }
            }, status=status.HTTP_200_OK)

        hora_str = ahora.astimezone(tz).strftime('%H:%M:%S')
        return Response({
            'success': True,
            'message': 'Asistencia registrada',
            'data': {
                'alumno': alumno.nombre_completo,
                'codigo': alumno.codigo,
                'grado': str(alumno.seccion),
                'curso': sesion.curso.nombre,
                'estado': 'presente',
                'hora_registro': hora_str,
                'fecha': str(hoy),
            }
        }, status=status.HTTP_200_OK)


class EstadoSesionHoyView(APIView):
    """
    Devuelve el estado de las sesiones del día:
    activa, expirada (pasaron 8h) o sin_sesion.
    El frontend usa esto para saber si mostrar o no la tabla.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request):
        hoy = timezone.localdate()
        ahora = timezone.now()
        tz = timezone.get_current_timezone()

        sesiones = SesionClase.objects.filter(fecha=hoy).select_related('seccion__grado', 'curso')
        resultado = []

        for sesion in sesiones:
            inicio = timezone.make_aware(
                timezone.datetime(
                    sesion.fecha.year, sesion.fecha.month, sesion.fecha.day,
                    sesion.hora_inicio.hour, sesion.hora_inicio.minute
                )
            )
            fin_ventana = inicio + timedelta(hours=VENTANA_HORAS)
            if ahora > fin_ventana:
                estado_ventana = 'expirada'
            elif sesion.cerrada:
                estado_ventana = 'cerrada'
            else:
                estado_ventana = 'activa'

            resultado.append({
                'sesion_id': sesion.id,
                'seccion_id': sesion.seccion.id,
                'seccion_nombre': str(sesion.seccion),
                'curso': sesion.curso.nombre,
                'hora_inicio': sesion.hora_inicio.strftime('%H:%M'),
                'fin_ventana': fin_ventana.astimezone(tz).strftime('%H:%M'),
                'estado': estado_ventana,
            })

        return Response({'success': True, 'data': resultado})
