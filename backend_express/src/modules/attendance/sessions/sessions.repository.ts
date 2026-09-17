import supabase from '../../../config/database';

class AttendanceSessionsRepository {
  async findLatestByDate(date: string) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('*')
      .eq('fecha', date)
      .order('creado_en', { ascending: false })
      .limit(1)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findActive(date: string) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('*')
      .eq('fecha', date)
      .eq('estado', 'abierta')
      .order('creado_en', { ascending: false })
      .limit(1)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async create(date: string, createdBy: string | null) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .insert({ fecha: date, estado: 'abierta', creado_por: createdBy })
      .select()
      .single();
    return { data, error };
  }

  async findById(id: string) {
    const { data, error } = await supabase
      .from('asistencia_sesiones')
      .select('*')
      .eq('id', id)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async countStudentAttendance(sessionId: string) {
    const { count, error } = await supabase
      .from('asistencias')
      .select('id', { count: 'exact', head: true })
      .eq('sesion_id', sessionId)
      .eq('tipo_persona', 'alumno');
    if (error) throw error;
    return count || 0;
  }

  async deleteEmptyClosed(id: string) {
    const { error } = await supabase
      .from('asistencia_sesiones')
      .delete()
      .eq('id', id)
      .eq('estado', 'cerrada');
    if (error) throw error;
  }
}

export default new AttendanceSessionsRepository();
