import { Response, NextFunction } from 'express';
import supabase from '../config/database';
import { AuthRequest } from './auth';

type UserIdentity = NonNullable<AuthRequest['user']>;

const administratorRoles = new Set(['admin', 'administrador']);

export async function resolveStudent(studentRef: string) {
  const byId = await supabase
    .from('alumnos')
    .select('id, persona_id')
    .eq('id', studentRef)
    .maybeSingle();
  if (byId.error) throw byId.error;
  if (byId.data) return byId.data;

  const byPerson = await supabase
    .from('alumnos')
    .select('id, persona_id')
    .eq('persona_id', studentRef)
    .maybeSingle();
  if (byPerson.error) throw byPerson.error;
  return byPerson.data;
}

export async function canAccessStudent(user: UserIdentity, studentRef: string): Promise<boolean> {
  if (administratorRoles.has(user.rol)) return true;

  const student = await resolveStudent(studentRef);
  if (!student) return false;
  if (student.persona_id === user.id) return true;

  if (user.rol === 'padre') return false;

  if (user.rol === 'profesor') {
    const { data: docente, error: docenteError } = await supabase
      .from('docentes')
      .select('id')
      .eq('persona_id', user.id)
      .maybeSingle();
    if (docenteError) throw docenteError;
    if (!docente) return false;

    const { data: matricula, error: matriculaError } = await supabase
      .from('matriculas')
      .select('seccion_id')
      .eq('alumno_id', student.id)
      .eq('estado', 'activo')
      .limit(1)
      .maybeSingle();
    if (matriculaError) throw matriculaError;
    if (!matricula) return false;

    const { data: assignment, error: assignmentError } = await supabase
      .from('asignaciones')
      .select('id')
      .eq('docente_id', docente.id)
      .eq('seccion_id', matricula.seccion_id)
      .limit(1)
      .maybeSingle();
    if (assignmentError) throw assignmentError;
    return !!assignment;
  }

  return false;
}

export function requireStudentAccess(getStudentRef: (req: AuthRequest) => string) {
  return async (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    try {
      const studentRef = getStudentRef(req);
      if (!studentRef) return res.status(400).json({ success: false, message: 'Se requiere el identificador del alumno' });
      const allowed = await canAccessStudent(req.user, studentRef);
      if (!allowed) return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
      next();
    } catch {
      res.status(500).json({ success: false, message: 'No se pudo validar el acceso al alumno' });
    }
  };
}

export function requireTeacherAccess(getTeacherId: (req: AuthRequest) => string) {
  return async (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    if (administratorRoles.has(req.user.rol)) return next();
    if (req.user.rol !== 'profesor') return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
    try {
      const { data, error } = await supabase
        .from('docentes')
        .select('id')
        .eq('id', getTeacherId(req))
        .eq('persona_id', req.user.id)
        .maybeSingle();
      if (error) throw error;
      if (!data) return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
      next();
    } catch {
      res.status(500).json({ success: false, message: 'No se pudo validar el acceso al docente' });
    }
  };
}

export function requireSectionAccess(getSectionId: (req: AuthRequest) => string) {
  return async (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    if (administratorRoles.has(req.user.rol)) return next();
    if (req.user.rol !== 'profesor') return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
    try {
      const { data: docente, error: docenteError } = await supabase
        .from('docentes')
        .select('id')
        .eq('persona_id', req.user.id)
        .maybeSingle();
      if (docenteError) throw docenteError;
      if (!docente) return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
      const { data: assignment, error: assignmentError } = await supabase
        .from('asignaciones')
        .select('id')
        .eq('docente_id', docente.id)
        .eq('seccion_id', getSectionId(req))
        .limit(1)
        .maybeSingle();
      if (assignmentError) throw assignmentError;
      if (!assignment) return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
      next();
    } catch {
      res.status(500).json({ success: false, message: 'No se pudo validar el acceso a la sección' });
    }
  };
}
