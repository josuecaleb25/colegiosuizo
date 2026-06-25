import { Router, Request, Response } from 'express';
import supabase from '../config/database';

const router = Router();

router.get('/', async (req: Request, res: Response) => {
  try {
    const { estudiante_id, solo_no_leidas, page, limit } = req.query;

    if (!estudiante_id) {
      return res.status(400).json({ success: false, message: 'estudiante_id es requerido' });
    }

    const pageNum = Math.max(1, parseInt(page as string) || 1);
    const limitNum = Math.min(50, Math.max(1, parseInt(limit as string) || 20));
    const offset = (pageNum - 1) * limitNum;

    let query = supabase
      .from('notificaciones_historial')
      .select('*', { count: 'exact' })
      .or(`estudiante_id.eq.${estudiante_id},estudiante_id.is.null`)
      .order('fecha_envio', { ascending: false })
      .range(offset, offset + limitNum - 1);

    if (solo_no_leidas === 'true') {
      query = query.eq('leida', false);
    }

    const { data, error, count } = await query;

    if (error) throw error;

    res.json({
      success: true,
      data,
      pagination: {
        page: pageNum,
        limit: limitNum,
        total: count || 0,
        totalPages: Math.ceil((count || 0) / limitNum)
      }
    });
  } catch (err: any) {
    console.error('Error al obtener notificaciones:', err.message);
    res.status(500).json({ success: false, message: err.message });
  }
});

router.get('/no-leidas', async (req: Request, res: Response) => {
  try {
    const { estudiante_id } = req.query;

    if (!estudiante_id) {
      return res.status(400).json({ success: false, message: 'estudiante_id es requerido' });
    }

    const { count, error } = await supabase
      .from('notificaciones_historial')
      .select('*', { count: 'exact', head: true })
      .or(`estudiante_id.eq.${estudiante_id},estudiante_id.is.null`)
      .eq('leida', false);

    if (error) throw error;

    res.json({ success: true, no_leidas: count || 0 });
  } catch (err: any) {
    console.error('Error al contar no leídas:', err.message);
    res.status(500).json({ success: false, message: err.message });
  }
});

router.put('/:id/leer', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    const { error } = await supabase
      .from('notificaciones_historial')
      .update({ leida: true, fecha_lectura: new Date().toISOString() })
      .eq('id', id);

    if (error) throw error;

    res.json({ success: true, message: 'Notificación marcada como leída' });
  } catch (err: any) {
    console.error('Error al marcar como leída:', err.message);
    res.status(500).json({ success: false, message: err.message });
  }
});

export default router;
