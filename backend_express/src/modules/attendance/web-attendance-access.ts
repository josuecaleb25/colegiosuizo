import supabase from '../../config/database';

export async function getTeacherSectionIds(personaId: string) {
  const { data: teacher, error: teacherError } = await supabase
    .from('docentes')
    .select('id')
    .eq('persona_id', personaId)
    .eq('estado', 'activo')
    .maybeSingle();
  if (teacherError) throw teacherError;
  if (!teacher) return [];

  const { data: assignments, error: assignmentsError } = await supabase
    .from('asignaciones')
    .select('seccion_id')
    .eq('docente_id', teacher.id);
  if (assignmentsError) throw assignmentsError;
  return [...new Set((assignments || []).map((item: any) => item.seccion_id).filter(Boolean))] as string[];
}

export async function getTeacherStudentPersonaIds(sectionIds: string[]) {
  if (!sectionIds.length) return [];
  const { data, error } = await supabase
    .from('alumnos')
    .select('persona_id, matriculas!inner(seccion_id)')
    .eq('estado', 'activo')
    .eq('matriculas.estado', 'activo')
    .in('matriculas.seccion_id', sectionIds);
  if (error) throw error;
  return [...new Set((data || []).map((item: any) => item.persona_id).filter(Boolean))] as string[];
}

export async function teacherOwnsAttendance(personaId: string, attendanceId: string) {
  const sectionIds = await getTeacherSectionIds(personaId);
  if (!sectionIds.length) return false;

  const { data: attendance, error: attendanceError } = await supabase
    .from('asistencias')
    .select('persona_id')
    .eq('id', attendanceId)
    .eq('tipo_persona', 'alumno')
    .maybeSingle();
  if (attendanceError) throw attendanceError;
  if (!attendance?.persona_id) return false;

  const { count, error } = await supabase
    .from('alumnos')
    .select('id, matriculas!inner(seccion_id)', { count: 'exact', head: true })
    .eq('persona_id', attendance.persona_id)
    .eq('estado', 'activo')
    .eq('matriculas.estado', 'activo')
    .in('matriculas.seccion_id', sectionIds);
  if (error) throw error;
  return (count || 0) > 0;
}
