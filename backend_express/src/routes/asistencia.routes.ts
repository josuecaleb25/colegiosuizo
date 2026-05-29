import { Router } from 'express';
import supabase from '../config/database';
import notificationService from '../services/notification.service';

const router = Router();

// Obtener asistencias por fecha
router.get('/', async (req, res) => {
  try {
    const { fecha, seccion_id, alumno_id } = req.query;
    
    let query = supabase
      .from('asistencia_asistencia')
      .select(`
        id,
        estado,
        hora_registro,
        registrado_via_qr,
        observaciones,
        alumno_id,
        sesion_id,
        asistencia_alumno!inner (
          id,
          nombre_completo,
          codigo,
          asistencia_seccion!inner (
            nombre,
            asistencia_grado!inner (
              nombre
            )
          )
        ),
        asistencia_sesionclase!inner (
          id,
          fecha,
          seccion_id,
          asistencia_curso!inner (
            nombre
          )
        )
      `);

    // Filtro por fecha
    if (fecha) {
      query = query.eq('asistencia_sesionclase.fecha', fecha);
    } else {
      const hoy = new Date().toLocaleDateString('en-CA', { timeZone: 'America/Lima' });
      query = query.eq('asistencia_sesionclase.fecha', hoy);
    }

    // Filtro por sección
    if (seccion_id) {
      query = query.eq('asistencia_sesionclase.seccion_id', seccion_id);
    }

    // Filtro por alumno
    if (alumno_id) {
      query = query.eq('alumno_id', alumno_id);
    }

    query = query.order('hora_registro', { ascending: false }).limit(500);

    const { data: asistencias, error } = await query;

    if (error) throw error;

    const asistenciasFormateadas = asistencias?.map((ast: any) => ({
      id: ast.id,
      estado: ast.estado,
      hora_registro: ast.hora_registro,
      registrado_via_qr: ast.registrado_via_qr,
      observaciones: ast.observaciones,
      alumno_id: ast.asistencia_alumno.id,
      nombre_completo: ast.asistencia_alumno.nombre_completo,
      codigo: ast.asistencia_alumno.codigo,
      salon: `${ast.asistencia_alumno.asistencia_seccion.asistencia_grado.nombre} ${ast.asistencia_alumno.asistencia_seccion.nombre}`,
      sesion_id: ast.asistencia_sesionclase.id,
      fecha: ast.asistencia_sesionclase.fecha,
      curso: ast.asistencia_sesionclase.asistencia_curso.nombre
    })) || [];

    res.json({
      success: true,
      data: asistenciasFormateadas,
      total: asistenciasFormateadas.length
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener asistencias',
      error: error.message
    });
  }
});

