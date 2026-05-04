from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.utils import timezone
from django.http import HttpResponse
from django.db.models import Count, Q
import csv

from config.permissions import EsAdminOProfesor
from asistencia.models import Alumno, Asistencia, SesionClase
from asistencia_docente.models import AsistenciaDocente
from usuarios.models import Usuario


class DashboardView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        hoy = timezone.localdate()

        # Stats alumnos hoy
        sesiones_hoy = SesionClase.objects.filter(fecha=hoy)
        asistencias_hoy = Asistencia.objects.filter(sesion__in=sesiones_hoy)

        total_alumnos = Alumno.objects.filter(activo=True).count()
        presentes = asistencias_hoy.filter(estado='presente').count()
        tardanzas = asistencias_hoy.filter(estado='tardanza').count()
        ausentes = total_alumnos - presentes - tardanzas

        # Stats docentes hoy
        total_docentes = Usuario.objects.filter(rol='profesor', is_active=True).count()
        doc_asistencias = AsistenciaDocente.objects.filter(fecha=hoy)
        doc_presentes = doc_asistencias.filter(estado='presente').count()
        doc_tardanzas = doc_asistencias.filter(estado='tardanza').count()
        doc_ausentes = total_docentes - doc_presentes - doc_tardanzas

        # Últimas 5 asistencias registradas hoy
        ultimas = (
            Asistencia.objects
            .filter(sesion__in=sesiones_hoy, hora_registro__isnull=False)
            .select_related('alumno', 'sesion__seccion')
            .order_by('-hora_registro')[:5]
        )
        ultimas_data = [
            {
                'alumno': a.alumno.nombre_completo,
                'seccion': str(a.sesion.seccion),
                'estado': a.estado,
                'hora': a.hora_registro.astimezone(timezone.get_current_timezone()).strftime('%H:%M:%S'),
            }
            for a in ultimas
        ]

        return Response({
            'success': True,
            'data': {
                'fecha': str(hoy),
                'alumnos': {
                    'total': total_alumnos,
                    'presentes': presentes,
                    'tardanzas': tardanzas,
                    'ausentes': ausentes,
                },
                'docentes': {
                    'total': total_docentes,
                    'presentes': doc_presentes,
                    'tardanzas': doc_tardanzas,
                    'ausentes': doc_ausentes,
                },
                'ultimas_asistencias': ultimas_data,
            }
        })


class ReporteAsistenciaCSVView(APIView):
    permission_classes = [EsAdminOProfesor]

    def get(self, request):
        fecha = request.query_params.get('fecha', str(timezone.localdate()))
        seccion_id = request.query_params.get('seccion')

        sesiones = SesionClase.objects.filter(fecha=fecha)
        if seccion_id:
            sesiones = sesiones.filter(seccion_id=seccion_id)

        alumnos = Alumno.objects.filter(activo=True).select_related('seccion__grado')
        if seccion_id:
            alumnos = alumnos.filter(seccion_id=seccion_id)

        asistencias = {
            a.alumno_id: a
            for a in Asistencia.objects.filter(sesion__in=sesiones).select_related('alumno')
        }

        response = HttpResponse(content_type='text/csv; charset=utf-8')
        response['Content-Disposition'] = f'attachment; filename="asistencia_{fecha}.csv"'
        response.write('\ufeff')  # BOM para Excel

        writer = csv.writer(response)
        writer.writerow(['Codigo', 'Apellidos', 'Nombres', 'Seccion', 'Estado', 'Hora', 'Via QR'])

        for alumno in alumnos.order_by('apellidos'):
            reg = asistencias.get(alumno.id)
            hora = ''
            if reg and reg.hora_registro:
                hora = reg.hora_registro.astimezone(timezone.get_current_timezone()).strftime('%H:%M:%S')
            writer.writerow([
                alumno.codigo,
                alumno.apellidos,
                alumno.nombres,
                str(alumno.seccion),
                reg.estado if reg else 'ausente',
                hora,
                'Si' if reg and reg.registrado_via_qr else 'No',
            ])

        return response


class ReporteDocenteCSVView(APIView):
    permission_classes = [EsAdminOProfesor]

    def get(self, request):
        fecha = request.query_params.get('fecha', str(timezone.localdate()))
        docentes = Usuario.objects.filter(rol='profesor', is_active=True).order_by('apellidos')
        registros = {
            r.docente_id: r
            for r in AsistenciaDocente.objects.filter(fecha=fecha)
        }

        response = HttpResponse(content_type='text/csv; charset=utf-8')
        response['Content-Disposition'] = f'attachment; filename="asistencia_docentes_{fecha}.csv"'
        response.write('\ufeff')

        writer = csv.writer(response)
        writer.writerow(['Email', 'Apellidos', 'Nombres', 'Estado', 'Hora', 'Via QR'])

        for d in docentes:
            reg = registros.get(d.id)
            hora = ''
            if reg and reg.hora_registro:
                hora = reg.hora_registro.astimezone(timezone.get_current_timezone()).strftime('%H:%M:%S')
            writer.writerow([
                d.email, d.apellidos, d.nombres,
                reg.estado if reg else 'ausente',
                hora,
                'Si' if reg and reg.registrado_via_qr else 'No',
            ])

        return response
