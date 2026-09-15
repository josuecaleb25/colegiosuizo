import { Request, Response } from 'express';
import announcementsService from './announcements.service';

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