// Registrar asistencia manual
router.post('/', async (req, res) => {
  try {
    const { sesion_id, alumno_id, estado, observaciones } = req.body;

    if (!sesion_id || !alumno_id || !estado) {
      return res.status(400).json({
        success: false,
        message: 'Faltan campos requeridos'
      });
    }

    // Verificar si ya existe asistencia
    const { data: existente } = await supabase
      .from('asistencia_asistencia')
      .select('id')
      .eq('sesion_id', sesion_id)
      .eq('alumno_id', alumno_id)
      .limit(1);

    if (existente && existente.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'Ya existe asistencia registrada para este alumno en esta sesión'
      });
    }

    const ahoraLima = new Date();
    const fechaLima = ahoraLima.toLocaleDateString('en-CA', { timeZone: 'America/Lima' });
    const horaLima = ahoraLima.toLocaleTimeString('en-GB', { timeZone: 'America/Lima', hour12: false });

    const { data: nuevaAsistencia, error } = await supabase
      .from('asistencia_asistencia')
      .insert({
        sesion_id,
        alumno_id,
        estado,
        hora_registro: `${fechaLima}T${horaLima}.000Z`,
        registrado_via_qr: false,
        observaciones: observaciones || ''
      })
      .select()
      .single();

    if (error) throw error;

    console.log('✅ Asistencia registrada, ID:', nuevaAsistencia.id);
    console.log('📤 Intentando enviar notificación al alumno_id:', alumno_id);

    // ========================================
    // ENVIAR NOTIFICACIÓN PUSH
    // ========================================
    try {
      // Obtener información del alumno
      const { data: alumno, error: alumnoError } = await supabase
        .from('alumnos')
        .select('personas!inner(nombres, apellidos)')
        .eq('id', alumno_id)
        .single();

      if (alumnoError) {
        console.error('❌ Error al obtener datos del alumno:', alumnoError);
        throw alumnoError;
      }

      console.log('👤 Datos del alumno obtenidos:', JSON.stringify(alumno, null, 2));

      // Construir nombre completo
      const persona = Array.isArray(alumno?.personas) ? alumno.personas[0] : alumno?.personas;
      const nombreCompleto = persona 
        ? `${persona.nombres} ${persona.apellidos}`
        : 'El estudiante';

      console.log('📝 Nombre completo:', nombreCompleto);

      // Mapear estado a texto legible
      const estadoTexto = {
        'presente': 'PRESENTE',
        'tardanza': 'TARDANZA',
        'falta': 'FALTA',
        'justificado': 'JUSTIFICADO'
      }[estado] || estado.toUpperCase();

      console.log('📊 Estado:', estadoTexto);
      console.log('🔔 Enviando notificación...');

      // Enviar notificación
      const resultado = await notificationService.enviarAEstudiante(alumno_id, {
        tipo: 'asistencia',
        titulo: '📋 Asistencia Registrada',
        mensaje: `${nombreCompleto} fue marcado como ${estadoTexto}`,
        datos: {
          alumno_id: alumno_id.toString(),
          estado: estado,
          fecha: fechaLima
        }
      });

      console.log('📬 Resultado del envío:', resultado);
    } catch (notifError: any) {
      // No fallar el registro si falla la notificación
      console.error('❌ Error al enviar notificación:', notifError.message);
      console.error('Stack:', notifError.stack);
    }
    // ========================================

    res.status(201).json({
      success: true,
      message: 'Asistencia registrada exitosamente',
      data: nuevaAsistencia
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al registrar asistencia',
      error: error.message
    });
  }
});

// POST /api/asistencia/registrar-ausentes-batch - Registrar múltiples ausentes de una vez
router.post('/registrar-ausentes-batch', async (req, res) => {
  try {
    const { ausentes, fecha } = req.body;

    console.log('📦 Batch ausentes recibido:', { total: ausentes?.length, fecha });

    if (!ausentes || !Array.isArray(ausentes) || ausentes.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere un array de ausentes'
      });
    }

    if (!fecha) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere fecha'
      });
    }

    const resultados = {
      guardados: 0,
      errores: 0,
      notificaciones: 0,
      detalles: [] as string[]
    };

    const personaIds = ausentes.map(a => a.persona_id).filter(Boolean) as string[];

    if (personaIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Ningún persona_id válido'
      });
    }

    // 1. Traer todos los alumnos activos (sin filtro IN para evitar headers overflow)
    const { data: alumnos, error: alumnosError } = await supabase
      .from('alumnos')
      .select('id, persona_id')
      .eq('estado', 'activo');

    if (alumnosError) {
      console.error('❌ Error buscando alumnos:', alumnosError);
      throw alumnosError;
    }

    console.log(`✅ ${alumnos?.length || 0} alumnos activos en BD`);

    const alumnosMap = new Map<string, string>();
    for (const a of alumnos || []) {
      alumnosMap.set(a.persona_id, a.id);
    }

    // 2. Verificar quiénes ya tienen asistencia hoy (sin filtro IN, por fecha nomás)
    const { data: asistenciasHoy } = await supabase
      .from('asistencias')
      .select('persona_id')
      .eq('fecha', fecha)
      .eq('tipo_persona', 'alumno');

    const yaAsistieronHoy = new Set((asistenciasHoy || []).map(e => e.persona_id));
    console.log(`✅ ${yaAsistieronHoy.size} alumnos ya registraron asistencia hoy`);

    // 3. Preparar inserts (solo los que no asistieron hoy)
    const alumnosParaNotificar: string[] = [];
    const inserts = personaIds
      .filter(pid => !yaAsistieronHoy.has(pid))
      .map(pid => {
        const alumnoId = alumnosMap.get(pid);
        if (alumnoId) {
          alumnosParaNotificar.push(alumnoId);
          return {
            persona_id: pid,
            fecha,
            estado: 'falta',
            tipo_persona: 'alumno',
            hora_entrada: null
          };
        }
        resultados.errores++;
        resultados.detalles.push(`Alumno no encontrado: ${pid}`);
        return null;
      })
      .filter(Boolean);

    // 4. Insertar todo en UN SOLO upsert (ignora duplicados automáticamente)
    if (inserts.length > 0) {
      const { error: insertError } = await supabase
        .from('asistencias')
        .upsert(inserts, { onConflict: 'persona_id,fecha', ignoreDuplicates: true });

      if (insertError) {
        console.error('❌ Error insertando ausentes batch:', insertError);
        throw insertError;
      }

      resultados.guardados += inserts.length;
      inserts.forEach(i => resultados.detalles.push(`Guardado: ${i.persona_id}`));
      console.log(`✅ ${inserts.length} ausentes guardados en 1 upsert`);
    }

    // 5. Enviar notificaciones sin esperar
    for (const alumnoId of alumnosParaNotificar) {
      notificationService.enviarAEstudiante(alumnoId, {
        tipo: 'asistencia',
        titulo: '⚠️ Ausencia Registrada',
        mensaje: `Su hijo/a no registró asistencia hoy`,
        datos: {
          alumno_id: alumnoId.toString(),
          estado: 'falta',
          fecha: fecha
        }
      }).then(() => {
        resultados.notificaciones++;
      }).catch(err => {
        console.error('Error enviando notificación:', err.message);
      });
    }

    console.log('✅ Batch completado:', resultados);

    res.json({
      success: true,
      message: `Procesados: ${resultados.guardados} guardados, ${resultados.errores} errores`,
      data: resultados
    });
  } catch (error: any) {
    console.error('❌ Error en batch ausentes:', error);
    res.status(500).json({
      success: false,
      message: 'Error al registrar ausentes',
      error: error.message
    });
  }
});

