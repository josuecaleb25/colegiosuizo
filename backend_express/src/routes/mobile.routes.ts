import { Router } from 'express';
import supabase from '../config/database';
import { optionalAuthMiddleware, AuthRequest } from '../middleware/auth';
import notificationService from '../services/notification.service';

const router = Router();

// Test endpoint
router.get('/test', (req, res) => {
  res.json({ 
    success: true, 
    message: 'Endpoint móvil funcionando correctamente',
    timestamp: new Date().toISOString()
  });
});

// Test usuarios sin autenticación
router.get('/test-usuarios', async (req, res) => {
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
router.get('/usuarios', async (req, res) => {
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

    const { data: alumnos, error } = await query.limit(200);

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
router.get('/asistencia/alumnos', optionalAuthMiddleware, async (req: AuthRequest, res) => {
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

    const { data: alumnos, error } = await query.limit(200);

    if (error) throw error;

    // Filtrar por secciones permitidas si es profesor
    let alumnosFiltrados = alumnos || [];
    if (user && user.rol === 'profesor' && seccionesPermitidas.length > 0) {
      alumnosFiltrados = alumnosFiltrados.filter((a: any) => {
        const matricula = a.matriculas?.[0];
        return seccionesPermitidas.includes(matricula?.seccion_id);
      });
    }

    // NO devolver asistencias del día, solo la lista de alumnos
    // El panel de asistencia debe empezar siempre desde cero

    const alumnosFormateados = alumnosFiltrados.map((a: any) => {
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

// Escanear QR
router.post('/asistencia/escanear-qr', async (req, res) => {
  try {
    const { qr_token } = req.body;


    if (!qr_token) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere qr_token'
      });
    }

    // Buscar código QR
    const { data: codigosQr, error: qrError } = await supabase
      .from('codigos_qr')
      .select(`
        persona_id,
        personas!inner (
          id,
          nombres,
          apellidos
        )
      `)
      .eq('codigo', qr_token)
      .eq('activo', true)
      .limit(1);

    if (qrError || !codigosQr || codigosQr.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'QR no válido'
      });
    }

    const personaData = codigosQr[0]?.personas as any;
    const hoy = new Date().toISOString().split('T')[0];
    const ahora = new Date();
    const hora = ahora.getHours();
    const minutos = ahora.getMinutes();
    const estado = (hora < 7 || (hora === 7 && minutos <= 31)) ? 'presente' : 'tardanza';


    // Verificar si ya tiene asistencia HOY (no días anteriores)
    const { data: asistenciaExistente, error: asistError } = await supabase
      .from('asistencias')
      .select('hora_entrada, estado')
      .eq('persona_id', personaData?.id)
      .eq('fecha', hoy)  // SOLO HOY
      .limit(1);

    if (asistError) {
      console.error('Error verificando asistencia:', asistError);
    }

    if (asistenciaExistente && asistenciaExistente.length > 0) {
      return res.status(400).json({
        success: false,
        message: `Este QR ya fue escaneado hoy a las ${asistenciaExistente[0].hora_entrada}`
      });
    }


    // Registrar asistencia
    const { error: insertError } = await supabase
      .from('asistencias')
      .insert({
        persona_id: personaData?.id,
        tipo_persona: 'alumno',
        fecha: hoy,
        hora_entrada: ahora.toTimeString().split(' ')[0],
        estado
      });

    if (insertError) {
      console.error('❌ Error insertando asistencia:', insertError);
      throw insertError;
    }

    const horaFormateada = ahora.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    

    // ========================================
    // ENVIAR NOTIFICACIÓN PUSH AL ALUMNO
    // ========================================
    try {
      
      // Buscar el alumno_id usando persona_id
      const { data: alumno, error: alumnoError } = await supabase
        .from('alumnos')
        .select('id')
        .eq('persona_id', personaData?.id)
        .single();

      if (alumnoError || !alumno) {
      } else {
        
        const nombreCompleto = `${personaData?.nombres} ${personaData?.apellidos}`;
        const estadoTexto = estado === 'presente' ? 'a tiempo' : 'con tardanza';
        
        // Enviar notificación usando el servicio
        await notificationService.enviarAEstudiante(alumno.id, {
          tipo: 'asistencia',
          titulo: '✅ Asistencia Registrada',
          mensaje: `Buenos días, su hijo/a ${nombreCompleto} llegó ${estadoTexto} a las ${horaFormateada}`,
          datos: {
            alumno_id: alumno.id.toString(),
            estado: estado,
            hora: horaFormateada,
            fecha: hoy
          }
        });
        
      }
    } catch (notifError: any) {
      console.error('❌ Error enviando notificación:', notifError.message);
    }
    // ========================================

    res.json({
      success: true,
      message: 'Asistencia registrada exitosamente',
      data: {
        alumno: `${personaData?.nombres} ${personaData?.apellidos}`,
        estado,
        hora: horaFormateada
      }
    });
  } catch (error: any) {
    console.error('❌ Error en escanear-qr:', error);
    res.status(500).json({
      success: false,
      message: 'Error al registrar asistencia',
      error: error.message
    });
  }
});

// GET /api/mobile/asistencia/dias-asistidos/:personaId - Obtener días asistidos de un alumno
router.get('/asistencia/dias-asistidos/:personaId', async (req, res) => {
  try {
    const { personaId } = req.params;
    const { semana } = req.query; // Opcional: filtrar por semana actual

    let fechaInicio: string;
    let fechaFin: string;

    if (semana === 'actual') {
      // Obtener lunes y viernes de la semana actual
      const hoy = new Date();
      const diaSemana = hoy.getDay(); // 0=domingo, 1=lunes, etc.
      const diasDesdeLunes = diaSemana === 0 ? 6 : diaSemana - 1; // Ajustar para que lunes sea 0
      
      const lunes = new Date(hoy);
      lunes.setDate(hoy.getDate() - diasDesdeLunes);
      fechaInicio = lunes.toISOString().split('T')[0];
      
      const viernes = new Date(lunes);
      viernes.setDate(lunes.getDate() + 4); // Lunes + 4 días = Viernes
      fechaFin = viernes.toISOString().split('T')[0];
    } else {
      // Por defecto, últimos 30 días
      const hoy = new Date();
      fechaFin = hoy.toISOString().split('T')[0];
      const hace30Dias = new Date(hoy);
      hace30Dias.setDate(hoy.getDate() - 30);
      fechaInicio = hace30Dias.toISOString().split('T')[0];
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

    if (semana === 'actual' && asistencias) {
      asistencias.forEach(asistencia => {
        const fecha = new Date(asistencia.fecha + 'T00:00:00');
        const dia = fecha.getDay(); // 0=domingo, 1=lunes, etc.
        const nombreDia = diasSemana[dia];
        
        if (nombreDia in asistenciasSemana) {
          asistenciasSemana[nombreDia] = true;
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