const LIMA_TIME_ZONE = 'America/Lima';

export function formatAttendanceTime(value: string | null | undefined): string {
  if (!value) return '';

  const normalized = value.trim();
  const meridiemMatch = normalized.match(/^(\d{1,2}):(\d{2})(?::\d{2})?\s*(AM|PM)$/i);
  if (meridiemMatch) {
    const hour = Number(meridiemMatch[1]);
    const minute = meridiemMatch[2];
    const meridiem = meridiemMatch[3].toLowerCase() === 'am' ? 'a. m.' : 'p. m.';
    return `${hour}:${minute} ${meridiem}`;
  }

  const timeMatch = normalized.match(/^(\d{1,2}):(\d{2})/);
  if (!timeMatch) return normalized;

  const hour24 = Number(timeMatch[1]);
  const minute = timeMatch[2];
  if (hour24 < 0 || hour24 > 23) return normalized;

  const hour12 = hour24 % 12 || 12;
  const meridiem = hour24 < 12 ? 'a. m.' : 'p. m.';
  return `${hour12}:${minute} ${meridiem}`;
}

export function formatAttendanceDate(value: string): string {
  const match = value?.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return value;

  const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]), 12));
  const formatted = new Intl.DateTimeFormat('es-PE', {
    timeZone: LIMA_TIME_ZONE,
    weekday: 'long',
    day: 'numeric',
    month: 'long'
  }).format(date);
  return formatted;
}

export function buildAttendanceMessage(input: {
  name?: string;
  status: string;
  time?: string | null;
  date: string;
}): string {
  const name = input.name ? ` ${input.name}` : '';
  const date = formatAttendanceDate(input.date);

  if (input.status === 'falta' || input.status === 'ausente') {
    return `Su hijo/a${name} no asistió hoy, ${date}.`;
  }

  const time = formatAttendanceTime(input.time);
  if (input.status === 'tardanza') {
    return `Su hijo/a${name} llegó tarde hoy a las ${time}.`;
  }

  return `Su hijo/a${name} llegó temprano hoy a las ${time}.`;
}

export function attendanceNotificationTitle(status: string): string {
  if (status === 'falta' || status === 'ausente') return 'Falta registrada';
  if (status === 'tardanza') return 'Tardanza registrada';
  return 'Asistencia registrada';
}