// POST /api/asistencia/registrar-ausente - Registrar alumno ausente al culminar
router.post('/registrar-ausente', async (req, res) => {
  try {
    const { persona_id, estado, fecha } = req.body;

    if (!persona_id || !fecha) {
      return res.status(400).json({
        success: false,
        message: 'persona_id y fecha son requeridos'
      });
    }

    // Buscar el alumno_id usando persona_id
    const { data: alumno, error: alumnoError } = await supabase
      .from('alumnos')
      .select('id')
      .eq('persona_id', persona_id)
      .single();

    if (alumnoError || !alumno) {
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    const alumno_id = alumno.id;

    // Verificar si ya existe asistencia para este alumno hoy
    const { data: existente } = await supabase
      .from('asistencias')
      .select('id')
      .eq('persona_id', persona_id)
      .eq('fecha', fecha)
      .limit(1);

    if (existente && existente.length > 0) {
      return res.json({
        success: true,
        message: 'Ya existe asistencia para este alumno'
      });
    }

    // Registrar como ausente
    const { data, error } = await supabase
      .from('asistencias')
      .insert({
        persona_id,
        fecha,
        estado: estado || 'falta',
        tipo_persona: 'alumno',
        hora_entrada: null
      })
      .select()
      .single();

    if (error) throw error;

    // ========================================
    // ENVIAR NOTIFICACIÓN PUSH AL PADRE
    // ========================================
    try {
      // Obtener información del alumno
      const { data: alumnoInfo } = await supabase
        .from('alumnos')
        .select('personas!inner(nombres, apellidos)')
        .eq('id', alumno_id)
        .single();

      const persona = Array.isArray(alumnoInfo?.personas) ? alumnoInfo.personas[0] : alumnoInfo?.personas;
      const nombreCompleto = persona 
        ? `${persona.nombres} ${persona.apellidos}`
        : 'Su hijo/a';

      // Enviar notificación
      await notificationService.enviarAEstudiante(alumno_id, {
        tipo: 'asistencia',
        titulo: '⚠️ Ausencia Registrada',
        mensaje: `${nombreCompleto} no registró asistencia hoy`,
        datos: {
          alumno_id: alumno_id.toString(),
          estado: 'falta',
          fecha: fecha
        }
      });

      console.log(`📤 Notificación de ausencia enviada para alumno ${alumno_id}`);
    } catch (notifError: any) {
      console.error('Error al enviar notificación de ausencia:', notifError.message);
    }
    // ========================================

    res.json({
      success: true,
      message: 'Ausente registrado',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al registrar ausente',
      error: error.message
    });
  }
});

// POST /api/asistencia/device-token - Guardar token FCM del dispositivo
router.post('/device-token', async (req, res) => {
  try {
    console.log('📱 Petición de registro de token FCM recibida');
    console.log('📋 Body:', JSON.stringify(req.body, null, 2));
    
    const { token, user_id, estudiante_id, device_info } = req.body;

    if (!token || !estudiante_id) {
      console.log('❌ Faltan campos requeridos');
      return res.status(400).json({
        success: false,
        message: 'Token y estudiante_id son requeridos'
      });
    }

    console.log('🔍 Buscando alumno con persona_id:', estudiante_id);

    // Buscar el ID real del alumno usando el persona_id
    const { data: alumno, error: alumnoError } = await supabase
      .from('alumnos')
      .select('id')
      .eq('persona_id', estudiante_id)
      .single();

    if (alumnoError || !alumno) {
      console.error('❌ No se encontró alumno con persona_id:', estudiante_id);
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    const alumnoId = alumno.id;
    console.log('✅ Alumno encontrado, alumno_id:', alumnoId);

    // Verificar si user_id existe en la tabla usuarios
    let validUserId = null;
    if (user_id) {
      const { data: usuario } = await supabase
        .from('usuarios')
        .select('id')
        .eq('id', user_id)
        .single();
      
      if (usuario) {
        validUserId = user_id;
        console.log('✅ user_id válido encontrado en tabla usuarios');
      } else {
        console.log('⚠️  user_id no existe en tabla usuarios, se guardará como null');
      }
    }

    console.log('💾 Guardando token para alumno_id:', alumnoId);

    const { data, error } = await supabase
      .from('device_tokens')
      .upsert({
        user_id: validUserId,
        estudiante_id: alumnoId,
        token,
        device_info: device_info || '',
        updated_at: new Date().toISOString()
      }, {
        onConflict: 'estudiante_id,token'
      })
      .select()
      .single();

    if (error) {
      console.error('❌ Error de Supabase:', error);
      throw error;
    }

    console.log('✅ Token FCM guardado exitosamente para alumno_id:', alumnoId);
    console.log('📊 Datos guardados:', data);

    res.json({
      success: true,
      message: 'Token guardado exitosamente',
      data
    });
  } catch (error: any) {
    console.error('❌ Error al guardar token:', error.message);
    res.status(500).json({
      success: false,
      message: 'Error al guardar token',
      error: error.message
    });
  }
});

// Actualizar asistencia
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { estado, observaciones } = req.body;

    const { data: asistenciaActualizada, error } = await supabase
      .from('asistencia_asistencia')
      .update({
        estado,
        observaciones
      })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    if (!asistenciaActualizada) {
      return res.status(404).json({
        success: false,
        message: 'Asistencia no encontrada'
      });
    }

    res.json({
      success: true,
      message: 'Asistencia actualizada exitosamente',
      data: asistenciaActualizada
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al actualizar asistencia',
      error: error.message
    });
  }
});

