import { Router } from 'express';
import supabase from '../config/database';

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
router.get('/asistencia/alumnos', async (req, res) => {
  try {
    const { salon, search } = req.query;
    
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

    // Obtener asistencias de hoy
    const hoy = new Date().toISOString().split('T')[0];
    const { data: asistenciasHoy } = await supabase
      .from('asistencias')
      .select('persona_id, hora_entrada, estado')
      .eq('fecha', hoy)
      .eq('tipo_persona', 'alumno');

    const asistenciasMap: any = {};
    asistenciasHoy?.forEach((a: any) => {
      asistenciasMap[a.persona_id] = {
        hora_entrada: a.hora_entrada,
        estado: a.estado
      };
    });

    const alumnosFormateados = alumnos?.map((a: any) => {
      const matricula = a.matriculas?.[0];
      const seccionNombre = matricula?.secciones?.nombre || '';
      const gradoNombre = matricula?.secciones?.grados?.nombre || '';
      const asistencia = asistenciasMap[a.personas.id];
      
      return {
        id: a.id,
        persona_id: a.personas.id,
        nombre_completo: `${a.personas.nombres} ${a.personas.apellidos}`,
        salon: `${gradoNombre} ${seccionNombre}`,
        hora_registro: asistencia?.hora_entrada || null,
        estado_entrada: asistencia?.estado || 'ausente'
      };
    }) || [];

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

    // Verificar si ya tiene asistencia hoy
    const { data: asistenciaExistente } = await supabase
      .from('asistencias')
      .select('hora_entrada')
      .eq('persona_id', personaData?.id)
      .eq('fecha', hoy)
      .limit(1);

    if (asistenciaExistente && asistenciaExistente.length > 0) {
      return res.status(400).json({
        success: false,
        message: `Asistencia ya registrada a las ${asistenciaExistente[0].hora_entrada}`
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

    if (insertError) throw insertError;

    res.json({
      success: true,
      message: 'Asistencia registrada exitosamente',
      data: {
        alumno: `${personaData?.nombres} ${personaData?.apellidos}`,
        estado,
        hora: ahora.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al registrar asistencia',
      error: error.message
    });
  }
});

export default router;