const DATE_KEY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function assertAttendanceWeekday(dateValue: string) {
  const date = String(dateValue || '').slice(0, 10);
  if (!DATE_KEY_PATTERN.test(date)) {
    const error: any = new Error('La fecha de asistencia no es válida');
    error.status = 400;
    error.code = 'INVALID_ATTENDANCE_DATE';
    throw error;
  }

  const [year, month, day] = date.split('-').map(Number);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  if (parsed.getUTCFullYear() !== year || parsed.getUTCMonth() !== month - 1 || parsed.getUTCDate() !== day) {
    const error: any = new Error('La fecha de asistencia no es válida');
    error.status = 400;
    error.code = 'INVALID_ATTENDANCE_DATE';
    throw error;
  }

  const weekday = parsed.getUTCDay();
  if (weekday === 0 || weekday === 6) {
    const error: any = new Error('No se pueden registrar asistencias los sábados ni domingos');
    error.status = 400;
    error.code = 'ATTENDANCE_WEEKEND_NOT_ALLOWED';
    throw error;
  }
  return date;
}
