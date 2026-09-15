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
  if (studentIds.length === 0) return query.limit(0);
  return query.in('estudiante_id', studentIds);
}

function getComunicadoId(notificacion: any): string | null {
  if (notificacion?.tipo !== 'comunicado' || !notificacion.datos) return null;
  try {
    const datos = typeof notificacion.datos === 'string'
      ? JSON.parse(notificacion.datos)
      : notificacion.datos;
    return datos?.comunicado_id ? String(datos.comunicado_id) : null;
  } catch {
    return null;
  }
}

async function ocultarNotificacionesDeComunicadosEliminados(notificaciones: any[]) {
  const comunicadoIds = notificaciones
    .map(getComunicadoId)
    .filter((id): id is string => Boolean(id));

  if (comunicadoIds.length === 0) return notificaciones;

  const { data: comunicadosActivos, error } = await supabase
    .from('comunicados_nuevos')
    .select('id')
    .in('id', [...new Set(comunicadoIds)])
    .eq('activo', true);

  if (error) throw error;

  const idsActivos = new Set((comunicadosActivos || []).map((comunicado: any) => String(comunicado.id)));
  return notificaciones.filter((notificacion) => {
    const comunicadoId = getComunicadoId(notificacion);
    return !comunicadoId || idsActivos.has(comunicadoId);
  });
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

    const notificacionesVisibles = await ocultarNotificacionesDeComunicadosEliminados(data || []);

    res.json({
      success: true,
      data: notificacionesVisibles,
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
      .select('*')
      .eq('leida', false);
    query = applyRecipientFilter(query, studentIds);
    const { data, error } = await query;

    if (error) throw error;

    const notificacionesVisibles = await ocultarNotificacionesDeComunicadosEliminados(data || []);

    res.json({ success: true, no_leidas: notificacionesVisibles.length });
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

    let updateQuery: any = supabase
      .from('notificaciones_historial')
      .update({ leida: true, fecha_lectura: new Date().toISOString() })
      .eq('id', id);
    updateQuery = applyRecipientFilter(updateQuery, studentIds);

    const { error } = await updateQuery;

    if (error) throw error;

    res.json({ success: true, message: 'Notificación marcada como leída' });
  } catch (err: any) {
    console.error('Error al marcar como leída:', err.message);
    res.status(500).json({ success: false, message: err.message });
  }
});

export default router;
