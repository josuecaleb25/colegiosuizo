import { Router } from 'express';
import supabase from '../config/database';
import QRCode from 'qrcode';

const router = Router();

// Obtener todos los alumnos con filtros (para admin)
router.get('/alumnos', async (req, res) => {
  try {
    const { seccion, search, limit = 100 } = req.query;

    let query = supabase
      .from('alumnos')
      .select(`
        id,
        codigo_alumno,
        estado,
        persona_id,
        personas!inner (
          id,
          nombres,
          apellidos,
          dni,
          correo
        ),
        matriculas!inner (
          id,
          secciones!inner (
            id,
            nombre,
            grados!inner (
              nombre
            )
          )
        )
      `)
      .order('personas(apellidos)', { ascending: true });

    const { data: alumnos, error } = await query;

    if (error) throw error;

    // Formatear datos y generar QR codes
    const alumnosFormateados = await Promise.all(alumnos?.map(async (alumno: any) => {
      const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
      const matriculas = Array.isArray(alumno.matriculas) ? alumno.matriculas : [alumno.matriculas];
      const matriculaActiva = matriculas[0];
      const secciones = Array.isArray(matriculaActiva?.secciones) ? matriculaActiva.secciones[0] : matriculaActiva?.secciones;
      const grados = Array.isArray(secciones?.grados) ? secciones.grados[0] : secciones?.grados;
      
      const seccionNombre = secciones && grados ? `${grados.nombre} ${secciones.nombre}` : 'Sin sección';

      // Obtener código QR de la tabla codigos_qr usando persona.id
      const { data: codigoQR, error: qrError } = await supabase
        .from('codigos_qr')
        .select('codigo')
        .eq('persona_id', persona.id)
        .eq('activo', true)
        .single();

      const qrCodeString = codigoQR?.codigo || alumno.codigo_alumno;
      
      // Generar imagen QR en base64
      let qrImage = '';
      try {
        qrImage = await QRCode.toDataURL(qrCodeString, {
          width: 300,
          margin: 2,
          color: {
            dark: '#000000',
            light: '#FFFFFF'
          }
        });
      } catch (qrError) {
        console.error('Error generando QR:', qrError);
      }

      return {
        id: alumno.id,
        codigo_alumno: alumno.codigo_alumno,
        nombres: persona.nombres,
        apellidos: persona.apellidos,
        nombre_completo: `${persona.nombres} ${persona.apellidos}`,
        dni: persona.dni,
        email: persona.correo,
        seccion: seccionNombre,
        estado: alumno.estado,
        qr_code: qrCodeString,
        qr_image: qrImage
      };
    }) || []);

    // Aplicar filtros en memoria
    let alumnosFiltrados = alumnosFormateados;

    if (seccion && seccion !== 'Todos') {
      alumnosFiltrados = alumnosFiltrados.filter((a: any) => 
        a.seccion.toLowerCase().includes(seccion.toString().toLowerCase())
      );
    }

    if (search) {
      const searchLower = search.toString().toLowerCase();
      alumnosFiltrados = alumnosFiltrados.filter((a: any) =>
        a.nombre_completo.toLowerCase().includes(searchLower) ||
        a.codigo_alumno.toLowerCase().includes(searchLower) ||
        a.seccion.toLowerCase().includes(searchLower) ||
        (a.dni && a.dni.includes(searchLower))
      );
    }

    // Limitar resultados
    const limitNum = Number(limit);
    if (limitNum > 0) {
      alumnosFiltrados = alumnosFiltrados.slice(0, limitNum);
    }

    res.json({
      success: true,
      data: alumnosFiltrados,
      total: alumnosFiltrados.length
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumnos',
      error: error.message
    });
  }
});

