import { Router } from 'express';
import supabase from '../../../config/database';
import { authMiddleware, requireRoles, AuthRequest } from '../../../middleware/auth';
import { webMemberMiddleware } from '../../../middleware/web-members';
import { getTeacherSectionIds, getTeacherStudentPersonaIds } from '../web-attendance-access';

const router = Router();
const PAGE_SIZE = 1000;
const PERSON_BATCH_SIZE = 200;
const datePattern = /^\d{4}-\d{2}-\d{2}$/;

function relation(value: any) {
  return Array.isArray(value) ? value[0] || null : value || null;
}

async function loadAttendanceRows(personIds: string[], from: string, to: string) {
  const rows: any[] = [];
  for (let index = 0; index < personIds.length; index += PERSON_BATCH_SIZE) {
    const batch = personIds.slice(index, index + PERSON_BATCH_SIZE);
    for (let offset = 0; ; offset += PAGE_SIZE) {
      const { data, error } = await supabase
        .from('asistencias')
        .select('id, persona_id, fecha, hora_entrada, estado')
        .eq('tipo_persona', 'alumno')
        .in('persona_id', batch)
        .gte('fecha', from)
        .lte('fecha', to)
        .range(offset, offset + PAGE_SIZE - 1);
      if (error) throw error;
      rows.push(...(data || []));
      if (!data || data.length < PAGE_SIZE) break;
    }
  }
  return rows;
}

async function loadTeacherDetails(personIds: string[], sectionIds: string[]) {
  const students: any[] = [];
  for (let index = 0; index < personIds.length; index += PERSON_BATCH_SIZE) {
    const batch = personIds.slice(index, index + PERSON_BATCH_SIZE);
    const { data, error } = await supabase
      .from('alumnos')
      .select('persona_id, personas!inner(nombres, apellidos), matriculas!inner(seccion_id, secciones!inner(nombre, grados!inner(nombre)))')
      .eq('estado', 'activo')
      .eq('matriculas.estado', 'activo')
      .in('matriculas.seccion_id', sectionIds)
      .in('persona_id', batch);
    if (error) throw error;
    students.push(...(data || []));
  }
  return new Map(students.map((student: any) => {
    const person = relation(student.personas);
    const enrollment = relation(student.matriculas);
    const section = relation(enrollment?.secciones);
    const grade = relation(section?.grados);
    return [student.persona_id, {
      name: `${person?.nombres || ''} ${person?.apellidos || ''}`.trim(),
      salon: `${grade?.nombre || ''} ${section?.nombre || ''}`.trim(),
    }];
  }));
}

const webProfessor = [authMiddleware, requireRoles('profesor'), webMemberMiddleware];

router.get('/historial', ...webProfessor, async (req: AuthRequest, res) => {
  try {
    const from = String(req.query.desde || '');
    const to = String(req.query.hasta || '');
    if (!datePattern.test(from) || !datePattern.test(to) || from > to) {
      return res.status(400).json({ success: false, code: 'INVALID_DATE_RANGE', message: 'El rango de fechas no es válido' });
    }
    const sections = await getTeacherSectionIds(req.user!.id);
    const personIds = await getTeacherStudentPersonaIds(sections);
    if (!personIds.length) return res.json({ success: true, data: [], total: 0 });

    const rows = await loadAttendanceRows(personIds, from, to);
    const summary = new Map<string, { fecha: string; presentes: number; tardanzas: number; ausentes: number }>();
    for (const row of rows) {
      const fecha = String(row.fecha).slice(0, 10);
      const counts = summary.get(fecha) || { fecha, presentes: 0, tardanzas: 0, ausentes: 0 };
      const status = String(row.estado || '').toLowerCase();
      if (status === 'presente') counts.presentes += 1;
      else if (status === 'tardanza') counts.tardanzas += 1;
      else if (status === 'falta' || status === 'ausente' || status === 'justificado') counts.ausentes += 1;
      summary.set(fecha, counts);
    }
    return res.json({ success: true, data: [...summary.values()].sort((a, b) => b.fecha.localeCompare(a.fecha)), total: summary.size });
  } catch {
    return res.status(500).json({ success: false, code: 'TEACHER_ATTENDANCE_HISTORY_FAILED', message: 'No se pudo cargar el historial de tus secciones' });
  }
});

router.get('/fecha', ...webProfessor, async (req: AuthRequest, res) => {
  try {
    const date = String(req.query.fecha || '');
    if (!datePattern.test(date)) return res.status(400).json({ success: false, code: 'INVALID_DATE', message: 'La fecha no es válida' });
    const sections = await getTeacherSectionIds(req.user!.id);
    const personIds = await getTeacherStudentPersonaIds(sections);
    if (!personIds.length) return res.json({ success: true, data: [], total: 0 });

    const rows = await loadAttendanceRows(personIds, date, date);
    const studentDetails = await loadTeacherDetails([...new Set(rows.map((row) => row.persona_id))], sections);
    const data = rows.flatMap((row) => {
      const student = studentDetails.get(row.persona_id);
      return student ? [{
        id: row.id,
        persona_id: row.persona_id,
        nombre_completo: student.name,
        salon: student.salon,
        hora_registro: row.hora_entrada || '',
        estado_entrada: row.estado,
        fecha: row.fecha,
      }] : [];
    });
    return res.json({ success: true, data, total: data.length });
  } catch {
    return res.status(500).json({ success: false, code: 'TEACHER_ATTENDANCE_DAY_FAILED', message: 'No se pudo cargar la asistencia de tus secciones' });
  }
});

export default router;
