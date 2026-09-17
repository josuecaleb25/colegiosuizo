import { messaging } from '../config/firebase';
import supabase from '../config/database';

interface NotificationData {
  tipo: 'asistencia' | 'comunicado' | 'calificacion';
  titulo: string;
  mensaje: string;
  datos?: Record<string, string>;
  asistenciaId?: string | null;
}

interface NotificationRecipient {
  personaId: string;
  estudianteId?: string;
}

class NotificationService {
  private isFirebaseAvailable(): boolean {
    if (!messaging) {
      console.warn('Firebase no está inicializado. Notificación no enviada.');
      return false;
    }
    return true;
  }

  // QR/asistencia: únicamente la cuenta asociada al alumno.
  async enviarAEstudiante(estudianteId: string, notificacion: NotificationData) {
    const recipients = await this.obtenerDestinatariosDeAlumnos([estudianteId]);
    if (recipients.length === 0) {
      return { success: false, message: 'No se encontró la cuenta del alumno' };
    }
    const nuevosDestinatarios = await this.guardarHistorial(recipients, notificacion);
    return this.enviarPush(nuevosDestinatarios, notificacion);
  }

  async enviarAMultiplesEstudiantes(estudianteIds: string[], notificacion: NotificationData) {
    const recipients = await this.obtenerDestinatariosDeAlumnos(estudianteIds);
    const nuevosDestinatarios = await this.guardarHistorial(recipients, notificacion);
    return this.enviarPush(nuevosDestinatarios, notificacion);
  }

  async enviarAusencias(registros: Array<{ estudianteId: string; asistenciaId: string; fecha: string }>) {
    const unicosRegistros = [...new Map(registros.map((registro) => [registro.asistenciaId, registro])).values()];
    if (unicosRegistros.length === 0) return { success: true, enviados: 0, total: 0 };

    const studentIds = unicosRegistros.map((registro) => registro.estudianteId);
    const recipients = await this.obtenerDestinatariosDeAlumnos(studentIds);
    const recipientByStudent = new Map(
      recipients.map((recipient) => [recipient.estudianteId, recipient])
    );
    const attendanceIds = unicosRegistros.map((registro) => registro.asistenciaId);

    const { data: existentes, error: existentesError } = await supabase
      .from('notificaciones_historial')
      .select('asistencia_id, persona_id')
      .eq('tipo', 'asistencia')
      .in('asistencia_id', attendanceIds);
    if (existentesError) throw existentesError;

    const notificados = new Set((existentes || []).map((row: any) =>
      `${row.asistencia_id}:${row.persona_id}`
    ));
    const pendientes = unicosRegistros
      .map((registro) => ({
        registro,
        recipient: recipientByStudent.get(registro.estudianteId)
      }))
      .filter((item) => item.recipient)
      .filter(({ registro, recipient }) =>
        !notificados.has(`${registro.asistenciaId}:${recipient!.personaId}`)
      ) as Array<{
        registro: { estudianteId: string; asistenciaId: string; fecha: string };
        recipient: NotificationRecipient;
      }>;

    if (pendientes.length === 0) return { success: true, enviados: 0, total: 0 };

    const history = pendientes.map(({ registro, recipient }) => ({
      persona_id: recipient.personaId,
      estudiante_id: recipient.estudianteId || null,
      asistencia_id: registro.asistenciaId,
      tipo: 'asistencia',
      titulo: 'Ausencia registrada',
      mensaje: `No se registró asistencia de su hijo/a el ${registro.fecha}.`,
      datos: {
        alumno_id: registro.estudianteId,
        estado: 'falta',
        fecha: registro.fecha
      }
    }));
    const { error: historyError } = await supabase
      .from('notificaciones_historial')
      .insert(history);
    if (historyError) throw historyError;

    if (!this.isFirebaseAvailable()) {
      return { success: false, message: 'Firebase no disponible', enviados: 0, total: pendientes.length };
    }

    const personaIds = [...new Set(pendientes.map(({ recipient }) => recipient.personaId))];
    const tokenBatches = await Promise.all(this.dividirEnLotes(personaIds, 100).map(async (batch) => {
      const { data, error } = await supabase
        .from('device_tokens')
        .select('token, persona_id')
        .in('persona_id', batch);
      if (error) throw error;
      return data || [];
    }));
    const tokens = tokenBatches.flat();
    const pendingByPersona = new Map(
      pendientes.map(({ registro, recipient }) => [recipient.personaId, registro])
    );

    let enviados = 0;
    for (const lote of this.dividirEnLotes(tokens, 500)) {
      const resultados = await Promise.all(lote.map((row: { token: string; persona_id: string }) => {
        const registro = pendingByPersona.get(row.persona_id);
        if (!registro) return Promise.resolve(null);
        return messaging!.send({
          token: row.token,
          notification: {
            title: 'Ausencia registrada',
            body: `No se registró asistencia de su hijo/a el ${registro.fecha}.`
          },
          data: {
            tipo: 'asistencia',
            asistencia_id: registro.asistenciaId,
            alumno_id: registro.estudianteId,
            estado: 'falta',
            fecha: registro.fecha
          },
          android: { priority: 'high', notification: { sound: 'default', channelId: 'asistencia_channel' } }
        }).then(() => true).catch((error: any) => {
          if (this.esTokenFCMInvalido(error)) return this.eliminarTokenInvalido(row.token);
          console.error(`Failed to send absence notification to token ${row.token.substring(0, 20)}...:`, error.message);
          return null;
        });
      }));
      enviados += resultados.filter(Boolean).length;
    }

    return { success: true, enviados, total: tokens.length };
  }