// Obtener estadísticas de alumnos
router.get('/alumnos/estadisticas', async (req, res) => {
  try {
    // Total de alumnos
    const { count: totalAlumnos } = await supabase
      .from('alumnos')
      .select('*', { count: 'exact', head: true });

    // Alumnos por estado
    const { data: alumnosPorEstado } = await supabase
      .from('alumnos')
      .select('estado')
      .eq('estado', 'activo');

    // Alumnos por sección
    const { data: alumnosPorSeccion } = await supabase
      .from('matriculas')
      .select(`
        secciones!inner (
          nombre,
          grados!inner (
            nombre
          )
        )
      `);

    res.json({
      success: true,
      data: {
        total: totalAlumnos || 0,
        activos: alumnosPorEstado?.length || 0,
        por_seccion: alumnosPorSeccion || []
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

// GET /api/admin/asistencia/fecha - Obtener asistencias por fecha
router.get('/asistencia/fecha', async (req, res) => {
  try {
    const { fecha } = req.query;

    if (!fecha) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere el parámetro fecha'
      });
    }

    console.log('📅 Consultando asistencias para fecha:', fecha);

    // Obtener asistencias de la fecha especificada
    const { data: asistencias, error } = await supabase
      .from('asistencias')
      .select(`
        id,
        persona_id,
        fecha,
        hora_entrada,
        estado,
        tipo_persona
      `)
      .eq('fecha', fecha)
      .eq('tipo_persona', 'alumno');

    if (error) {
      console.error('Error consultando asistencias:', error);
      throw error;
    }

    console.log(`✅ Encontradas ${asistencias?.length || 0} asistencias`);

    if (!asistencias || asistencias.length === 0) {
      return res.json({
        success: true,
        data: [],
        total: 0,
        message: 'No hay asistencias para esta fecha'
      });
    }

    // Obtener información de los alumnos (sin filtro IN para evitar headers overflow)
    const { data: alumnos, error: alumnosError } = await supabase
      .from('alumnos')
      .select(`
        id,
        persona_id,
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
      `);

    if (alumnosError) {
      console.error('Error consultando alumnos:', alumnosError);
      throw alumnosError;
    }

    // Indexar alumnos por persona_id para búsqueda rápida
    const alumnosPorPersonaId = new Map((alumnos || []).map(a => [a.persona_id, a]));

    // Mapear asistencias con información de alumnos
    const asistenciasFormateadas = asistencias.map(asist => {
      const alumno = alumnosPorPersonaId.get(asist.persona_id);
      
      if (!alumno) {
        return null;
      }

      const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
      const matricula = Array.isArray(alumno.matriculas) ? alumno.matriculas[0] : alumno.matriculas;
      const seccion = Array.isArray(matricula?.secciones) ? matricula.secciones[0] : matricula?.secciones;
      const grado = Array.isArray(seccion?.grados) ? seccion.grados[0] : seccion?.grados;

      const horaCruda = asist.hora_entrada || '';
      const horaFormateada = horaCruda.includes('AM') || horaCruda.includes('PM')
        ? horaCruda
        : (() => {
            const partes = horaCruda.split(':');
            if (partes.length < 2) return horaCruda;
            let h = parseInt(partes[0], 10);
            const m = partes[1];
            const ampm = h >= 12 ? 'PM' : 'AM';
            if (h > 12) h -= 12;
            if (h === 0) h = 12;
            return `${h.toString().padStart(2, '0')}:${m} ${ampm}`;
          })();

      return {
        id: asist.id,
        persona_id: asist.persona_id,
        nombre_completo: `${persona.nombres} ${persona.apellidos}`,
        salon: `${grado?.nombre || ''} ${seccion?.nombre || ''}`.trim(),
        hora_registro: horaFormateada,
        estado_entrada: asist.estado,
        fecha: asist.fecha
      };
    }).filter(a => a !== null);

    console.log(`📊 Formateadas ${asistenciasFormateadas.length} asistencias`);

    res.json({
      success: true,
      data: asistenciasFormateadas,
      total: asistenciasFormateadas.length
    });
  } catch (error: any) {
    console.error('❌ Error en /admin/asistencia/fecha:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener asistencias',
      error: error.message
    });
  }
});

export default router;
