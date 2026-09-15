import { AuthRequest } from '../../../middleware/auth';
import { Response } from 'express';
import sessionsService from './sessions.service';

export async function getActiveSession(_req: AuthRequest, res: Response) {
  try {
    const data = await sessionsService.getActive();
    return res.json({
      success: true,
      data: data || null,
      message: data ? 'Sesión activa encontrada' : 'No hay sesión activa'
    });
  } catch (error: any) {
    console.error('Error getting active attendance session:', error.message);
    return res.status(500).json({ success: false, message: 'Error al obtener sesión activa', error: error.message });
  }
}

export async function createSession(req: AuthRequest, res: Response) {
  try {
    const result = await sessionsService.create(req.body?.creado_por || null);
    return res.status(result.reused ? 200 : 201).json({
      success: true,
      reused: result.reused,
      message: result.message,
      data: result.data
    });
  } catch (error: any) {
    console.error('Error creating attendance session:', error.message);
    return res.status(500).json({ success: false, message: 'Error al crear sesión de asistencia', error: error.message });
  }
}

export async function closeSession(req: AuthRequest, res: Response) {
  try {
    const data = await sessionsService.close(req.params.id);
    if (!data) return res.status(404).json({ success: false, code: 'SESSION_NOT_FOUND', message: 'Sesión no encontrada' });
    return res.json({ success: true, message: 'Sesión cerrada y ausencias registradas correctamente', data });
  } catch (error: any) {
    console.error('Error closing attendance session:', error.message);
    return res.status(error.status || 500).json({
      success: false,
      code: error.code || 'SESSION_CLOSE_FAILED',
      message: 'Error al cerrar la sesión',
      error: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
}
