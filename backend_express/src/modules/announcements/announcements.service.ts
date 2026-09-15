import announcementsRepository from './announcements.repository';
import notificationService from '../notifications/notifications.service';

export interface CreateAnnouncementInput {
  personaId: string;
  titulo: string;
  contenido: string;
  tipo?: string;
  destinatarioTipo: string;
  seccionId?: string;
  gradoId?: string;
}

class AnnouncementsService {
  async create(input: CreateAnnouncementInput) {
    const data = await announcementsRepository.create({
      persona_id: input.personaId,
      titulo: input.titulo,
      contenido: input.contenido,
      tipo: input.tipo || 'general',
      destinatario_tipo: input.destinatarioTipo,
      seccion_id: input.destinatarioTipo === 'seccion' ? input.seccionId : null,
      grado_id: input.destinatarioTipo === 'grado' ? input.gradoId : null
    });

    try {
      const notification = {
        tipo: 'comunicado' as const,
        titulo: '📢 Nuevo Comunicado',
        mensaje: input.titulo,
        datos: {
          comunicado_id: String(data.id),
          tipo: input.tipo || 'general'
        }
      };

      if (input.destinatarioTipo === 'global') {
        await notificationService.enviarATodos(notification);
      } else if (input.destinatarioTipo === 'seccion' && input.seccionId) {
        await notificationService.enviarASeccion(input.seccionId, notification);
      }
    } catch (error: any) {
      // A notification failure must not undo a successfully created announcement.
      console.error('Failed to send announcement notification:', error.message);
    }

    return data;
  }
}

export default new AnnouncementsService();
