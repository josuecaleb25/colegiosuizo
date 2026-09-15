import supabase from '../../../config/database';

class AttendanceReportsRepository {
  async findDay(sessionId: string, date: string) {
    const { data: session, error: sessionError } = await supabase
      .from('asistencia_sesiones')
      .select('id, fecha, estado')
      .eq('id', sessionId)
      .maybeSingle();
    if (sessionError) throw sessionError;
    if (!session) return { session: null, records: [] };

    const { data, error } = await supabase
      .from('asistencias')
      .select('id, persona_id, fecha, hora_entrada, estado, sesion_id')
      .eq('fecha', date)
      .eq('sesion_id', sessionId)
      .eq('tipo_persona', 'alumno');
    if (error) throw error;
    return { session, records: data || [] };
  }

  async findLeaderboardStudents(sectionId?: string) {
    let query: any = supabase
      .from('alumnos')
      .select('id, persona_id, personas!inner(id, nombres, apellidos), matriculas!inner(seccion_id, secciones!inner(id, nombre, grados!inner(nombre)))')
      .eq('estado', 'activo');
    if (sectionId && sectionId !== 'todos') query = query.eq('matriculas.seccion_id', sectionId);
    const { data, error } = await query;
    if (error) throw error;
    return data || [];
  }

  async findRecordsForPeople(personaIds: string[], from: string, to: string) {
    const records: any[] = [];
    for (let i = 0; i < personaIds.length; i += 50) {
      const { data, error } = await supabase
        .from('asistencias')
        .select('persona_id, estado, hora_entrada, fecha')
        .in('persona_id', personaIds.slice(i, i + 50))
        .gte('fecha', from)
        .lte('fecha', to)
        .eq('tipo_persona', 'alumno');
      if (error) throw error;
      records.push(...(data || []));
    }
    return records;
  }

  async findRecords(filters: { date: string; sectionId?: string; studentId?: string }) {
    let personaIds: string[] | undefined;

    if (filters.sectionId) {
      const { data: students, error } = await supabase
        .from('alumnos')
        .select('persona_id, matriculas!inner(seccion_id)')
        .eq('estado', 'activo')
        .eq('matriculas.seccion_id', filters.sectionId);
      if (error) throw error;
      personaIds = (students || []).map((student: any) => student.persona_id).filter(Boolean);
      if (personaIds.length === 0) return [];
    }

    let query: any = supabase
      .from('asistencias')
      .select('id, persona_id, fecha, hora_entrada, estado, sesion_id')
      .eq('fecha', filters.date)
      .eq('tipo_persona', 'alumno')
      .order('hora_entrada', { ascending: false, nullsFirst: false })
      .limit(500);

    if (filters.studentId) {
      const student = await this.findStudent(filters.studentId);
      if (!student) return [];
      query = query.eq('persona_id', student.persona_id);
    }
    if (personaIds) query = query.in('persona_id', personaIds);

    const { data, error } = await query;
    if (error) throw error;
    return this.enrichRecords(data || []);
  }

  async findStudent(studentId: string) {
    const { data, error } = await supabase
      .from('alumnos')
      .select('id, persona_id, codigo_alumno, personas!inner(id, nombres, apellidos), matriculas(seccion_id, secciones(nombre, grados(nombre)))')
      .eq('id', studentId)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findHistory(studentId: string, filters: { from?: string; to?: string; limit: number }) {
    const student = await this.findStudent(studentId);
    if (!student) return { student: null, records: [] };

    let query: any = supabase
      .from('asistencias')
      .select('id, fecha, hora_entrada, estado, sesion_id')
      .eq('persona_id', student.persona_id)
      .eq('tipo_persona', 'alumno')
      .order('fecha', { ascending: false })
      .limit(filters.limit);
    if (filters.from) query = query.gte('fecha', filters.from);
    if (filters.to) query = query.lte('fecha', filters.to);

    const { data, error } = await query;
    if (error) throw error;
    return { student, records: data || [] };
  }

  async findStatistics(filters: { from?: string; to?: string; sectionId?: string }) {
    let personaIds: string[] | undefined;
    if (filters.sectionId) {
      const { data: students, error } = await supabase
        .from('alumnos')
        .select('persona_id, matriculas!inner(seccion_id)')
        .eq('estado', 'activo')
        .eq('matriculas.seccion_id', filters.sectionId);
      if (error) throw error;
      personaIds = (students || []).map((student: any) => student.persona_id).filter(Boolean);
      if (personaIds.length === 0) return [];
    }

    let query: any = supabase
      .from('asistencias')
      .select('estado, fecha')
      .eq('tipo_persona', 'alumno');
    if (filters.from) query = query.gte('fecha', filters.from);
    if (filters.to) query = query.lte('fecha', filters.to);
    if (personaIds) query = query.in('persona_id', personaIds);

    const { data, error } = await query;
    if (error) throw error;
    return data || [];
  }

  private async enrichRecords(records: any[]) {
    const personaIds = [...new Set(records.map((record) => record.persona_id).filter(Boolean))];
    if (personaIds.length === 0) return records;

    const { data: students, error } = await supabase
      .from('alumnos')
      .select('id, persona_id, codigo_alumno, personas!inner(nombres, apellidos), matriculas(seccion_id, secciones(nombre, grados(nombre)))')
      .in('persona_id', personaIds);
    if (error) throw error;

    const byPersona = new Map((students || []).map((student: any) => [student.persona_id, student]));
    return records.map((record) => ({ ...record, student: byPersona.get(record.persona_id) || null }));
  }
}

export default new AttendanceReportsRepository();
