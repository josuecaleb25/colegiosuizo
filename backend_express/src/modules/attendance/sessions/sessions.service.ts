import { closeAttendanceSession, getLimaDate } from '../../../services/attendance-session.service';
import sessionsRepository from './sessions.repository';

class AttendanceSessionsService {
  async getActive() {
    return sessionsRepository.findActive(getLimaDate());
  }

  async getToday() {
    const session = await sessionsRepository.findLatestByDate(getLimaDate());
    if (!session || session.estado === 'abierta') return session;

    const attendanceCount = await sessionsRepository.countStudentAttendance(session.id);
    if (attendanceCount === 0) {
      await sessionsRepository.deleteEmptyClosed(session.id);
      return null;
    }
    return session;
  }

  async create(createdBy: string | null) {
    const date = getLimaDate();
    const active = await sessionsRepository.findActive(date);
    if (active) return { data: active, reused: true, message: 'Se reutilizó la sesión de asistencia activa' };

    const existing = await this.getToday();
    if (existing) {
      const error: any = new Error('La asistencia del día ya fue completada');
      error.status = 409;
      error.code = 'ATTENDANCE_ALREADY_COMPLETED';
      throw error;
    }

    const { data, error } = await sessionsRepository.create(date, createdBy);
    if (error?.code === '23505') {
      const concurrent = await sessionsRepository.findActive(date);
      return { data: concurrent, reused: true, message: 'Se reutilizó la sesión creada por otra tablet' };
    }
    if (error) throw error;
    return { data, reused: false, message: 'Sesión de asistencia creada correctamente' };
  }

  async close(id: string) {
    return closeAttendanceSession(id);
  }
}

export default new AttendanceSessionsService();
