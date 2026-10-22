import { Router } from 'express';
import supabase from '../config/database';
import { authMiddleware, AuthRequest, requireRoles, requireSelfOrAdmin } from '../middleware/auth';
import notificationService from '../modules/notifications/notifications.service';
import { scanAttendanceQr } from '../modules/attendance/qr/qr.controller';
import { attendanceNotificationTitle, buildAttendanceMessage, formatAttendanceTime } from '../modules/attendance/attendance-notification-text';
import { getLimaDate } from '../services/attendance-session.service';

const router = Router();

router.post('/asistencia/escanear-qr', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), scanAttendanceQr);

// Test endpoint
router.get('/test', (req, res) => {
  res.json({ 
    success: true, 
    message: 'Endpoint móvil funcionando correctamente',
    timestamp: new Date().toISOString()
  });
});

// Test usuarios sin autenticación
router.get('/test-usuarios', authMiddleware, requireRoles('administrador', 'admin'), async (req, res) => {
  try {
    const { data: alumnos, error } = await supabase
      .from('alumnos')
      .select(`
        id,
        codigo_alumno,
        personas!inner (
          nombres,
          apellidos
        )
      `)
      .limit(5);

    if (error) throw error;

    res.json({
      success: true,
      message: 'Test usuarios exitoso',
      data: alumnos || [],
      total: alumnos?.length || 0
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error en test usuarios',
      error: error.message
    });
  }
});

// Obtener todos los alumnos con QR (sin autenticación)
router.get('/usuarios', authMiddleware, requireRoles('administrador', 'admin'), async (req, res) => {
  try {
    const { seccion, search } = req.query;
    
    let query = supabase
      .from('alumnos')
      .select(`
        id,
        codigo_alumno,
        personas!inner (
          id,
          nombres,
          apellidos,
          correo,
          codigos_qr (
            codigo
          )
        ),
        matriculas!inner (
          secciones!inner (
            nombre,
            grados!inner (
              nombre
            )
          )
        )
      `)
      .eq('estado', 'activo');
    const { data: alumnos, error } = await query;

    if (error) throw error;

    // Formatear respuesta
    const alumnosFormateados = alumnos?.map((a: any) => {
      const matricula = a.matriculas?.[0];
      const seccionNombre = matricula?.secciones?.nombre || '';
      const gradoNombre = matricula?.secciones?.grados?.nombre || '';
      
      return {
        id: a.id,
        codigo: a.codigo_alumno,
        nombre_completo: `${a.personas.nombres} ${a.personas.apellidos}`,
        salon: `${gradoNombre} ${seccionNombre}`,
        qr_token: a.personas.codigos_qr?.[0]?.codigo || null,
        email: a.personas.correo || `${a.personas.nombres.toLowerCase()}.${a.personas.apellidos.toLowerCase()}@peruanosuizo.edu.pe`
      };
    }) || [];

    res.json({
      success: true,
      message: 'Usuarios obtenidos exitosamente',
      data: alumnosFormateados,
      total: alumnosFormateados.length
    });

  } catch (error: any) {
    console.error('Error al obtener usuarios:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener usuarios',
      error: error.message
    });
  }
});

// Obtener alumnos para asistencia
router.get('/asistencia/alumnos', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), async (req: AuthRequest, res) => {
  try {
    const { salon, search } = req.query;
    const user = req.user;
    
    // Si es profesor, obtener sus secciones asignadas
    let seccionesPermitidas: string[] = [];
    
    if (user && user.rol === 'profesor') {
      // Buscar el docente por persona_id
      const { data: personas } = await supabase
        .from('personas')
        .select('id')
        .eq('correo', user.email)
        .single();

      if (personas) {
        const { data: docente } = await supabase
          .from('docentes')
          .select('id')
          .eq('persona_id', personas.id)
          .single();

        if (docente) {
          // Obtener secciones asignadas al docente
          const { data: asignaciones } = await supabase
            .from('asignaciones')
            .select('seccion_id')
            .eq('docente_id', docente.id);

          seccionesPermitidas = asignaciones?.map((a: any) => a.seccion_id) || [];
        }
      }

      if (seccionesPermitidas.length === 0) {
        return res.json({ success: true, data: [], total: 0, message: 'No hay secciones asignadas' });
      }
    }
    
    let query = supabase
      .from('alumnos')
      .select(`
        id,
        codigo_alumno,
        personas!inner (
          id,
          nombres,
          apellidos
        ),
        matriculas!inner (
          seccion_id,
          secciones!inner (
            nombre,
            grados!inner (
              nombre
            )
          )
        )
      `)
      .eq('estado', 'activo');

    if (user?.rol === 'profesor') {
      query = query.in('matriculas.seccion_id', seccionesPermitidas);
    }

    const { data: alumnos, error } = await query;

    if (error) throw error;

    const alumnosFormateados = (alumnos || []).map((a: any) => {
      const matricula = a.matriculas?.[0];
      const seccionNombre = matricula?.secciones?.nombre || '';
      const gradoNombre = matricula?.secciones?.grados?.nombre || '';
      
      return {
        id: a.id,
        persona_id: a.personas.id,
        nombre_completo: `${a.personas.nombres} ${a.personas.apellidos}`,
        salon: `${gradoNombre} ${seccionNombre}`,
        hora_registro: null,  // Siempre null
        estado_entrada: 'ausente'  // Siempre ausente
      };
    });

    res.json({
      success: true,
      data: alumnosFormateados,
      total: alumnosFormateados.length,
      message: `Se encontraron ${alumnosFormateados.length} alumnos`
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumnos',
      error: error.message
    });
  }
});

