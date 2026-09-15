import supabase from '../../config/database';

export interface CreateAnnouncementRecord {
  persona_id: string;
  titulo: string;
  contenido: string;
  tipo: string;
  destinatario_tipo: string;
  seccion_id?: string | null;
  grado_id?: string | null;
}

class AnnouncementsRepository {
  async findVisible(filters: { rol?: string; seccionId?: string; gradoId?: string }) {
    let query: any = supabase
      .from('comunicados_nuevos')
      .select(`
        id,
        titulo,
        contenido,
        tipo,
        destinatario_tipo,
        fecha_publicacion,
        personas!inner (nombres, apellidos),
        secciones (nombre, grados (nombre))
      `)
      .eq('activo', true)
      .order('fecha_publicacion', { ascending: false });

    if (filters.rol === 'alumno' || filters.rol === 'padre') {
      query = query.or(
        `destinatario_tipo.eq.global,and(destinatario_tipo.eq.seccion,seccion_id.eq.${filters.seccionId}),and(destinatario_tipo.eq.grado,grado_id.eq.${filters.gradoId})`
      );
    }

    const { data, error } = await query;
    if (error) throw error;
    return data || [];
  }

  async create(record: CreateAnnouncementRecord) {
    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .insert(record)
      .select()
      .single();

    if (error) throw error;
    return data;
  }

  async findById(id: string) {
    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .select(`
        *,
        usuarios!inner (personas!inner (nombres, apellidos)),
        secciones (nombre, grados (nombre))
      `)
      .eq('id', id)
      .single();
    if (error) throw error;
    return data;
  }

  async findSent(personaId: string, rol?: string) {
    let query: any = supabase
      .from('comunicados_nuevos')
      .select(`
        id, titulo, contenido, tipo, destinatario_tipo,
        fecha_publicacion, activo,
        personas!inner (nombres, apellidos),
        secciones (nombre, grados (nombre)),
        grados (nombre)
      `)
      .eq('activo', true)
      .order('fecha_publicacion', { ascending: false });

    if (rol !== 'admin' && rol !== 'administrador' && rol !== 'auxiliar') {
      query = query.eq('persona_id', personaId);
    }

    const { data, error } = await query;
    if (error) throw error;
    return data || [];
  }

  async findReads(announcementId: string) {
    const { data, error } = await supabase
      .from('lecturas_comunicados')
      .select(`
        id, fecha_lectura,
        usuarios!inner (personas!inner (nombres, apellidos))
      `)
      .eq('comunicado_id', announcementId)
      .order('fecha_lectura', { ascending: false });
    if (error) throw error;
    return data || [];
  }

  async markAsRead(announcementId: string, usuarioId: string) {
    const { data: existing, error: existingError } = await supabase
      .from('lecturas_comunicados')
      .select('id')
      .eq('comunicado_id', announcementId)
      .eq('usuario_id', usuarioId)
      .maybeSingle();
    if (existingError) throw existingError;
    if (existing) return false;

    const { error } = await supabase.from('lecturas_comunicados').insert({
      comunicado_id: announcementId,
      usuario_id: usuarioId
    });
    if (error) throw error;
    return true;
  }

  async update(id: string, values: Record<string, unknown>) {
    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .update(values)
      .eq('id', id)
      .select()
      .single();
    if (error) throw error;
    return data;
  }

  async deactivate(id: string) {
    const { error } = await supabase
      .from('comunicados_nuevos')
      .update({ activo: false })
      .eq('id', id);
    if (error) throw error;

    const { error: notificationError } = await supabase
      .from('notificaciones_historial')
      .delete()
      .eq('tipo', 'comunicado')
      .eq('datos->>comunicado_id', id);
    if (notificationError) throw notificationError;
  }
}

export default new AnnouncementsRepository();