// POST /api/asistencia/eliminar-batch - Eliminar múltiples asistencias de una vez
router.post('/eliminar-batch', async (req, res) => {
  try {
    const { ids } = req.body;

    console.log('🗑️  Batch delete recibido:', { total: ids?.length });

    if (!ids || !Array.isArray(ids) || ids.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere un array de IDs'
      });
    }

    // Eliminar todos los IDs en una sola query
    const { data: eliminadas, error } = await supabase
      .from('asistencias')
      .delete()
      .in('id', ids)
      .select('id');

    if (error) {
      console.error('Error eliminando asistencias batch:', error);
      throw error;
    }

    const totalEliminadas = eliminadas?.length || 0;
    console.log(`✅ ${totalEliminadas} asistencias eliminadas`);

    res.json({
      success: true,
      message: `${totalEliminadas} asistencias eliminadas exitosamente`,
      data: {
        eliminadas: totalEliminadas,
        solicitadas: ids.length
      }
    });
  } catch (error: any) {
    console.error('❌ Error en batch delete:', error);
    res.status(500).json({
      success: false,
      message: 'Error al eliminar asistencias',
      error: error.message
    });
  }
});

// DELETE /api/asistencia/fecha/:fecha - Eliminar TODA la asistencia de una fecha
// router.delete('/fecha/:fecha', async (req, res) => {
//   try {
//     const { fecha } = req.params;
// 
//     console.log('🗑️  Eliminando asistencias por fecha:', fecha);
// 
//     const { data: eliminadas, error } = await supabase
//       .from('asistencias')
//       .delete()
//       .eq('fecha', fecha)
//       .eq('tipo_persona', 'alumno')
//       .select('id');
// 
//     if (error) {
//       console.error('Error eliminando asistencias por fecha:', error);
//       throw error;
//     }
// 
//     const totalEliminadas = eliminadas?.length || 0;
//     console.log(`✅ ${totalEliminadas} asistencias eliminadas para fecha ${fecha}`);
// 
//     if (totalEliminadas === 0) {
//       return res.status(404).json({
//         success: false,
//         message: `No se encontraron asistencias para la fecha ${fecha}`
//       });
//     }
// 
//     res.json({
//       success: true,
//       message: `${totalEliminadas} asistencias eliminadas para la fecha ${fecha}`,
//       data: { eliminadas: totalEliminadas, fecha }
//     });
//   } catch (error: any) {
//     console.error('❌ Error eliminando asistencias por fecha:', error);
//     res.status(500).json({
//       success: false,
//       message: 'Error al eliminar asistencias por fecha',
//       error: error.message
//     });
//   }
// });