router.get('/asistencia/dias-asistidos/:personaId', authMiddleware, requireSelfOrAdmin(req => req.params.personaId), async (req, res) => {
  try {
    const { personaId } = req.params;
    const { semana } = req.query; // Opcional: filtrar por semana actual

    let fechaInicio: string;
    let fechaFin: string;
    const hoyLima = getLimaDate();
    const fechaDesdeMediodia = (date: Date) => date.toISOString().split('T')[0];

    if (semana === 'actual') {
      // Obtener lunes y viernes de la semana actual
      const hoy = new Date(`${hoyLima}T12:00:00Z`);
      const diaSemana = hoy.getUTCDay(); // 0=domingo, 1=lunes, etc.
      const diasDesdeLunes = diaSemana === 0 ? 6 : diaSemana - 1; // Ajustar para que lunes sea 0
      
      const lunes = new Date(hoy);
      lunes.setUTCDate(hoy.getUTCDate() - diasDesdeLunes);
      fechaInicio = fechaDesdeMediodia(lunes);
      
      const viernes = new Date(lunes);
      viernes.setUTCDate(lunes.getUTCDate() + 4); // Lunes + 4 días = Viernes
      fechaFin = fechaDesdeMediodia(viernes);
    } else {
      // Por defecto, últimos 30 días
      fechaFin = hoyLima;
      const hace30Dias = new Date(`${hoyLima}T12:00:00Z`);
      hace30Dias.setUTCDate(hace30Dias.getUTCDate() - 30);
      fechaInicio = fechaDesdeMediodia(hace30Dias);
    }

    // Obtener asistencias del alumno (presente y tardanza cuentan)
    const { data: asistencias, error } = await supabase
      .from('asistencias')
      .select('fecha, estado, hora_entrada')
      .eq('persona_id', personaId)
      .gte('fecha', fechaInicio)
      .lte('fecha', fechaFin)
      .order('fecha', { ascending: false });

    if (error) throw error;

    // Calcular estadísticas
    const totalDias = asistencias?.length || 0;
    const diasPresente = asistencias?.filter(a => a.estado === 'presente').length || 0;
    const diasTardanza = asistencias?.filter(a => a.estado === 'tardanza').length || 0;
    
    // Calcular racha actual (días consecutivos de asistencia)
    let rachaActual = 0;
    if (asistencias && asistencias.length > 0) {
      // Obtener todas las asistencias ordenadas de más reciente a más antigua
      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);
      
      // Verificar si hoy es día escolar (lunes a viernes)
      const diaHoy = hoy.getDay();
      const esFinDeSemana = diaHoy === 0 || diaHoy === 6;
      
      // Empezar desde el último día escolar
      let fechaActual = new Date(hoy);
      
      // Si hoy es fin de semana, retroceder al viernes
      if (esFinDeSemana) {
        if (diaHoy === 0) { // Domingo
          fechaActual.setDate(fechaActual.getDate() - 2); // Retroceder a viernes
        } else { // Sábado
          fechaActual.setDate(fechaActual.getDate() - 1); // Retroceder a viernes
        }
      }
      
      // Contar días consecutivos de asistencia
      for (const asistencia of asistencias) {
        const fechaAsistencia = new Date(asistencia.fecha + 'T00:00:00');
        fechaAsistencia.setHours(0, 0, 0, 0);
        const fechaEsperada = fechaActual.toISOString().split('T')[0];
        const fechaAsist = fechaAsistencia.toISOString().split('T')[0];
        
        if (fechaAsist === fechaEsperada) {
          rachaActual++;
          // Retroceder al siguiente día escolar
          fechaActual.setDate(fechaActual.getDate() - 1);
          // Saltar fines de semana
          while (fechaActual.getDay() === 0 || fechaActual.getDay() === 6) {
            fechaActual.setDate(fechaActual.getDate() - 1);
          }
        } else {
          // Si no hay asistencia en el día esperado, la racha se rompe
          break;
        }
      }
    }

    // Obtener días de la semana actual con asistencia
    const diasSemana = ['domingo', 'lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado'];
    const asistenciasSemana: { [key: string]: boolean } = {
      lunes: false,
      martes: false,
      miercoles: false,
      jueves: false,
      viernes: false
    };
    const estadosSemana: { [key: string]: string | null } = {
      lunes: null,
      martes: null,
      miercoles: null,
      jueves: null,
      viernes: null
    };

    const normalizarEstado = (estado: string | null) => {
      if (estado === 'ausente') return 'falta';
      if (estado === 'presente' || estado === 'tardanza' || estado === 'falta') return estado;
      return null;
    };

    if (semana === 'actual' && asistencias) {
      asistencias.forEach(asistencia => {
        const fecha = new Date(asistencia.fecha + 'T12:00:00Z');
        const dia = fecha.getUTCDay(); // 0=domingo, 1=lunes, etc.
        const nombreDia = diasSemana[dia];
        
        if (nombreDia in asistenciasSemana) {
          const estado = normalizarEstado(asistencia.estado);
          estadosSemana[nombreDia] = estado;
          asistenciasSemana[nombreDia] = estado === 'presente' || estado === 'tardanza';
        }
      });
    }

    res.json({
      success: true,
      data: {
        total_dias: totalDias,
        dias_presente: diasPresente,
        dias_tardanza: diasTardanza,
        racha_actual: rachaActual,
        asistencias_semana: asistenciasSemana,
        estados_semana: estadosSemana,
        historial: asistencias?.map(a => ({
          fecha: a.fecha,
          estado: a.estado,
          hora_entrada: a.hora_entrada
        })) || []
      }
    });
  } catch (error: any) {
    console.error('Error al obtener días asistidos:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener días asistidos',
      error: error.message
    });
  }
});

export default router;
