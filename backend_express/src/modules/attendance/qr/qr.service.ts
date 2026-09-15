import notificationService from '../../notifications/notifications.service';
import qrRepository from './qr.repository';

const LIMA_TIME_ZONE = 'America/Lima';

class QrAttendanceService {
  async scan(code: string, sessionId: string) {
    const now = new Date();
    const date = now.toLocaleDateString('en-CA', { timeZone: LIMA_TIME_ZONE });
    const session = await qrRepository.findSession(sessionId);

    if (!session) throw this.error('SESSION_NOT_FOUND', 404);
    if (session.estado !== 'abierta') throw this.error('SESSION_CLOSED', 409);
    if (session.fecha !== date) throw this.error('SESSION_DATE_MISMATCH', 400);

    const qr = await qrRepository.findActiveQr(code);
    if (!qr) throw this.error('QR_NOT_FOUND', 404, 'QR no válido');

    const person = qr.personas as any;
    const time24 = now.toLocaleTimeString('en-GB', { timeZone: LIMA_TIME_ZONE, hour12: false });
    const [hourText, minuteText] = time24.split(':');
    const hour = Number.parseInt(hourText, 10);
    const minute = Number.parseInt(minuteText, 10);
    const status = hour < 7 || (hour === 7 && minute <= 30) ? 'presente' : 'tardanza';

    const result = await qrRepository.registerAtomically({
      personaId: person.id,
      sessionId,
      date,
      time: time24,
      status
    });
    const registeredTime = result.data?.hora_entrada || time24;
    const registeredStatus = result.data?.estado || status;
    const studentId = result.created ? await qrRepository.findStudentId(person.id) : null;

    return {
      created: result.created,
      studentId,
      date,
      time: registeredTime,
      status: registeredStatus,
      studentName: `${person.nombres} ${person.apellidos}`
    };
  }

  async notifyStudent(result: any) {
    if (!result.created || !result.studentId) return;
    const statusText = result.status === 'presente' ? 'a tiempo' : 'con tardanza';
    await notificationService.enviarAEstudiante(result.studentId, {
      tipo: 'asistencia',
      titulo: '✅ Asistencia Registrada',
      mensaje: `Buenos días, su hijo/a ${result.studentName} llegó ${statusText} a las ${result.time}`,
      datos: {
        alumno_id: String(result.studentId),
        estado: result.status,
        hora: result.time,
        fecha: result.date
      }
    });
  }

  private error(code: string, status: number, message = '') {
    const error: any = new Error(message || code);
    error.code = code;
    error.status = status;
    return error;
  }
}

export default new QrAttendanceService();