// Eliminar asistencia individual
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    console.log('🗑️  Eliminando asistencia ID:', id);

    const { data: asistenciaEliminada, error } = await supabase
      .from('asistencias')
      .delete()
      .eq('id', id)
      .select()
      .single();

    if (error) {
      console.error('Error eliminando asistencia:', error);
      throw error;
    }

    if (!asistenciaEliminada) {
      return res.status(404).json({
        success: false,
        message: 'Asistencia no encontrada'
      });
    }

    console.log('✅ Asistencia eliminada');

    res.json({
      success: true,
      message: 'Asistencia eliminada exitosamente'
    });
  } catch (error: any) {
    console.error('❌ Error eliminando asistencia:', error);
    res.status(500).json({
      success: false,
      message: 'Error al eliminar asistencia',
      error: error.message
    });
  }
});

// Obtener estadísticas de asistencia
router.get('/estadisticas', async (req, res) => {
  try {
    const { fecha_inicio, fecha_fin, seccion_id } = req.query;

    let query = supabase
      .from('asistencia_asistencia')
      .select(`
        estado,
        asistencia_sesionclase!inner (
          fecha,
          seccion_id
        )
      `);

    // Filtros
    if (fecha_inicio) {
      query = query.gte('asistencia_sesionclase.fecha', fecha_inicio);
    }

    if (fecha_fin) {
      query = query.lte('asistencia_sesionclase.fecha', fecha_fin);
    }

    if (seccion_id) {
      query = query.eq('asistencia_sesionclase.seccion_id', seccion_id);
    }

    const { data: asistencias, error } = await query;

    if (error) throw error;

    // Calcular estadísticas
    const total = asistencias?.length || 0;
    const presentes = asistencias?.filter((a: any) => a.estado === 'presente').length || 0;
    const tardanzas = asistencias?.filter((a: any) => a.estado === 'tardanza').length || 0;
    const faltas = asistencias?.filter((a: any) => a.estado === 'falta').length || 0;
    const justificados = asistencias?.filter((a: any) => a.estado === 'justificado').length || 0;

    res.json({
      success: true,
      data: {
        total,
        presentes,
        tardanzas,
        faltas,
        justificados
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener estadísticas',
      error: error.message
    });
  }
});

// Obtener historial de asistencia de un alumno
router.get('/historial/:alumno_id', async (req, res) => {
  try {
    const { alumno_id } = req.params;
    const { fecha_inicio, fecha_fin, limit = 100 } = req.query;

    let query = supabase
      .from('asistencias')
      .select(`
        id,
        fecha,
        hora_entrada,
        estado,
        observaciones
      `)
      .eq('alumno_id', alumno_id);

    // Filtros de fecha
    if (fecha_inicio) {
      query = query.gte('fecha', fecha_inicio);
    }

    if (fecha_fin) {
      query = query.lte('fecha', fecha_fin);
    }

    query = query.order('fecha', { ascending: false })
                 .limit(Number(limit));

    const { data: historial, error } = await query;

    if (error) throw error;

    const historialFormateado = historial?.map((h: any) => ({
      fecha: h.fecha,
      hora_entrada: h.hora_entrada,
      estado: h.estado,
      observaciones: h.observaciones
    })) || [];

    res.json({
      success: true,
      data: historialFormateado,
      total: historialFormateado.length
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener historial de asistencia',
      error: error.message
    });
  }
});

export default router;
