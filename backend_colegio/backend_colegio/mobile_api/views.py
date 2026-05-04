from rest_framework import status, generics
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import authenticate
from django.utils import timezone
from django.db.models import Q

from usuarios.models import Usuario
from asistencia.models import Alumno, Seccion, Curso, Asistencia, SesionClase
from .serializers import (
    MobileLoginSerializer, MobileUserSerializer, MobileAlumnoAsistenciaSerializer,
    MobileCursoSerializer, MobileSeccionSerializer
)


# Endpoint de prueba
@api_view(['GET'])
@permission_classes([AllowAny])
def test_endpoint(request):
    return Response({'success': True, 'message': 'Endpoint funcionando sin autenticación'})


# Endpoint de prueba para alumnos
@api_view(['GET'])
@permission_classes([AllowAny])
def test_alumnos(request):
    from asistencia.models import Alumno
    alumnos = Alumno.objects.filter(activo=True).select_related('seccion__grado')[:10]
    data = []
    for a in alumnos:
        data.append({
            'nombre': a.nombre_completo,
            'salon': f"{a.seccion.grado.nombre} {a.seccion.nombre}" if a.seccion else ""
        })
    return Response({'success': True, 'total': len(data), 'data': data})


@api_view(['POST'])
@permission_classes([AllowAny])
def mobile_login(request):
    """
    Endpoint para login desde la app móvil
    POST /api/mobile/auth/login/
    """
    serializer = MobileLoginSerializer(data=request.data)
    if not serializer.is_valid():
        return Response({
            'success': False,
            'message': 'Datos inválidos',
            'errors': serializer.errors
        }, status=status.HTTP_400_BAD_REQUEST)
    
    email = serializer.validated_data['email']
    password = serializer.validated_data['password']
    
    user = authenticate(email=email, password=password)
    if not user:
        return Response({
            'success': False,
            'message': 'Credenciales incorrectas'
        }, status=status.HTTP_401_UNAUTHORIZED)
    
    if not user.is_active:
        return Response({
            'success': False,
            'message': 'Usuario inactivo'
        }, status=status.HTTP_401_UNAUTHORIZED)
    
    # Generar tokens
    refresh = RefreshToken.for_user(user)
    
    return Response({
        'success': True,
        'message': 'Login exitoso',
        'data': {
            'user': MobileUserSerializer(user).data,
            'tokens': {
                'access': str(refresh.access_token),
                'refresh': str(refresh)
            }
        }
    })


@api_view(['POST'])
@permission_classes([AllowAny])
def mobile_register(request):
    """
    Endpoint para registro desde la app móvil
    POST /api/mobile/auth/register/
    """
    # Por ahora solo permitir registro de padres
    data = request.data.copy()
    data['rol'] = 'padre'
    
    serializer = MobileUserSerializer(data=data)
    if not serializer.is_valid():
        return Response({
            'success': False,
            'message': 'Datos inválidos',
            'errors': serializer.errors
        }, status=status.HTTP_400_BAD_REQUEST)
    
    # Crear usuario
    user = Usuario.objects.create_user(
        email=data['email'],
        password=data['password'],
        nombres=data['nombres'],
        apellidos=data['apellidos'],
        rol='padre',
        telefono=data.get('telefono', '')
    )
    
    return Response({
        'success': True,
        'message': 'Usuario registrado exitosamente',
        'data': MobileUserSerializer(user).data
    })


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def mobile_profile(request):
    """
    Endpoint para obtener perfil del usuario
    GET /api/mobile/profile/
    """
    return Response({
        'success': True,
        'data': MobileUserSerializer(request.user).data
    })


