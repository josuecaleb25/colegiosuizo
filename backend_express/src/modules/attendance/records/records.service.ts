import notificationService from '../../notifications/notifications.service';
import recordsRepository from './records.repository';

const LIMA_TIME_ZONE = 'America/Lima';

function personFromStudent(student: any) {
  return Array.isArray(student?.personas) ? student.personas[0] : student?.personas;
}

class AttendanceRecordsService {
  async registerManual(input: { sessionId: string; studentId: string; status: string; observations?: string }) {
    const student = await recordsRepository.findStudentById(input.studentId);
    if (!student) throw this.error('STUDENT_NOT_FOUND', 404, 'Alumno no encontrado');
    const session = await recordsRepository.findSession(input.sessionId);
    if (!session) throw this.error('SESSION_NOT_FOUND', 404, 'Sesión no encontrada');
    if (session.estado !== 'abierta') throw this.error('SESSION_CLOSED', 409, 'La sesión ya fue cerrada');

    const existing = await recordsRepository.findExisting(student.persona_id, session.fecha, input.sessionId);
    if (existing) throw this.error('ATTENDANCE_ALREADY_EXISTS', 400, 'Ya existe asistencia registrada para este alumno en esta sesión');

    const now = new Date();
    const date = now.toLocaleDateString('en-CA', { timeZone: LIMA_TIME_ZONE });
    const time = now.toLocaleTimeString('en-GB', { timeZone: LIMA_TIME_ZONE, hour12: false });
    const data = await recordsRepository.insert({
      personaId: student.persona_id,
      date,
      time,
      status: input.status,
      sessionId: input.sessionId,
      observations: input.observations
    });

    const person = personFromStudent(student);
    await this.notify(student.id, person, input.status, date, data?.id);
    return data;
  }

  async registerAbsencesBatch(absences: Array<{ persona_id: string }>, date: string) {
    const result = { guardados: 0, errores: 0, notificaciones: 0, detalles: [] as string[] };
    const students = await recordsRepository.findActiveStudents();
    const studentByPersona = new Map(students.map((student: any) => [student.persona_id, student]));
    const existing = new Set((await recordsRepository.findRecordsByDate(date)).map((row: any) => row.persona_id));
    const session = await recordsRepository.findSessionByDate(date);
    const uniquePersonas = [...new Set(absences.map((item) => item.persona_id).filter(Boolean))];
    const notifications: string[] = [];
    const records = uniquePersonas.map((personaId) => {
      const student = studentByPersona.get(personaId);
      if (!student) {
        result.errores++;
        result.detalles.push(`Alumno no encontrado: ${personaId}`);
        return null;
      }
      if (existing.has(personaId)) return null;
      notifications.push(student.id);
      return {
        persona_id: personaId,
        tipo_persona: 'alumno',
        fecha: date,
        estado: 'falta',
        hora_entrada: null,
        sesion_id: session?.id || null
      };
    }).filter(Boolean) as Array<Record<string, unknown>>;

    const insertedAbsences = await recordsRepository.insertAbsences(records);
    result.guardados = records.length;
    records.forEach((record) => result.detalles.push(`Guardado: ${record.persona_id}`));
    await Promise.all(notifications.map(async (studentId) => {
      const attendance = insertedAbsences.find((record: any) => record.persona_id === studentByPersona.get(studentId)?.persona_id);
      try {
        await notificationService.enviarAEstudiante(studentId, {
          tipo: 'asistencia',
          titulo: 'Ausencia registrada',
          mensaje: `No se registró asistencia de su hijo/a el ${date}.`,
          asistenciaId: attendance?.id || null,
          datos: { alumno_id: studentId, estado: 'falta', fecha: date }
        });
        result.notificaciones++;
      } catch (error: any) {
        console.error('Failed to send absence notification:', error.message);
      }
    }));
    return result;
  }

  async registerAbsence(personaId: string, status: string | undefined, date: string) {
    const student = await recordsRepository.findStudentByPersona(personaId);
    if (!student) throw this.error('STUDENT_NOT_FOUND', 404, 'Alumno no encontrado');
    const existing = await recordsRepository.findExisting(personaId, date);
    if (existing) return { existing: true, data: null };
    const data = await recordsRepository.insert({ personaId, date, time: null, status: status || 'falta' });
    const person = personFromStudent(student);
    await this.notify(student.id, person, 'falta', date, data?.id);
    return { existing: false, data };
  }

  private async notify(studentId: string, person: any, status: string, date: string, attendanceId?: string) {
    const name = person ? `${person.nombres} ${person.apellidos}` : 'El estudiante';
    const statusText = status === 'presente' ? 'a tiempo' : status === 'tardanza' ? 'con tardanza' : status.toLowerCase();
    try {
      await notificationService.enviarAEstudiante(studentId, {
        tipo: 'asistencia',
        titulo: status === 'falta' ? 'Ausencia registrada' : 'Asistencia registrada',
        mensaje: status === 'falta'
          ? `${name} no registró asistencia el ${date}.`
          : `${name} registró asistencia ${statusText}.`,
        asistenciaId: attendanceId || null,
        datos: { alumno_id: studentId, estado: status, fecha: date }
      });
    } catch (error: any) {
      console.error('Failed to send attendance notification:', error.message);
    }
  }

  private error(code: string, status: number, message: string) {
    const error: any = new Error(message);
    error.code = code;
    error.status = status;
    return error;
  }
}

export default new AttendanceRecordsService();
