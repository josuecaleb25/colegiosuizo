import { Response, NextFunction } from 'express';
import supabase from '../../config/database';
import { AuthRequest } from '../../middleware/auth';

const adminRoles = new Set(['admin', 'administrador']);
const managerRoles = new Set(['admin', 'administrador', 'profesor']);

export function requireAnnouncementVisible(req: AuthRequest, res: Response, next: NextFunction) {
  void (async () => {
    const user = req.user;
    if (!user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });

    try {
      const { data: announcement, error } = await supabase
        .from('comunicados_nuevos')
        .select('destinatario_tipo, seccion_id, grado_id, activo')
        .eq('id', req.params.id)
        .maybeSingle();
      if (error) throw error;
      if (!announcement || announcement.activo === false) return res.status(404).json({ success: false, message: 'Comunicado no encontrado' });
      if (managerRoles.has(user.rol) || announcement.destinatario_tipo === 'global') return next();
      if (user.rol !== 'alumno') return res.status(403).json({ success: false, message: 'No tienes acceso a este comunicado' });

      let studentIds: string[] = [];
      if (user.rol === 'alumno') {
        const { data, error: studentError } = await supabase.from('alumnos').select('id').eq('persona_id', user.id);
        if (studentError) throw studentError;
        studentIds = (data || []).map(row => row.id);
      }

      if (!studentIds.length) return res.status(403).json({ success: false, message: 'No tienes acceso a este comunicado' });
      const { data: enrollments, error: enrollmentError } = await supabase
        .from('matriculas')
        .select('seccion_id')
        .in('alumno_id', studentIds)
        .eq('estado', 'activo');
      if (enrollmentError) throw enrollmentError;
      const sectionIds = [...new Set((enrollments || []).map(row => row.seccion_id).filter(Boolean))];
      const sectionMatch = announcement.destinatario_tipo === 'seccion' && sectionIds.includes(announcement.seccion_id);

      let gradeMatch = false;
      if (announcement.destinatario_tipo === 'grado' && sectionIds.length) {
        const { data: sections, error: sectionError } = await supabase
          .from('secciones')
          .select('grado_id')
          .in('id', sectionIds);
        if (sectionError) throw sectionError;
        gradeMatch = (sections || []).some(section => section.grado_id === announcement.grado_id);
      }

      if (!sectionMatch && !gradeMatch) return res.status(403).json({ success: false, message: 'No tienes acceso a este comunicado' });
      next();
    } catch {
      res.status(500).json({ success: false, message: 'No se pudo validar el acceso al comunicado' });
    }
  })();
}

export function requireAnnouncementOwner(req: AuthRequest, res: Response, next: NextFunction) {
  void (async () => {
    const user = req.user;
    if (!user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    if (adminRoles.has(user.rol)) return next();

    try {
      const { data, error } = await supabase
        .from('comunicados_nuevos')
        .select('persona_id')
        .eq('id', req.params.id)
        .maybeSingle();
      if (error) throw error;
      if (!data) return res.status(404).json({ success: false, message: 'Comunicado no encontrado' });
      if (data.persona_id !== user.id) return res.status(403).json({ success: false, message: 'No tienes permisos para modificar este comunicado' });
      next();
    } catch {
      res.status(500).json({ success: false, message: 'No se pudo validar el propietario del comunicado' });
    }
  })();
}