@api_view(['GET'])
@permission_classes([AllowAny])
def mobile_asistencia_alumnos(request):
    """
    Endpoint para obtener lista de alumnos con asistencia
    GET /api/mobile/asistencia/alumnos/
    Query params: salon, search
    """
    salon = request.GET.get('salon')
    search = request.GET.get('search', '').strip()
    
    # Base queryset
    queryset = Alumno.objects.filter(activo=True).select_related('seccion__grado')
    
    # Filtrar por salón si se especifica
    if salon and salon != 'Salon':
        # Extraer solo la letra del salón (ej: "1ro C" -> "C", "C" -> "C")
        salon_letra = salon.strip().split()[-1] if ' ' in salon else salon
        queryset = queryset.filter(seccion__nombre__iexact=salon_letra)
    
    # Filtrar por búsqueda
    if search:
        queryset = queryset.filter(
            Q(nombres__icontains=search) | 
            Q(apellidos__icontains=search)
        )
    
    # Si no hay filtros específicos, mostrar todos los alumnos (no solo los con asistencia)
    # Esto permite que la app móvil muestre la lista completa y maneje la lógica de filtrado
    
    # Limitar resultados para mejor rendimiento
    queryset = queryset.order_by('seccion__nombre', 'apellidos', 'nombres')[:200]
    
    serializer = MobileAlumnoAsistenciaSerializer(queryset, many=True)
    
    return Response({
        'success': True,
        'data': serializer.data,
        'total': len(serializer.data),
        'message': f'Se encontraron {len(serializer.data)} alumnos'
    })


@api_view(['GET'])
@permission_classes([AllowAny])  # Permitir acceso sin autenticación para pruebas
def mobile_salones(request):
    """
    Endpoint para obtener lista de salones/secciones
    GET /api/mobile/salones/
    """
    secciones = Seccion.objects.filter(activa=True).select_related('grado')
    serializer = MobileSeccionSerializer(secciones, many=True)
    
    return Response({
        'success': True,
        'data': serializer.data
    })


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def mobile_cursos(request):
    """
    Endpoint para obtener lista de cursos
    GET /api/mobile/cursos/
    """
    cursos = Curso.objects.filter(activo=True).select_related('grado', 'profesor')
    serializer = MobileCursoSerializer(cursos, many=True)
    
    return Response({
        'success': True,
        'data': serializer.data
    })


