import { Router } from 'express';
import supabase from '../../../config/database';
import { authMiddleware, requireRoles } from '../../../middleware/auth';

const router = Router();
const managers = ['profesor', 'administrador', 'admin'];

router.put('/:id', authMiddleware, requireRoles(...managers), async (req, res) => {
  try {
    const { estado } = req.body;
    if (!estado) return res.status(400).json({ success: false, code: 'STATUS_REQUIRED', message: 'El estado es obligatorio' });
    const { data, error } = await supabase
      .from('asistencias')
      .update({ estado })
      .eq('id', req.params.id)
      .eq('tipo_persona', 'alumno')
      .select()
      .maybeSingle();
    if (error) throw error;
    if (!data) return res.status(404).json({ success: false, code: 'ATTENDANCE_NOT_FOUND', message: 'Asistencia no encontrada' });
    res.json({ success: true, message: 'Asistencia actualizada correctamente', data });
  } catch (error: any) {
    res.status(500).json({ success: false, code: 'ATTENDANCE_UPDATE_FAILED', message: 'No se pudo actualizar la asistencia', error: error.message });
  }
});

router.post('/eliminar-batch', authMiddleware, requireRoles(...managers), async (req, res) => {
  try {
    const ids = req.body?.ids;
    if (!Array.isArray(ids) || ids.length === 0) return res.status(400).json({ success: false, code: 'IDS_REQUIRED', message: 'Se requiere un array de IDs' });
    let deleted = 0;
    for (let i = 0; i < ids.length; i += 100) {
      const { data, error } = await supabase.from('asistencias').delete().in('id', ids.slice(i, i + 100)).eq('tipo_persona', 'alumno').select('id');
      if (error) throw error;
      deleted += data?.length || 0;
    }
    res.json({ success: true, message: `${deleted} asistencias eliminadas correctamente`, data: { eliminadas: deleted, solicitadas: ids.length } });
  } catch (error: any) {
    res.status(500).json({ success: false, code: 'ATTENDANCE_DELETE_FAILED', message: 'No se pudieron eliminar las asistencias', error: error.message });
  }
});

router.delete('/:id', authMiddleware, requireRoles(...managers), async (req, res) => {
  try {
    const { data, error } = await supabase.from('asistencias').delete().eq('id', req.params.id).eq('tipo_persona', 'alumno').select('id').maybeSingle();
    if (error) throw error;
    if (!data) return res.status(404).json({ success: false, code: 'ATTENDANCE_NOT_FOUND', message: 'Asistencia no encontrada' });
    res.json({ success: true, message: 'Asistencia eliminada correctamente' });
  } catch (error: any) {
    res.status(500).json({ success: false, code: 'ATTENDANCE_DELETE_FAILED', message: 'No se pudo eliminar la asistencia', error: error.message });
  }
});

export default router;
