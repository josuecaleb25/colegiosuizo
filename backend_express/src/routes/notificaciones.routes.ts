import { Router, Request, Response } from 'express';
import supabase from '../config/database';
import { authMiddleware, AuthRequest } from '../middleware/auth';

const router = Router();

async function getStudentIdsForUser(req: AuthRequest): Promise<string[]> {
  const personaId = req.user!.id;
  const { data: directStudent, error: directError } = await supabase
    .from('alumnos')
    .select('id')
    .eq('persona_id', personaId)
    .maybeSingle();
  if (directError) throw directError;
  if (directStudent) return [directStudent.id];

  if (req.user?.rol === 'padre') {
    const { data, error } = await supabase
      .from('padres_alumnos')
      .select('alumno_id')
      .eq('padre_id', personaId);
    if (error) throw error;
    return (data || []).map((row: any) => row.alumno_id).filter(Boolean);
  }
  return [];
}

function applyRecipientFilter(query: any, studentIds: string[]) {
  if (studentIds.length === 0) return query.is('estudiante_id', null);
  return query.or(`estudiante_id.in.(${studentIds.join(',')}),estudiante_id.is.null`);
}

router.get('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { solo_no_leidas, page, limit } = req.query;
    const studentIds = await getStudentIdsForUser(req);

    const pageNum = Math.max(1, parseInt(page as string) || 1);
    const limitNum = Math.min(50, Math.max(1, parseInt(limit as string) || 20));
    const offset = (pageNum - 1) * limitNum;

    let query: any = supabase
      .from('notificaciones_historial')
      .select('*', { count: 'exact' })
      .order('fecha_envio', { ascending: false })
      .range(offset, offset + limitNum - 1);
    query = applyRecipientFilter(query, studentIds);

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

router.get('/no-leidas', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const studentIds = await getStudentIdsForUser(req);

    let query: any = supabase
      .from('notificaciones_historial')
      .select('*', { count: 'exact', head: true })
      .eq('leida', false);
    query = applyRecipientFilter(query, studentIds);
    const { count, error } = await query;

    if (error) throw error;

    res.json({ success: true, no_leidas: count || 0 });
  } catch (err: any) {
    console.error('Error al contar no leídas:', err.message);
    res.status(500).json({ success: false, message: err.message });
  }
});

router.put('/:id/leer', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const studentIds = await getStudentIdsForUser(req);
    if (studentIds.length === 0) {
      return res.status(403).json({ success: false, message: 'Notificación no disponible para este usuario' });
    }

    const { error } = await supabase
      .from('notificaciones_historial')
      .update({ leida: true, fecha_lectura: new Date().toISOString() })
      .eq('id', id)
      .in('estudiante_id', studentIds);

    if (error) throw error;

    res.json({ success: true, message: 'Notificación marcada como leída' });
  } catch (err: any) {
    console.error('Error al marcar como leída:', err.message);
    res.status(500).json({ success: false, message: err.message });
  }
});

export default router;
