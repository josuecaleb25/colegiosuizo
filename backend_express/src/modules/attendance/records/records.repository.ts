import supabase from '../../../config/database';

class AttendanceRecordsRepository {
  async findStudentById(studentId: string) {
    const { data, error } = await supabase
      .from('alumnos')
      .select('id, persona_id, personas!inner(nombres, apellidos)')
      .eq('id', studentId)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findStudentByPersona(personaId: string) {
    const { data, error } = await supabase
      .from('alumnos')
      .select('id, persona_id, personas!inner(nombres, apellidos)')
      .eq('persona_id', personaId)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findSession(id: string) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('id, fecha, estado')
      .eq('id', id)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findSessionByDate(date: string) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('id, fecha, estado')
      .eq('fecha', date)
      .order('creado_en', { ascending: false })
      .limit(1)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findExisting(personaId: string, date: string, sessionId?: string) {
    let query: any = supabase
      .from('asistencias')
      .select('id, hora_entrada, estado')
      .eq('persona_id', personaId)
      .eq('fecha', date)
      .eq('tipo_persona', 'alumno')
      .limit(1);
    if (sessionId) query = query.eq('sesion_id', sessionId);
    const { data, error } = await query;
    if (error) throw error;
    return data?.[0] || null;
  }

  async insert(input: {
    personaId: string;
    date: string;
    time: string | null;
    status: string;
    sessionId?: string | null;
    observations?: string;
  }) {
    const { data, error } = await supabase
      .from('asistencias')
      .insert({
        persona_id: input.personaId,
        tipo_persona: 'alumno',
        fecha: input.date,
        hora_entrada: input.time,
        estado: input.status,
        sesion_id: input.sessionId || null
      })
      .select()
      .single();
    if (error) throw error;
    return data;
  }

  async findActiveStudents() {
    const { data, error } = await supabase
      .from('alumnos')
      .select('id, persona_id')
      .eq('estado', 'activo');
    if (error) throw error;
    return data || [];
  }

  async findRecordsByDate(date: string) {
    const { data, error } = await supabase
      .from('asistencias')
      .select('persona_id')
      .eq('fecha', date)
      .eq('tipo_persona', 'alumno');
    if (error) throw error;
    return data || [];
  }

  async insertAbsences(records: Array<Record<string, unknown>>) {
    if (records.length === 0) return;
    const { error } = await supabase
      .from('asistencias')
      .upsert(records, { onConflict: 'persona_id,fecha', ignoreDuplicates: true });
    if (error) throw error;
  }
}

export default new AttendanceRecordsRepository();
