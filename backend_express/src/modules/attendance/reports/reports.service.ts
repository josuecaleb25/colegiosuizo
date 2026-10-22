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
    return reportsRepository.findStatistics({ from, to, sectionId });
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

export default new AttendanceReportsService();
