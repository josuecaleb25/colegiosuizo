import announcementsRepository from './announcements.repository';
import notificationService from '../notifications/notifications.service';

export interface ListAnnouncementsInput {
  rol?: string;
  seccionId?: string;
  gradoId?: string;
}

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
  async getById(id: string) {
    const announcement: any = await announcementsRepository.findById(id);
    return {
      id: announcement.id,
      titulo: announcement.titulo,
      contenido: announcement.contenido,
      tipo: announcement.tipo,
      destinatario_tipo: announcement.destinatario_tipo,
      fecha_publicacion: announcement.fecha_publicacion,
      autor: `${announcement.usuarios.personas.nombres} ${announcement.usuarios.personas.apellidos}`,
      destinatario: announcement.destinatario_tipo === 'global'
        ? 'Todos'
        : announcement.secciones
          ? `${announcement.secciones.grados.nombre}${announcement.secciones.nombre}`
          : 'N/A'
    };
  }

  async markAsRead(announcementId: string, usuarioId: string) {
    const created = await announcementsRepository.markAsRead(announcementId, usuarioId);
    return created
      ? 'Comunicado marcado como leído'
      : 'Comunicado ya fue marcado como leído';
  }

  async listSent(personaId: string, rol?: string) {
    const announcements = await announcementsRepository.findSent(personaId, rol);
    return announcements.map((announcement: any) => {
      let destinatario = 'Todos';
      if (announcement.destinatario_tipo === 'seccion' && announcement.secciones) {
        const section = Array.isArray(announcement.secciones)
          ? announcement.secciones[0] : announcement.secciones;
        const grade = Array.isArray(section.grados) ? section.grados[0] : section.grados;
        destinatario = `${grade.nombre} ${section.nombre}`;
      } else if (announcement.destinatario_tipo === 'grado' && announcement.grados) {
        const grade = Array.isArray(announcement.grados)
          ? announcement.grados[0] : announcement.grados;
        destinatario = grade.nombre;
      }

      const person = Array.isArray(announcement.personas)
        ? announcement.personas[0] : announcement.personas;
      return {
        id: announcement.id,
        titulo: announcement.titulo,
        contenido: announcement.contenido,
        tipo: announcement.tipo,
        destinatario_tipo: announcement.destinatario_tipo,
        fecha_publicacion: announcement.fecha_publicacion,
        destinatario,
        salon: destinatario,
        emisor: `${person.nombres} ${person.apellidos}`,
        estado: 'Enviado'
      };
    });
  }

  async listReads(announcementId: string) {
    const reads = await announcementsRepository.findReads(announcementId);
    return reads.map((read: any) => ({
      id: read.id,
      fecha_lectura: read.fecha_lectura,
      usuario: `${read.usuarios.personas.nombres} ${read.usuarios.personas.apellidos}`
    }));
  }

  async update(id: string, input: Record<string, unknown>) {
    const values: Record<string, unknown> = {};
    if (input.titulo !== undefined) values.titulo = input.titulo;
    if (input.contenido !== undefined) values.contenido = input.contenido;
    if (input.tipo !== undefined) values.tipo = input.tipo;
    if (input.destinatario_tipo !== undefined) {
      values.destinatario_tipo = input.destinatario_tipo;
      values.seccion_id = input.destinatario_tipo === 'seccion' ? input.seccion_id : null;
      values.grado_id = input.destinatario_tipo === 'grado' ? input.grado_id : null;
    }
    return announcementsRepository.update(id, values);
  }

  async remove(id: string) {
    await announcementsRepository.deactivate(id);
  }

  async listVisible(input: ListAnnouncementsInput) {
    const announcements = await announcementsRepository.findVisible(input);
    return announcements.map((announcement: any) => ({
      id: announcement.id,
      titulo: announcement.titulo,
      contenido: announcement.contenido,
      tipo: announcement.tipo,
      destinatario_tipo: announcement.destinatario_tipo,
      fecha_publicacion: announcement.fecha_publicacion,
      emisor: `${announcement.personas.nombres} ${announcement.personas.apellidos}`,
      seccion: announcement.secciones
        ? `${announcement.secciones.grados.nombre}${announcement.secciones.nombre}`
        : null
    }));
  }

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
