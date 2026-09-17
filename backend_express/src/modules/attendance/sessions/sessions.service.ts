import { closeAttendanceSession, getLimaDate } from '../../../services/attendance-session.service';
import sessionsRepository from './sessions.repository';

class AttendanceSessionsService {
  async getActive() {
    return sessionsRepository.findActive(getLimaDate());
  }

  async getToday() {
    return sessionsRepository.findLatestByDate(getLimaDate());
  }

  async create(createdBy: string | null) {
    const date = getLimaDate();
    const active = await sessionsRepository.findActive(date);
    if (active) return { data: active, reused: true, message: 'Se reutilizó la sesión de asistencia activa' };

    const existing = await sessionsRepository.findLatestByDate(date);
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
