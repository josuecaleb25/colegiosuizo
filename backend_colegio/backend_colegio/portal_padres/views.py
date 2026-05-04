from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.utils import timezone

from config.permissions import EsPadre
from asistencia.models import Alumno, Asistencia, SesionClase
from comunicados.models import Comunicado, LecturaComunicado
from comunicados.serializers import ComunicadoSerializer


def get_alumno_del_padre(user):
    """
    El correo del usuario padre coincide con el correo institucional del alumno.
    Ej: abadvillaneralisethsayuri@peruanosuizo.edu.pe -> alumno Liseth Sayuri Abad Villaner
    Buscamos por el prefijo del email (antes del @).
    """
    # El alumno se identifica porque su codigo o dni está asociado al email
    # La estrategia: el email del padre ES el email del alumno en el sistema
    # Buscamos el alumno cuyo email_padre (campo en Usuario) coincide
    # Como no hay campo directo, buscamos por el email del usuario en la tabla de alumnos
    # usando la convención: apellido1+apellido2+nombre1+nombre2@peruanosuizo.edu.pe
    # Pero lo más simple: guardamos el email del padre en el modelo Usuario y lo vinculamos
    # al alumno por email_padre
    # Por ahora: el alumno se busca por el campo email_padre del usuario
    try:
        return Alumno.objects.select_related('seccion__grado').get(
            email_padre=user.email, activo=True
        )
    except Alumno.DoesNotExist:
        return None


class MiHijoView(APIView):
    permission_classes = [EsPadre]

    def get(self, request):
        alumno = get_alumno_del_padre(request.user)
        if not alumno:
            return Response({'success': False, 'message': 'No se encontró alumno asociado'}, status=404)

        hoy = timezone.localdate()
        tz = timezone.get_current_timezone()
        sesiones_hoy = SesionClase.objects.filter(seccion=alumno.seccion, fecha=hoy)
        asistencia_hoy = Asistencia.objects.filter(sesion__in=sesiones_hoy, alumno=alumno).first()

        return Response({
            'success': True,
            'data': {
                'id': alumno.id,
                'codigo': alumno.codigo,
                'nombre_completo': alumno.nombre_completo,
                'seccion': str(alumno.seccion),
                'asistencia_hoy': {
                    'estado': asistencia_hoy.estado if asistencia_hoy else 'ausente',
                    'hora': asistencia_hoy.hora_registro.astimezone(tz).strftime('%H:%M:%S')
                    if asistencia_hoy and asistencia_hoy.hora_registro else None,
                },
            }
        })


class HistorialHijoView(APIView):
    permission_classes = [EsPadre]

    def get(self, request):
        alumno = get_alumno_del_padre(request.user)
        if not alumno:
            return Response({'success': False, 'message': 'No se encontró alumno asociado'}, status=404)

        asistencias = (
            Asistencia.objects
            .filter(alumno=alumno)
            .select_related('sesion__curso')
            .order_by('-sesion__fecha')[:60]
        )

        tz = timezone.get_current_timezone()
        data = [{
            'fecha': str(a.sesion.fecha),
            'curso': a.sesion.curso.nombre,
            'estado': a.estado,
            'hora': a.hora_registro.astimezone(tz).strftime('%H:%M:%S') if a.hora_registro else None,
        } for a in asistencias]

        return Response({'success': True, 'alumno': alumno.nombre_completo, 'data': data})


class ComunicadosPadreView(APIView):
    permission_classes = [EsPadre]

    def get(self, request):
        comunicados = Comunicado.objects.filter(publicado=True).order_by('-fecha_publicacion')[:20]
        serializer = ComunicadoSerializer(comunicados, many=True, context={'request': request})
        return Response({'success': True, 'data': serializer.data})
