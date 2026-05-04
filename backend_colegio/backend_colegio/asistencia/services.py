import qrcode
import io
import base64
from datetime import timedelta
from django.utils import timezone
from django.conf import settings
from .models import TokenQR, SesionClase, Asistencia, Alumno


def generar_token_qr(sesion: SesionClase) -> TokenQR:
    TokenQR.objects.filter(sesion=sesion).update(activo=False)

    expira_en = timezone.now() + timedelta(minutes=settings.QR_EXPIRATION_MINUTES)
    token_qr = TokenQR.objects.create(sesion=sesion, expira_en=expira_en)
    return token_qr


def generar_imagen_qr(token_qr: TokenQR) -> str:
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_H,
        box_size=10,
        border=4,
    )
    qr.add_data(str(token_qr.token))
    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")
    buffer = io.BytesIO()
    img.save(buffer, format='PNG')
    buffer.seek(0)

    return base64.b64encode(buffer.getvalue()).decode('utf-8')


def registrar_asistencia_qr(token_uuid: str, alumno_id: int) -> dict:
    try:
        token_qr = TokenQR.objects.select_related('sesion').get(token=token_uuid)
    except TokenQR.DoesNotExist:
        return {'success': False, 'message': 'QR inválido'}

    if not token_qr.esta_vigente:
        return {'success': False, 'message': 'El QR ha expirado o ya no está activo'}

    if token_qr.sesion.cerrada:
        return {'success': False, 'message': 'La sesión ya fue cerrada'}

    try:
        alumno = Alumno.objects.get(id=alumno_id, activo=True)
    except Alumno.DoesNotExist:
        return {'success': False, 'message': 'Alumno no encontrado'}

    asistencia, creada = Asistencia.objects.get_or_create(
        sesion=token_qr.sesion,
        alumno=alumno,
        defaults={
            'estado': 'presente',
            'hora_registro': timezone.now(),
            'registrado_via_qr': True,
        }
    )

    if not creada:
        return {'success': False, 'message': 'La asistencia ya fue registrada para este alumno'}

    return {
        'success': True,
        'message': f'Asistencia registrada para {alumno.nombre_completo}',
        'data': {
            'alumno': alumno.nombre_completo,
            'estado': asistencia.estado,
            'hora': asistencia.hora_registro.strftime('%H:%M'),
        }
    }


def crear_asistencias_pendientes(sesion: SesionClase):
    alumnos = Alumno.objects.filter(seccion=sesion.seccion, activo=True)
    asistencias_existentes = Asistencia.objects.filter(sesion=sesion).values_list('alumno_id', flat=True)
    nuevas = [
        Asistencia(sesion=sesion, alumno=alumno, estado='ausente')
        for alumno in alumnos
        if alumno.id not in asistencias_existentes
    ]
    if nuevas:
        Asistencia.objects.bulk_create(nuevas)
