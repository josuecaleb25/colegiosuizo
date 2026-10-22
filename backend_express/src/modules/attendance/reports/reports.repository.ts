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
      .eq('tipo_persona', 'alumno')
      .limit(2500);
    if (error) throw error;
    return { session, records: data || [] };
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
      .limit(2500);

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

    const peopleBatches = personaIds
      ? Array.from({ length: Math.ceil(personaIds.length / 250) }, (_, index) => personaIds.slice(index * 250, (index + 1) * 250))
      : [undefined];
    const statuses = ['__total__', 'presente', 'tardanza', 'falta', 'justificado'];
    const jobs = statuses.flatMap(status => peopleBatches.map(personBatch => ({ status, personBatch })));
    const counts: Record<string, number> = { __total__: 0, presente: 0, tardanza: 0, falta: 0, justificado: 0 };

    for (let i = 0; i < jobs.length; i += 4) {
      const results = await Promise.all(jobs.slice(i, i + 4).map(async ({ status, personBatch }) => {
        let query: any = supabase
          .from('asistencias')
          .select('id', { count: 'exact', head: true })
          .eq('tipo_persona', 'alumno');
        if (status !== '__total__') query = query.eq('estado', status);
        if (filters.from) query = query.gte('fecha', filters.from);
        if (filters.to) query = query.lte('fecha', filters.to);
        if (personBatch) query = query.in('persona_id', personBatch);
        return { status, result: await query };
      }));

      for (const { status, result } of results) {
        if (result.error) throw result.error;
        counts[status] += result.count || 0;
      }
    }

    return {
      total: counts.__total__,
      presentes: counts.presente,
      tardanzas: counts.tardanza,
      faltas: counts.falta,
      justificados: counts.justificado
    };
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
