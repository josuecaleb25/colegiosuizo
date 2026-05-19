import { messaging } from '../config/firebase';
import supabase from '../config/database';

interface NotificationData {
  tipo: 'asistencia' | 'comunicado' | 'calificacion';
  titulo: string;
  mensaje: string;
  datos?: Record<string, string>;
}

class NotificationService {
  
  /**
   * Verificar si Firebase está disponible
   */
  private isFirebaseAvailable(): boolean {
    if (!messaging) {
      console.warn('⚠️  Firebase no está inicializado. Notificación no enviada.');
      return false;
    }
    return true;
  }

  /**
   * Enviar notificación a un estudiante específico
   */
  async enviarAEstudiante(estudianteId: string, notificacion: NotificationData) {
    if (!this.isFirebaseAvailable()) {
      return { success: false, message: 'Firebase no disponible' };
    }

    try {
      // Buscar tokens del estudiante
      const { data: tokens, error } = await supabase
        .from('device_tokens')
        .select('token')
        .eq('estudiante_id', estudianteId);

      if (error) throw error;

      if (!tokens || tokens.length === 0) {
        // Silencioso: no mostrar log si no hay tokens
        return { success: false, message: 'No hay dispositivos registrados' };
      }

      // Enviar a todos los dispositivos del estudiante
      const promesas = tokens.map(({ token }) =>
        messaging!.send({
          token,
          notification: {
            title: notificacion.titulo,
            body: notificacion.mensaje
          },
          data: {
            tipo: notificacion.tipo,
            ...notificacion.datos
          },
          android: {
            priority: 'high',
            notification: {
              sound: 'default',
              channelId: 'asistencia_channel'
            }
          }
        }).catch(err => {
          console.error(`Error enviando a token ${token.substring(0, 20)}...:`, err.message);
          return null;
        })
      );

      const resultados = await Promise.all(promesas);
      const exitosos = resultados.filter(r => r !== null).length;
      
      console.log(`✅ Notificación enviada a estudiante ${estudianteId}: ${exitosos}/${tokens.length} dispositivos`);
      return { success: true, enviados: exitosos, total: tokens.length };
      
    } catch (error: any) {
      console.error('Error al enviar notificación:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * Enviar notificación a múltiples estudiantes
   */
  async enviarAMultiplesEstudiantes(estudianteIds: string[], notificacion: NotificationData) {
    if (!this.isFirebaseAvailable()) {
      return { success: false, message: 'Firebase no disponible' };
    }

    try {
      // Buscar todos los tokens
      const { data: tokens, error } = await supabase
        .from('device_tokens')
        .select('token')
        .in('estudiante_id', estudianteIds);

      if (error) throw error;

      if (!tokens || tokens.length === 0) {
        console.log('⚠️  No hay tokens para los estudiantes especificados');
        return { success: false, message: 'No hay dispositivos registrados' };
      }

      // Enviar a todos
      const promesas = tokens.map(({ token }) =>
        messaging!.send({
          token,
          notification: {
            title: notificacion.titulo,
            body: notificacion.mensaje
          },
          data: {
            tipo: notificacion.tipo,
            ...notificacion.datos
          },
          android: {
            priority: 'high',
            notification: {
              sound: 'default',
              channelId: 'asistencia_channel'
            }
          }
        }).catch(err => {
          console.error(`Error enviando a token:`, err.message);
          return null;
        })
      );

      const resultados = await Promise.all(promesas);
      const exitosos = resultados.filter(r => r !== null).length;
      
      console.log(`✅ Notificaciones enviadas: ${exitosos}/${tokens.length}`);
      return { success: true, enviados: exitosos, total: tokens.length };
      
    } catch (error: any) {
      console.error('Error al enviar notificaciones:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * Enviar notificación a toda una sección
   */
  async enviarASeccion(seccionId: string, notificacion: NotificationData) {
    if (!this.isFirebaseAvailable()) {
      return { success: false, message: 'Firebase no disponible' };
    }

    try {
      console.log(`🔍 Buscando alumnos de sección ID: ${seccionId}`);
      
      // Obtener todos los alumnos de la sección a través de matriculas
      const { data: matriculas, error } = await supabase
        .from('matriculas')
        .select('alumno_id')
        .eq('seccion_id', seccionId);

      if (error) {
        console.error('❌ Error al buscar matrículas:', error);
        throw error;
      }

      console.log(`📊 Matrículas encontradas en sección ${seccionId}: ${matriculas?.length || 0}`);

      if (!matriculas || matriculas.length === 0) {
        console.log(`⚠️  No hay alumnos matriculados en la sección ${seccionId}`);
        return { success: false, message: 'No hay alumnos en esta sección' };
      }

      const estudianteIds = matriculas.map(m => m.alumno_id);
      console.log(`📤 Enviando notificación "${notificacion.titulo}" a ${estudianteIds.length} alumnos de la sección ${seccionId}`);
      console.log(`👥 IDs de alumnos: ${estudianteIds.join(', ')}`);
      
      return await this.enviarAMultiplesEstudiantes(estudianteIds, notificacion);
      
    } catch (error: any) {
      console.error('Error al enviar a sección:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * Enviar notificación a TODOS los usuarios
   */
  async enviarATodos(notificacion: NotificationData) {
    if (!this.isFirebaseAvailable()) {
      return { success: false, message: 'Firebase no disponible' };
    }

    try {
      const { data: tokens, error } = await supabase
        .from('device_tokens')
        .select('token');

      if (error) throw error;

      if (!tokens || tokens.length === 0) {
        return { success: false, message: 'No hay dispositivos registrados' };
      }

      // Enviar en lotes de 500 (límite de FCM)
      const lotes = this.dividirEnLotes(tokens.map(t => t.token), 500);
      let totalEnviados = 0;
      
      for (const lote of lotes) {
        const promesas = lote.map(token =>
          messaging!.send({
            token,
            notification: {
              title: notificacion.titulo,
              body: notificacion.mensaje
            },
            data: {
              tipo: notificacion.tipo,
              ...notificacion.datos
            },
            android: {
              priority: 'high',
              notification: {
                sound: 'default',
                channelId: 'asistencia_channel'
              }
            }
          }).catch(err => null)
        );

        const resultados = await Promise.all(promesas);
        totalEnviados += resultados.filter(r => r !== null).length;
      }

      console.log(`✅ Notificación enviada a ${totalEnviados}/${tokens.length} dispositivos`);
      return { success: true, enviados: totalEnviados, total: tokens.length };
      
    } catch (error: any) {
      console.error('Error al enviar a todos:', error.message);
      return { success: false, error: error.message };
    }
  }

  private dividirEnLotes<T>(array: T[], tamano: number): T[][] {
    const lotes: T[][] = [];
    for (let i = 0; i < array.length; i += tamano) {
      lotes.push(array.slice(i, i + tamano));
    }
    return lotes;
  }
}

export default new NotificationService();
