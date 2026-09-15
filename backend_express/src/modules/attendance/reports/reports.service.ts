import reportsRepository from './reports.repository';

const LIMA_TIME_ZONE = 'America/Lima';

class AttendanceReportsService {
  async day(sessionId: string, date: string) {
    const result = await reportsRepository.findDay(sessionId, date);
    if (!result.session) {
      const error: any = new Error('Sesión de asistencia no encontrada');
      error.status = 404;
      error.code = 'SESSION_NOT_FOUND';
      throw error;
    }
    if (result.session.estado !== 'abierta') {
      const error: any = new Error('La sesión de asistencia fue cerrada');
      error.status = 409;
      error.code = 'SESSION_CLOSED';
      throw error;
    }
    if (result.session.fecha !== date) {
      const error: any = new Error('La fecha no corresponde a la sesión activa');
      error.status = 400;
      error.code = 'SESSION_DATE_MISMATCH';
      throw error;
    }
    return result.records;
  }

  async leaderboard(sectionId: string | undefined, type: string, month: string) {
    const students = await reportsRepository.findLeaderboardStudents(sectionId);
    const [year, monthNumber] = month.split('-').map(Number);
    const from = `${month}-01`;
    const lastDay = new Date(year, monthNumber, 0).getDate();
    const to = `${month}-${String(lastDay).padStart(2, '0')}`;
    const personaIds = students.map((student: any) => student.personas?.id).filter(Boolean);
    const records = await reportsRepository.findRecordsForPeople(personaIds, from, to);
    const unique = new Map<string, any>();
    records.forEach((record) => unique.set(`${record.persona_id}|${record.fecha}`, record));

    const result = students.map((student: any) => {
      const person = student.personas;
      const enrollment = Array.isArray(student.matriculas) ? student.matriculas[0] : student.matriculas;
      const section = enrollment?.secciones;
      const grade = section?.grados;
      const studentRecords = [...unique.values()].filter((record) => record.persona_id === person.id);
      let punctual = 0;
      let attendance = 0;
      let minutesSum = 0;
      let timed = 0;
      studentRecords.forEach((record) => {
        const minutes = parseTime(record.hora_entrada);
        if (record.estado === 'presente' || record.estado === 'tardanza') attendance++;
        if (minutes !== null) {
          if (minutes <= 435) punctual++;
          minutesSum += minutes;
          timed++;
        }
      });
      const totalDays = studentRecords.length;
      return {
        persona_id: person.id,
        nombres: person.nombres || '',
        apellidos: person.apellidos || '',
        salon: section ? `${grade?.nombre || ''} ${section.nombre || ''}`.trim() : '',
        seccion_id: section?.id || null,
        total_dias: totalDays,
        puntual: punctual,
        tardanza: totalDays - punctual,
        puntualidad: totalDays ? Math.round((punctual / totalDays) * 100) : 0,
        asistencia_dias: attendance,
        asistencia: totalDays ? Math.round((attendance / totalDays) * 100) : 0,
        promedio: timed ? Math.round(minutesSum / timed) : 9999
      };
    });
    return result.sort((a, b) => {
      const first = type === 'puntual' ? b.puntual - a.puntual : b.asistencia_dias - a.asistencia_dias;
      return first || a.promedio - b.promedio;
    }).slice(0, 15);
  }

  async list(date?: string, sectionId?: string, studentId?: string) {
    const resolvedDate = date || new Date().toLocaleDateString('en-CA', { timeZone: LIMA_TIME_ZONE });
    const records = await reportsRepository.findRecords({ date: resolvedDate, sectionId, studentId });
    return records.map((record: any) => {
      const student = record.student;
      const person = Array.isArray(student?.personas) ? student.personas[0] : student?.personas;
      const enrollment = Array.isArray(student?.matriculas) ? student.matriculas[0] : student?.matriculas;
      const section = Array.isArray(enrollment?.secciones) ? enrollment.secciones[0] : enrollment?.secciones;
      const grade = Array.isArray(section?.grados) ? section.grados[0] : section?.grados;
      return {
        id: record.id,
        estado: record.estado,
        hora_registro: record.hora_entrada,
        hora_entrada: record.hora_entrada,
        registrado_via_qr: false,
        alumno_id: student?.id,
        nombre_completo: person ? `${person.nombres} ${person.apellidos}` : undefined,
        codigo: student?.codigo_alumno,
        salon: section ? `${grade?.nombre || ''} ${section.nombre || ''}`.trim() : undefined,
        sesion_id: record.sesion_id,
        fecha: record.fecha
      };
    });
  }

  async statistics(from?: string, to?: string, sectionId?: string) {
    const records = await reportsRepository.findStatistics({ from, to, sectionId });
    return {
      total: records.length,
      presentes: records.filter((record: any) => record.estado === 'presente').length,
      tardanzas: records.filter((record: any) => record.estado === 'tardanza').length,
      faltas: records.filter((record: any) => record.estado === 'falta').length,
      justificados: records.filter((record: any) => record.estado === 'justificado').length
    };
  }

  async history(studentId: string, from?: string, to?: string, limit = 100) {
    const result = await reportsRepository.findHistory(studentId, { from, to, limit });
    return result.records.map((record: any) => ({
      id: record.id,
      fecha: record.fecha,
      hora_entrada: record.hora_entrada,
      estado: record.estado,
      observaciones: null,
      sesion_id: record.sesion_id,
      registrado_via_qr: false
    }));
  }
}

function parseTime(value: string | null): number | null {
  if (!value) return null;
  const match = value.trim().toUpperCase().match(/^(\d{1,2}):(\d{2})/);
  if (!match) return null;
  let hour = Number(match[1]);
  const minute = Number(match[2]);
  if (value.toUpperCase().includes('PM') && hour < 12) hour += 12;
  if (value.toUpperCase().includes('AM') && hour === 12) hour = 0;
  return hour * 60 + minute;
}

export default new AttendanceReportsService();