@api_view(['POST'])
@permission_classes([AllowAny])
def mobile_escanear_qr(request):
    """
    Endpoint para escanear QR desde la app móvil
    POST /api/mobile/asistencia/escanear-qr/
    """
    qr_token = request.data.get('qr_token')
    if not qr_token:
        return Response({
            'success': False,
            'message': 'Se requiere qr_token'
        }, status=status.HTTP_400_BAD_REQUEST)
    
    # Intentar primero como alumno
    try:
        alumno = Alumno.objects.get(qr_token=qr_token, activo=True)
        
        # Registrar asistencia
        hoy = timezone.localdate()
        ahora = timezone.now()
        
        # Buscar sesión activa
        sesion = SesionClase.objects.filter(
            seccion=alumno.seccion,
            fecha=hoy,
            cerrada=False
        ).first()
        
        if not sesion:
            return Response({
                'success': False,
                'message': 'No hay sesión activa para este alumno'
            })
        
        # Determinar estado según hora
        hora_actual = (ahora.hour, ahora.minute)
        if hora_actual <= (7, 31):
            estado = 'presente'
        else:
            estado = 'tardanza'
        
        # Crear o actualizar asistencia
        asistencia, creada = Asistencia.objects.get_or_create(
            sesion=sesion,
            alumno=alumno,
            defaults={
                'estado': estado,
                'hora_registro': ahora,
                'registrado_via_qr': True
            }
        )
        
        if not creada:
            return Response({
                'success': False,
                'message': f'Asistencia ya registrada a las {asistencia.hora_registro.strftime("%H:%M")}'
            })
        
        return Response({
            'success': True,
            'message': 'Asistencia registrada exitosamente',
            'data': {
                'alumno': alumno.nombre_completo,
                'salon': str(alumno.seccion),
                'estado': estado,
                'hora': ahora.strftime('%I:%M %p').lower()
            }
        })
        
    except Alumno.DoesNotExist:
        # Intentar como docente
        try:
            from usuarios.models import Usuario
            from asistencia_docente.models import AsistenciaDocente
            
            docente = Usuario.objects.get(qr_token=qr_token, rol='profesor', is_active=True)
            
            hoy = timezone.localdate()
            ahora = timezone.now()
            
            hora_actual = (ahora.hour, ahora.minute)
            estado = 'presente' if hora_actual <= (7, 31) else 'tardanza'
            
            asistencia, creada = AsistenciaDocente.objects.get_or_create(
                docente=docente,
                fecha=hoy,
                defaults={
                    'estado': estado,
                    'hora_registro': ahora,
                    'registrado_via_qr': True
                }
            )
            
            if not creada:
                return Response({
                    'success': False,
                    'message': f'Asistencia ya registrada a las {asistencia.hora_registro.strftime("%H:%M")}'
                })
            
            return Response({
                'success': True,
                'message': 'Asistencia de docente registrada',
                'data': {
                    'docente': docente.nombre_completo,
                    'estado': estado,
                    'hora': ahora.strftime('%I:%M %p').lower()
                }
            })
            
        except Usuario.DoesNotExist:
            return Response({
                'success': False,
                'message': 'QR no válido'
            }, status=status.HTTP_404_NOT_FOUND)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def mobile_dashboard_stats(request):
    """
    Endpoint para obtener estadísticas del dashboard
    GET /api/mobile/dashboard/stats/
    """
    from django.db.models import Count, Q
    
    hoy = timezone.localdate()
    
    # Estadísticas de alumnos
    total_alumnos = Alumno.objects.filter(activo=True).count()
    asistencias_hoy = Asistencia.objects.filter(sesion__fecha=hoy)
    
    alumnos_stats = {
        'total': total_alumnos,
        'presentes': asistencias_hoy.filter(estado='presente').count(),
        'tardanzas': asistencias_hoy.filter(estado='tardanza').count(),
        'ausentes': total_alumnos - asistencias_hoy.filter(estado__in=['presente', 'tardanza']).count()
    }
    
    # Estadísticas de docentes
    from asistencia_docente.models import AsistenciaDocente
    total_docentes = Usuario.objects.filter(rol='profesor', is_active=True).count()
    asistencias_docentes_hoy = AsistenciaDocente.objects.filter(fecha=hoy)
    
    docentes_stats = {
        'total': total_docentes,
        'presentes': asistencias_docentes_hoy.filter(estado='presente').count(),
        'tardanzas': asistencias_docentes_hoy.filter(estado='tardanza').count(),
        'ausentes': total_docentes - asistencias_docentes_hoy.filter(estado__in=['presente', 'tardanza']).count()
    }
    
    # Estadísticas adicionales
    total_secciones = Seccion.objects.filter(activa=True).count()
    total_cursos = Curso.objects.filter(activo=True).count()
    
    return Response({
        'success': True,
        'data': {
            'fecha': hoy.isoformat(),
            'alumnos': alumnos_stats,
            'docentes': docentes_stats,
            'resumen': {
                'total_secciones': total_secciones,
                'total_cursos': total_cursos,
                'porcentaje_asistencia': round((alumnos_stats['presentes'] + alumnos_stats['tardanzas']) / max(total_alumnos, 1) * 100, 1)
            }
        }
    })

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def mobile_historial_asistencia(request, alumno_id):
    """
    Endpoint para obtener historial de asistencia de un alumno
    GET /api/mobile/asistencia/historial/{alumno_id}/
    """
    try:
        # Verificar permisos
        if request.user.rol not in ['administrador', 'profesor']:
            return Response({
                'success': False,
                'message': 'No tienes permisos para ver esta información'
            }, status=status.HTTP_403_FORBIDDEN)
        
        # Verificar que el alumno existe
        try:
            alumno = Alumno.objects.get(id=alumno_id, activo=True)
        except Alumno.DoesNotExist:
            return Response({
                'success': False,
                'message': 'Alumno no encontrado'
            }, status=status.HTTP_404_NOT_FOUND)
        
        # Obtener parámetros de consulta
        dias = int(request.GET.get('dias', 30))  # Últimos 30 días por defecto
        fecha_desde = timezone.localdate() - timezone.timedelta(days=dias)
        
        # Obtener historial de asistencia
        asistencias = Asistencia.objects.filter(
            alumno=alumno,
            sesion__fecha__gte=fecha_desde
        ).select_related('sesion', 'sesion__curso').order_by('-sesion__fecha')
        
        # Serializar datos
        historial = []
        for asistencia in asistencias:
            historial.append({
                'fecha': asistencia.sesion.fecha.isoformat(),
                'curso': asistencia.sesion.curso.nombre,
                'estado': asistencia.estado,
                'hora_registro': asistencia.hora_registro.strftime('%I:%M %p').lower() if asistencia.hora_registro else None,
                'registrado_via_qr': asistencia.registrado_via_qr,
                'observacion': asistencia.observacion
            })
        
        # Estadísticas del período
        total_dias = asistencias.count()
        presentes = asistencias.filter(estado='presente').count()
        tardanzas = asistencias.filter(estado='tardanza').count()
        ausentes = asistencias.filter(estado='ausente').count()
        justificados = asistencias.filter(estado='justificado').count()
        
        return Response({
            'success': True,
            'data': {
                'alumno': {
                    'id': alumno.id,
                    'nombre_completo': alumno.nombre_completo,
                    'codigo': alumno.codigo,
                    'seccion': str(alumno.seccion)
                },
                'periodo': {
                    'fecha_desde': fecha_desde.isoformat(),
                    'fecha_hasta': timezone.localdate().isoformat(),
                    'dias_consultados': dias
                },
                'estadisticas': {
                    'total_registros': total_dias,
                    'presentes': presentes,
                    'tardanzas': tardanzas,
                    'ausentes': ausentes,
                    'justificados': justificados,
                    'porcentaje_asistencia': round((presentes + tardanzas) / max(total_dias, 1) * 100, 1)
                },
                'historial': historial
            }
        })
        
    except Exception as e:
        return Response({
            'success': False,
            'message': f'Error al obtener historial: {str(e)}'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@permission_classes([AllowAny])
def mobile_alumnos_all(request):
    """Obtener todos los alumnos de todas las secciones con sus QR"""
    try:
        # Comentado temporalmente para desarrollo - permitir acceso sin autenticación
        # if request.user.is_authenticated and request.user.rol not in ['administrador', 'profesor']:
        #     return Response({
        #         'success': False,
        #         'message': 'No tienes permisos para ver esta información'
        #     }, status=status.HTTP_403_FORBIDDEN)
        
        # Filtros opcionales
        seccion_filter = request.GET.get('seccion', '')
        search_filter = request.GET.get('search', '')
        
        # Query base optimizada
        alumnos = Alumno.objects.filter(activo=True).select_related(
            'seccion', 'seccion__grado'
        ).order_by('seccion__nombre', 'apellidos', 'nombres')
        
        # Aplicar filtros
        if seccion_filter and seccion_filter != 'Todos':
            alumnos = alumnos.filter(seccion__nombre__icontains=seccion_filter.replace('1ro ', ''))
        
        if search_filter:
            from django.db.models import Q
            alumnos = alumnos.filter(
                Q(nombres__icontains=search_filter) |
                Q(apellidos__icontains=search_filter) |
                Q(codigo__icontains=search_filter)
            )
        
        # Limitar resultados para mejor rendimiento
        alumnos = alumnos[:200]  # Máximo 200 alumnos
        
        # Crear serializer específico para usuarios con QR
        data = []
        for alumno in alumnos:
            data.append({
                'id': alumno.id,
                'codigo': alumno.codigo,
                'nombre_completo': alumno.nombre_completo,
                'seccion': str(alumno.seccion),
                'qr_token': str(alumno.qr_token),
                'qr_image': generate_qr_image(alumno),
                'email': f"{alumno.nombres.lower().replace(' ', '').replace('ñ', 'n')}.{alumno.apellidos.lower().replace(' ', '').replace('ñ', 'n')}@peruanosuizo.edu.pe"
            })
        
        return Response({
            'success': True,
            'data': data,
            'total': len(data),
            'message': f'Se encontraron {len(data)} estudiantes'
        })
        
    except Exception as e:
        return Response({
            'success': False,
            'message': f'Error al obtener alumnos: {str(e)}'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

def generate_qr_image(alumno):
    """Generar imagen QR para un alumno con optimizaciones"""
    import qrcode, io, base64
    from PIL import Image, ImageDraw, ImageFont
    import os

    try:
        # Generar QR con configuración optimizada
        qr = qrcode.QRCode(
            version=1, 
            error_correction=qrcode.constants.ERROR_CORRECT_M,  # Menor corrección para mejor rendimiento
            box_size=8,  # Tamaño reducido para mejor rendimiento
            border=3
        )
        qr.add_data(str(alumno.qr_token))
        qr.make(fit=True)
        qr_img = qr.make_image(fill_color="black", back_color="white").convert('RGB')

        qr_w, qr_h = qr_img.size

        # Área de texto optimizada
        padding = 10
        font_size = 18
        
        # Usar fuente del sistema o default
        try:
            if os.name == 'nt':  # Windows
                font = ImageFont.truetype("arial.ttf", font_size)
                font_small = ImageFont.truetype("arial.ttf", 14)
            else:  # Linux/Mac
                font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size)
                font_small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
        except Exception:
            font = ImageFont.load_default()
            font_small = ImageFont.load_default()

        # Textos optimizados
        nombre = alumno.nombre_completo[:30]  # Limitar longitud
        codigo = alumno.codigo

        # Calcular dimensiones del texto
        dummy = Image.new('RGB', (1, 1))
        draw_dummy = ImageDraw.Draw(dummy)
        
        # Usar textbbox si está disponible, sino usar textsize
        try:
            _, _, tw1, th1 = draw_dummy.textbbox((0, 0), nombre, font=font)
            _, _, tw2, th2 = draw_dummy.textbbox((0, 0), codigo, font=font_small)
        except AttributeError:
            tw1, th1 = draw_dummy.textsize(nombre, font=font)
            tw2, th2 = draw_dummy.textsize(codigo, font=font_small)
        
        text_area_h = th1 + th2 + padding * 3

        # Canvas final optimizado
        total_h = qr_h + text_area_h
        canvas = Image.new('RGB', (qr_w, total_h), 'white')
        canvas.paste(qr_img, (0, 0))

        draw = ImageDraw.Draw(canvas)
        
        # Dibujar textos centrados
        draw.text(((qr_w - tw1) // 2, qr_h + padding), nombre, fill='black', font=font)
        draw.text(((qr_w - tw2) // 2, qr_h + padding + th1 + 4), codigo, fill='gray', font=font_small)

        # Convertir a base64 con compresión optimizada
        buffer = io.BytesIO()
        canvas.save(buffer, format='PNG', optimize=True, compress_level=6)
        return f"data:image/png;base64,{base64.b64encode(buffer.getvalue()).decode()}"
        
    except Exception as e:
        # Fallback: QR simple sin texto
        try:
            qr = qrcode.QRCode(version=1, box_size=8, border=3)
            qr.add_data(str(alumno.qr_token))
            qr.make(fit=True)
            qr_img = qr.make_image(fill_color="black", back_color="white")
            
            buffer = io.BytesIO()
            qr_img.save(buffer, format='PNG')
            return f"data:image/png;base64,{base64.b64encode(buffer.getvalue()).decode()}"
        except:
            return ""  # Retornar vacío si falla completamente

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def mobile_reporte_asistencia(request):
    """
    Endpoint para obtener reportes de asistencia por fecha
    GET /api/mobile/reportes/asistencia/
    """
    try:
        # Verificar permisos
        if request.user.rol not in ['administrador', 'profesor']:
            return Response({
                'success': False,
                'message': 'No tienes permisos para ver esta información'
            }, status=status.HTTP_403_FORBIDDEN)
        
        # Parámetros de consulta
        fecha_str = request.GET.get('fecha', timezone.localdate().isoformat())
        seccion_id = request.GET.get('seccion')
        
        try:
            fecha = timezone.datetime.strptime(fecha_str, '%Y-%m-%d').date()
        except ValueError:
            return Response({
                'success': False,
                'message': 'Formato de fecha inválido. Use YYYY-MM-DD'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Query base
        asistencias = Asistencia.objects.filter(
            sesion__fecha=fecha
        ).select_related('alumno', 'alumno__seccion', 'sesion__curso')
        
        # Filtrar por sección si se especifica
        if seccion_id:
            asistencias = asistencias.filter(alumno__seccion_id=seccion_id)
        
        # Agrupar por sección
        reporte_por_seccion = {}
        for asistencia in asistencias:
            seccion_nombre = str(asistencia.alumno.seccion)
            
            if seccion_nombre not in reporte_por_seccion:
                reporte_por_seccion[seccion_nombre] = {
                    'seccion': seccion_nombre,
                    'alumnos': [],
                    'estadisticas': {
                        'total': 0,
                        'presentes': 0,
                        'tardanzas': 0,
                        'ausentes': 0,
                        'justificados': 0
                    }
                }
            
            # Agregar alumno
            reporte_por_seccion[seccion_nombre]['alumnos'].append({
                'id': asistencia.alumno.id,
                'nombre_completo': asistencia.alumno.nombre_completo,
                'codigo': asistencia.alumno.codigo,
                'estado': asistencia.estado,
                'hora_registro': asistencia.hora_registro.strftime('%I:%M %p').lower() if asistencia.hora_registro else None,
                'curso': asistencia.sesion.curso.nombre,
                'registrado_via_qr': asistencia.registrado_via_qr
            })
            
            # Actualizar estadísticas
            stats = reporte_por_seccion[seccion_nombre]['estadisticas']
            stats['total'] += 1
            stats[asistencia.estado + 's'] += 1
        
        # Convertir a lista y calcular porcentajes
        reporte_final = []
        for seccion_data in reporte_por_seccion.values():
            stats = seccion_data['estadisticas']
            total = stats['total']
            if total > 0:
                stats['porcentaje_asistencia'] = round((stats['presentes'] + stats['tardanzas']) / total * 100, 1)
            else:
                stats['porcentaje_asistencia'] = 0
            
            reporte_final.append(seccion_data)
        
        # Ordenar por nombre de sección
        reporte_final.sort(key=lambda x: x['seccion'])
        
        # Estadísticas generales
        total_general = sum(s['estadisticas']['total'] for s in reporte_final)
        presentes_general = sum(s['estadisticas']['presentes'] for s in reporte_final)
        tardanzas_general = sum(s['estadisticas']['tardanzas'] for s in reporte_final)
        
        return Response({
            'success': True,
            'data': {
                'fecha': fecha.isoformat(),
                'estadisticas_generales': {
                    'total_registros': total_general,
                    'presentes': presentes_general,
                    'tardanzas': tardanzas_general,
                    'ausentes': sum(s['estadisticas']['ausentes'] for s in reporte_final),
                    'justificados': sum(s['estadisticas']['justificados'] for s in reporte_final),
                    'porcentaje_asistencia': round((presentes_general + tardanzas_general) / max(total_general, 1) * 100, 1)
                },
                'reporte_por_seccion': reporte_final,
                'total_secciones': len(reporte_final)
            }
        })
        
    except Exception as e:
        return Response({
            'success': False,
            'message': f'Error al generar reporte: {str(e)}'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)