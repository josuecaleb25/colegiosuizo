import supabase from '../../../config/database';

class AttendanceQrRepository {
  async findSession(id: string) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('id, fecha, estado')
      .eq('id', id)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findActiveQr(code: string) {
    const { data, error } = await supabase
      .from('codigos_qr')
      .select('persona_id, personas!inner (id, nombres, apellidos)')
      .eq('codigo', code)
      .eq('activo', true)
      .limit(1);
    if (error) throw error;
    return data?.[0] || null;
  }

  async register(input: {
    personaId: string;
    sessionId: string;
    date: string;
    time: string;
    status: string;
  }) {
    const { data: inserted, error: insertError } = await supabase
      .from('asistencias')
      .insert({
        persona_id: input.personaId,
        tipo_persona: 'alumno',
        fecha: input.date,
        hora_entrada: input.time,
        estado: input.status,
        sesion_id: input.sessionId
      })
      .select('hora_entrada, estado')
      .single();

    if (insertError?.code === '23505') {
      const { data: existing, error: existingError } = await supabase
        .from('asistencias')
        .select('hora_entrada, estado')
        .eq('persona_id', input.personaId)
        .eq('fecha', input.date)
        .eq('tipo_persona', 'alumno')
        .limit(1)
        .maybeSingle();
      if (existingError) throw existingError;
      return { data: existing, created: false };
    }
    if (insertError) throw insertError;
    return { data: inserted, created: true };
  }

  async findStudentId(personaId: string) {
    const { data, error } = await supabase
      .from('alumnos')
      .select('id')
      .eq('persona_id', personaId)
      .maybeSingle();
    if (error) throw error;
    return data?.id || null;
  }
}

export default new AttendanceQrRepository();
