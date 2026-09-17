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
}

export default new AttendanceSessionsRepository();