  // Comunicado dirigido a alumnos de la sección y docentes asignados.
  async enviarASeccion(seccionId: string, notificacion: NotificationData) {
    try {
      const { data: matriculas, error: matriculasError } = await supabase
        .from('matriculas').select('alumno_id').eq('seccion_id', seccionId);
      if (matriculasError) throw matriculasError;

      const studentIds = (matriculas || []).map((row: { alumno_id: string }) => row.alumno_id);
      const studentRecipients = await this.obtenerDestinatariosDeAlumnos(studentIds);

      const { data: asignaciones, error: asignacionesError } = await supabase
        .from('asignaciones').select('docente_id').eq('seccion_id', seccionId);
      if (asignacionesError) throw asignacionesError;

      const teacherIds = [...new Set((asignaciones || [])
        .map((row: { docente_id: string }) => row.docente_id).filter(Boolean))];
      const teacherRecipients = await this.obtenerDestinatariosDeDocentes(teacherIds);
      const recipients = this.unicos([...studentRecipients, ...teacherRecipients]);

      if (recipients.length === 0) {
        return { success: false, message: 'No hay destinatarios en esta sección' };
      }
      const nuevosDestinatarios = await this.guardarHistorial(recipients, notificacion);
      return this.enviarPush(nuevosDestinatarios, notificacion);
    } catch (error: any) {
      console.error('Error al enviar comunicado a sección:', error.message);
      return { success: false, error: error.message };
    }
  }

  // Comunicado global: alumnos, docentes y administradores activos.
  async enviarATodos(notificacion: NotificationData) {
    const recipients: NotificationRecipient[] = [];

    const { data: alumnos, error: alumnosError } = await supabase
      .from('alumnos').select('id, persona_id, estado');
    if (alumnosError) throw alumnosError;
    for (const alumno of alumnos || []) {
      if (alumno.estado !== 'inactivo' && alumno.persona_id) {
        recipients.push({ personaId: alumno.persona_id, estudianteId: alumno.id });
      }
    }

    const { data: docentes, error: docentesError } = await supabase
      .from('docentes').select('persona_id, estado');
    if (docentesError) throw docentesError;
    for (const docente of docentes || []) {
      if (docente.estado !== 'inactivo' && docente.persona_id) {
        recipients.push({ personaId: docente.persona_id });
      }
    }

    const { data: admins, error: adminsError } = await supabase
      .from('usuarios').select('persona_id, rol, activo')
      .in('rol', ['administrador', 'admin']);
    if (adminsError) throw adminsError;
    for (const admin of admins || []) {
      if (admin.activo !== false && admin.persona_id) {
        recipients.push({ personaId: admin.persona_id });
      }
    }

    const uniqueRecipients = this.unicos(recipients);
    const nuevosDestinatarios = await this.guardarHistorial(uniqueRecipients, notificacion);
    return this.enviarPush(nuevosDestinatarios, notificacion);
  }

  private async obtenerDestinatariosDeAlumnos(estudianteIds: string[]) {
    const ids = [...new Set(estudianteIds.filter(Boolean))];
    if (ids.length === 0) return [];
    const { data, error } = await supabase.from('alumnos')
      .select('id, persona_id').in('id', ids);
    if (error) throw error;
    return (data || []).filter((alumno: any) => alumno.persona_id)
      .map((alumno: any) => ({ personaId: alumno.persona_id, estudianteId: alumno.id }));
  }

  private async obtenerDestinatariosDeDocentes(docenteIds: string[]) {
    if (docenteIds.length === 0) return [];
    const { data, error } = await supabase.from('docentes')
      .select('persona_id').in('id', docenteIds);
    if (error) throw error;
    return (data || []).filter((docente: any) => docente.persona_id)
      .map((docente: any) => ({ personaId: docente.persona_id }));
  }

