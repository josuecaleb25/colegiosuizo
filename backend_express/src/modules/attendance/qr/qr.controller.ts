import { AuthRequest } from '../../../middleware/auth';
import { Response } from 'express';
import qrAttendanceService from './qr.service';

export async function scanAttendanceQr(req: AuthRequest, res: Response) {
  try {
    if (!['profesor', 'administrador', 'admin'].includes(req.user?.rol || '')) {
      return res.status(403).json({ success: false, message: 'No tienes permiso para escanear códigos QR' });
    }

    const { qr_token, sesion_id } = req.body;
    if (!qr_token || !sesion_id) {
      return res.status(400).json({
        success: false,
        code: 'SESSION_ID_REQUIRED',
        message: 'Se requieren qr_token y sesion_id'
      });
    }

    const result = await qrAttendanceService.scan(qr_token, sesion_id);
    const response = {
      success: true,
      reused: !result.created,
      message: result.created ? 'Asistencia registrada exitosamente' : 'La asistencia ya estaba registrada',
      data: {
        alumno: result.studentName,
        estado: result.status,
        hora: result.time,
        reused: !result.created
      }
    };
    res.json(response);

    if (result.created) {
      void qrAttendanceService.notifyStudent(result).catch((error: any) => {
        console.error('Error sending attendance notification:', error.message);
      });
    }
  } catch (error: any) {
    console.error('Error scanning attendance QR:', error.message);
    const code = error.code || 'ATTENDANCE_SCAN_FAILED';
    return res.status(error.status || 500).json({
      success: false,
      code,
      message: code === 'SESSION_CLOSED'
        ? 'La sesión de asistencia ya fue cerrada'
        : code === 'SESSION_NOT_FOUND'
          ? 'Sesión de asistencia no encontrada'
          : code === 'SESSION_DATE_MISMATCH'
            ? 'La sesión no corresponde al día actual'
            : code === 'QR_NOT_FOUND'
              ? 'QR no válido'
              : 'Error al registrar asistencia',
      error: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
}
