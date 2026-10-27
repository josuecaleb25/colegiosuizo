import { Request, Response } from 'express';
import supabase from '../../config/database';
import { AuthRequest } from '../../middleware/auth';
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
    const user = (req as AuthRequest).user;
    if (!user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    const message = await announcementsService.markAsRead(req.params.id, user.id);
    return res.json({ success: true, message });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al marcar comunicado como leído', error: error.message });
  }
}

export async function listSentAnnouncements(req: Request, res: Response) {
  try {
    const user = (req as AuthRequest).user;
    if (!user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    const data = await announcementsService.listSent(user.id, user.rol);
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
    const user = (req as AuthRequest).user;
    if (!user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    let seccionIds: string[] = [];
    let gradoIds: string[] = [];

    if (user.rol === 'alumno' || user.rol === 'padre') {
      const { data, error } = await supabase.from('alumnos').select('id').eq('persona_id', user.id);
      if (error) throw error;
      const alumnoIds = (data || []).map(row => row.id);

      if (alumnoIds.length) {
        const { data, error } = await supabase
          .from('matriculas')
          .select('seccion_id')
          .in('alumno_id', alumnoIds)
          .eq('estado', 'activo');
        if (error) throw error;
        seccionIds = [...new Set((data || []).map(row => row.seccion_id).filter(Boolean))];
      }

      if (seccionIds.length) {
        const { data, error } = await supabase.from('secciones').select('grado_id').in('id', seccionIds);
        if (error) throw error;
        gradoIds = [...new Set((data || []).map(row => row.grado_id).filter(Boolean))];
      }
    }

    const data = await announcementsService.listVisible({
      rol: user.rol,
      seccionIds,
      gradoIds
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
    const user = (req as AuthRequest).user;
    if (!user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    const {
      titulo,
      contenido,
      tipo,
      destinatario_tipo,
      seccion_id,
      grado_id
    } = req.body;

    if (!titulo || !contenido || !destinatario_tipo || String(titulo).length > 160 || String(contenido).length > 20000) {
      return res.status(400).json({
        success: false,
        message: 'Datos del comunicado inválidos'
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
      personaId: user.id,
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