  private unicos(recipients: NotificationRecipient[]) {
    const byPersona = new Map<string, NotificationRecipient>();
    for (const recipient of recipients) {
      if (!recipient.personaId) continue;
      const current = byPersona.get(recipient.personaId);
      byPersona.set(recipient.personaId, {
        personaId: recipient.personaId,
        estudianteId: current?.estudianteId || recipient.estudianteId
      });
    }
    return [...byPersona.values()];
  }

  private async enviarPush(recipients: NotificationRecipient[], notificacion: NotificationData) {
    if (!this.isFirebaseAvailable()) return { success: false, message: 'Firebase no disponible' };
    if (recipients.length === 0) return { success: false, message: 'No hay destinatarios' };

    try {
      const personaIds = recipients.map((recipient) => recipient.personaId);
      // Supabase serializa .in() en la URL. Un comunicado global puede
      // incluir cientos de destinatarios, así que se consulta por lotes.
      const personaBatches = this.dividirEnLotes(personaIds, 100);
      const tokenResults = await Promise.all(personaBatches.map(async (batch) => {
        const { data, error } = await supabase.from('device_tokens')
          .select('token').in('persona_id', batch);
        if (error) throw error;
        return data || [];
      }));
      const tokens = [...new Map(tokenResults.flat()
        .map((row: { token: string }) => [row.token, row])).values()];
      if (!tokens || tokens.length === 0) {
        return { success: false, message: 'No hay dispositivos registrados' };
      }

      const lotes = this.dividirEnLotes(tokens.map((row: { token: string }) => row.token), 500);
      let enviados = 0;
      for (const lote of lotes) {
        const resultados = await Promise.all(lote.map((token) => messaging!.send({
          token,
          notification: { title: notificacion.titulo, body: notificacion.mensaje },
          data: Object.fromEntries(Object.entries({ tipo: notificacion.tipo, ...notificacion.datos })
            .map(([key, value]) => [key, String(value ?? '')])),
          android: { priority: 'high', notification: { sound: 'default', channelId: 'asistencia_channel' } }
        }).catch((error: any) => {
          console.error(`Error enviando a token ${token.substring(0, 20)}...:`, {
            code: error.code,
            message: error.message
          });
          if (this.esTokenFCMInvalido(error)) {
            return this.eliminarTokenInvalido(token);
          }
          return null;
        })));
        enviados += resultados.filter(Boolean).length;
      }
      return { success: true, enviados, total: tokens.length };
    } catch (error: any) {
      console.error('Error al enviar notificaciones:', {
        code: error.code,
        message: error.message,
        details: error.details,
        hint: error.hint
      });
      return { success: false, error: error.message };
    }
  }

  private esTokenFCMInvalido(error: any): boolean {
    return [
      'messaging/registration-token-not-registered',
      'messaging/invalid-registration-token'
    ].includes(error?.code);
  }

  private async eliminarTokenInvalido(token: string) {
    const { error } = await supabase.from('device_tokens').delete().eq('token', token);
    if (error) {
      console.error('No se pudo eliminar el token FCM inválido:', error.message);
    } else {
      console.log('Token FCM inválido eliminado automáticamente');
    }
    return null;
  }

  private async guardarHistorial(recipients: NotificationRecipient[], notificacion: NotificationData) {
    const unicosRecipients = this.unicos(recipients);
    if (unicosRecipients.length === 0) return [];

    let nuevosRecipients = unicosRecipients;
    if (notificacion.asistenciaId) {
      const { data: existentes, error: existentesError } = await supabase
        .from('notificaciones_historial')
        .select('persona_id')
        .eq('tipo', notificacion.tipo)
        .eq('asistencia_id', notificacion.asistenciaId);
      if (existentesError) throw existentesError;

      const personasNotificadas = new Set((existentes || [])
        .map((row: { persona_id: string | null }) => row.persona_id)
        .filter(Boolean));
      nuevosRecipients = unicosRecipients.filter(
        (recipient) => !personasNotificadas.has(recipient.personaId)
      );
    }

    if (nuevosRecipients.length === 0) return [];

    const registros = nuevosRecipients.map((recipient) => ({
      persona_id: recipient.personaId,
      estudiante_id: recipient.estudianteId || null,
      asistencia_id: notificacion.asistenciaId || null,
      tipo: notificacion.tipo,
      titulo: notificacion.titulo,
      mensaje: notificacion.mensaje,
      datos: notificacion.datos || null
    }));
    const { error } = await supabase.from('notificaciones_historial').insert(registros);
    if (error) console.error('Error al guardar historial de notificaciones:', error.message);
    return nuevosRecipients;
  }

  private dividirEnLotes<T>(array: T[], tamano: number): T[][] {
    const lotes: T[][] = [];
    for (let i = 0; i < array.length; i += tamano) lotes.push(array.slice(i, i + tamano));
    return lotes;
  }
}

export default new NotificationService();
