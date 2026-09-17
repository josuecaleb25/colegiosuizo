import supabase from '../config/database';
import notificationService from '../modules/notifications/notifications.service';

const LIMA_TIME_ZONE = 'America/Lima';

export function getLimaDate(date = new Date()): string {
  return date.toLocaleDateString('en-CA', { timeZone: LIMA_TIME_ZONE });
}

function isMissingFunction(error: any): boolean {
  return error?.code === 'PGRST202' || error?.code === '42883';
}

async function getSession(id: string) {
  const { data, error } = await supabase
    .from('asistencia_sesiones')
    .select('*')
    .eq('id', id)
    .maybeSingle();

  if (error) throw error;
  return data;
}

async function notifySessionAbsences(sessionId: string, sessionDate: string) {
  const { data: absences, error: absencesError } = await supabase
    .from('asistencias')
    .select('id, persona_id, fecha')
    .eq('sesion_id', sessionId)
    .eq('tipo_persona', 'alumno')
    .in('estado', ['falta', 'ausente']);
  if (absencesError) throw absencesError;
  if (!absences || absences.length === 0) return;

  const personaIds = [...new Set(absences.map((absence: any) => absence.persona_id).filter(Boolean))];
  const students: any[] = [];
  for (let index = 0; index < personaIds.length; index += 100) {
    const batch = personaIds.slice(index, index + 100);
    const { data, error } = await supabase
      .from('alumnos')
      .select('id, persona_id')
      .in('persona_id', batch);
    if (error) throw error;
    students.push(...(data || []));
  }

  const studentByPersona = new Map(
    (students || []).map((student: any) => [student.persona_id, student])
  );
  const pending = absences
    .map((absence: any) => ({
      absence,
      student: studentByPersona.get(absence.persona_id)
    }))
    .filter((item: any) => item.student);

  await notificationService.enviarAusencias(pending.map(({ absence, student }: any) => ({
    estudianteId: student.id,
    asistenciaId: absence.id,
    fecha: absence.fecha || sessionDate
  })));
}

/**
 * Closes a session and records absences. The SQL migration supplies the atomic
 * RPC. The fallback keeps deployments compatible while that migration is being
 * applied, but the database function remains the concurrency guarantee.
 */
export async function closeAttendanceSession(id: string) {
  const { error: rpcError } = await supabase.rpc('cerrar_sesion_asistencia_atomica', {
    p_sesion_id: id
  });

  if (!rpcError) {
    const session = await getSession(id);
    if (session?.fecha) {
      try {
        await notifySessionAbsences(id, session.fecha);
      } catch (notificationError: any) {
        console.error('Failed to process absence notifications after session close:', {
          message: notificationError.message,
          code: notificationError.code,
          details: notificationError.details,
          hint: notificationError.hint
        });
      }
    }
    return session;
  }
  if (rpcError?.message?.includes('SESSION_NOT_FOUND')) {
    const error: any = new Error('Sesión no encontrada');
    error.status = 404;
    error.code = 'SESSION_NOT_FOUND';
    throw error;
  }
  if (!isMissingFunction(rpcError)) throw rpcError;

  const session = await getSession(id);
  if (!session) {
    const error: any = new Error('Sesión no encontrada');
    error.status = 404;
    error.code = 'SESSION_NOT_FOUND';
    throw error;
  }
  if (session.estado === 'cerrada') {
    try {
      await notifySessionAbsences(id, session.fecha);
    } catch (notificationError: any) {
      console.error('Failed to retry absence notifications after session close:', {
        message: notificationError.message,
        code: notificationError.code,
        details: notificationError.details,
        hint: notificationError.hint
      });
    }
    return session;
  }

  const { data: students, error: studentsError } = await supabase
    .from('alumnos')
    .select('persona_id')
    .eq('estado', 'activo');
  if (studentsError) throw studentsError;

  for (let attempt = 0; attempt < 2; attempt++) {
    const { data: attendance, error: attendanceError } = await supabase
      .from('asistencias')
      .select('persona_id')
      .eq('fecha', session.fecha)
      .eq('tipo_persona', 'alumno');
    if (attendanceError) throw attendanceError;

    const registered = new Set((attendance || []).map((row: any) => row.persona_id));
    const absences = (students || [])
      .filter((row: any) => row.persona_id && !registered.has(row.persona_id))
      .map((row: any) => ({
        persona_id: row.persona_id,
        tipo_persona: 'alumno',
        fecha: session.fecha,
        hora_entrada: null,
        estado: 'falta',
        sesion_id: id
      }));

    if (absences.length === 0) break;
    const { error } = await supabase.from('asistencias').insert(absences);
    if (!error) break;
    if (error.code !== '23505' || attempt === 1) throw error;
  }

  const { data: closed, error: closeError } = await supabase
    .from('asistencia_sesiones')
    .update({ estado: 'cerrada', cerrado_en: new Date().toISOString() })
    .eq('id', id)
    .eq('estado', 'abierta')
    .select()
    .maybeSingle();
  if (closeError) throw closeError;

  const result = closed || await getSession(id);
  if (result?.fecha) {
    try {
      await notifySessionAbsences(id, result.fecha);
    } catch (notificationError: any) {
      console.error('Failed to process absence notifications after session close:', {
        message: notificationError.message,
        code: notificationError.code,
        details: notificationError.details,
        hint: notificationError.hint
      });
    }
  }
  return result;
}
