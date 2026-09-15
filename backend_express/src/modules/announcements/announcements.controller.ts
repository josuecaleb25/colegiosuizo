import { Request, Response } from 'express';
import announcementsService from './announcements.service';

export async function getAnnouncement(req: Request, res: Response) {
  try {
    const data = await announcementsService.getById(req.params.id);
    return res.json({ success: true, data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al obtener comunicado', error: error.message });
  }
}

export async function markAnnouncementAsRead(req: Request, res: Response) {
  try {
    const { usuario_id } = req.body;
    if (!usuario_id) return res.status(400).json({ success: false, message: 'usuario_id es requerido' });
    const message = await announcementsService.markAsRead(req.params.id, usuario_id);
    return res.json({ success: true, message });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al marcar comunicado como leído', error: error.message });
  }
}

export async function listSentAnnouncements(req: Request, res: Response) {
  try {
    const { usuario_id, rol } = req.query;
    if (!usuario_id) return res.status(400).json({ success: false, message: 'usuario_id es requerido' });
    const data = await announcementsService.listSent(usuario_id as string, rol as string | undefined);
    return res.json({ success: true, data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al obtener historial', error: error.message });
  }
}

export async function listAnnouncementReads(req: Request, res: Response) {
  try {
    const data = await announcementsService.listReads(req.params.id);
    return res.json({ success: true, data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al obtener lecturas', error: error.message });
  }
}

export async function updateAnnouncement(req: Request, res: Response) {
  try {
    const data = await announcementsService.update(req.params.id, req.body);
    return res.json({ success: true, message: 'Comunicado actualizado exitosamente', data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al actualizar comunicado', error: error.message });
  }
}

export async function deleteAnnouncement(req: Request, res: Response) {
  try {
    await announcementsService.remove(req.params.id);
    return res.json({ success: true, message: 'Comunicado eliminado exitosamente' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al eliminar comunicado', error: error.message });
  }
}

export async function listAnnouncements(req: Request, res: Response) {
  try {
    const { rol, seccion_id, grado_id } = req.query;
    const data = await announcementsService.listVisible({
      rol: rol as string | undefined,
      seccionId: seccion_id as string | undefined,
      gradoId: grado_id as string | undefined
    });

    return res.json({ success: true, data });
  } catch (error: any) {
    console.error('Error al obtener comunicados:', error.message);
    return res.status(500).json({
      success: false,
      message: 'Error al obtener comunicados',
      error: error.message
    });
  }
}

export async function createAnnouncement(req: Request, res: Response) {
  try {
    const {
      usuario_id,
      titulo,
      contenido,
      tipo,
      destinatario_tipo,
      seccion_id,
      grado_id
    } = req.body;

    if (!usuario_id || !titulo || !contenido || !destinatario_tipo) {
      return res.status(400).json({
        success: false,
        message: 'Faltan datos requeridos: usuario_id (persona_id), titulo, contenido, destinatario_tipo'
      });
    }

    if (destinatario_tipo === 'seccion' && !seccion_id) {
      return res.status(400).json({
        success: false,
        message: 'seccion_id es requerido cuando destinatario_tipo es "seccion"'
      });
    }

    if (destinatario_tipo === 'grado' && !grado_id) {
      return res.status(400).json({
        success: false,
        message: 'grado_id es requerido cuando destinatario_tipo es "grado"'
      });
    }

    const data = await announcementsService.create({
      personaId: usuario_id,
      titulo,
      contenido,
      tipo,
      destinatarioTipo: destinatario_tipo,
      seccionId: seccion_id,
      gradoId: grado_id
    });

    return res.json({
      success: true,
      message: 'Comunicado publicado exitosamente',
      data
    });
  } catch (error: any) {
    console.error('Error al crear comunicado:', error);
    return res.status(500).json({
      success: false,
      message: 'Error al crear comunicado',
      error: error.message
    });
  }
}
