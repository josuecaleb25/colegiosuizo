import { closeAttendanceSession, getLimaDate } from '../../../services/attendance-session.service';
import sessionsRepository from './sessions.repository';

class AttendanceSessionsService {
  async getActive() {
    return sessionsRepository.findActive(getLimaDate());
  }

  async create(createdBy: string | null) {
    const date = getLimaDate();
    const active = await sessionsRepository.findActive(date);
    if (active) return { data: active, reused: true, message: 'Se reutilizó la sesión de asistencia activa' };

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
