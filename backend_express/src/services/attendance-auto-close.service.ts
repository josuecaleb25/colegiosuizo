import supabase from '../config/database';
import { closeAttendanceSession, getLimaDate } from './attendance-session.service';

const CHECK_INTERVAL_MS = 60_000;
let timer: NodeJS.Timeout | null = null;
let running = false;

function getLimaTime(date = new Date()): string {
  return date.toLocaleTimeString('en-GB', {
    timeZone: 'America/Lima',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
}

async function closeExpiredSessions(closeTime: string) {
  if (running || getLimaTime() < closeTime) return;
  running = true;
  try {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('id')
      .eq('fecha', getLimaDate())
      .eq('estado', 'abierta');
    if (error) throw error;

    for (const session of data || []) {
      try {
        await closeAttendanceSession(session.id);
        console.log(`Sesión de asistencia cerrada automáticamente: ${session.id}`);
      } catch (error) {
        console.error(`No se pudo cerrar automáticamente la sesión ${session.id}:`, error);
      }
    }
  } catch (error) {
    console.error('No se pudo revisar el cierre automático de asistencia:', error);
  } finally {
    running = false;
  }
}

export function startAttendanceAutoClose() {
  if (process.env.ATTENDANCE_AUTO_CLOSE_ENABLED !== 'true') {
    console.log('Cierre automático de asistencia desactivado');
    return;
  }

  const closeTime = process.env.ATTENDANCE_AUTO_CLOSE_TIME || '';
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(closeTime)) {
    console.error('ATTENDANCE_AUTO_CLOSE_TIME debe tener formato HH:mm; cierre automático desactivado');
    return;
  }

  void closeExpiredSessions(closeTime);
  timer = setInterval(() => void closeExpiredSessions(closeTime), CHECK_INTERVAL_MS);
  timer.unref();
  console.log(`Cierre automático de asistencia activo a las ${closeTime} (America/Lima)`);
}
