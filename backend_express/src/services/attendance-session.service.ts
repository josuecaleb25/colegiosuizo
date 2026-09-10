import supabase from '../config/database';

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
    return getSession(id);
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
  if (session.estado === 'cerrada') return session;

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

  return closed || getSession(id);
}
