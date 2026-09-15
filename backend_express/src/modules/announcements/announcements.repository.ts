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
  async create(record: CreateAnnouncementRecord) {
    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .insert(record)
      .select()
      .single();

    if (error) throw error;
    return data;
  }
}

export default new AnnouncementsRepository();
