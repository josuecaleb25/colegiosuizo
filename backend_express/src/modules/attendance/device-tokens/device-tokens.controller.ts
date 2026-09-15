import { Response } from 'express';
import { AuthRequest } from '../../../middleware/auth';
import deviceTokensService from './device-tokens.service';

export async function registerDeviceToken(req: AuthRequest, res: Response) {
  try {
    const { token, device_info } = req.body;
    if (!token) return res.status(400).json({ success: false, message: 'Token es requerido' });

    const data = await deviceTokensService.register(req.user!.id, token, device_info || '');
    return res.json({ success: true, message: 'Token asociado correctamente', data });
  } catch (error: any) {
    console.error('Failed to save device token:', error.message);
    return res.status(500).json({ success: false, message: 'Error al guardar token', error: error.message });
  }
}
