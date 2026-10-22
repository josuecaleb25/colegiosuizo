import { Router } from 'express';
import supabase from '../config/database';
import QRCode from 'qrcode';
import { authMiddleware, requireRoles } from '../middleware/auth';

const router = Router();
const qrImageCache = new Map<string, Promise<string>>();
const qrImageCacheLimit = 1000;

const normalizeLabel = (value: unknown) => String(value || '')
  .toLowerCase()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/[\s-]+/g, '');

const getQrImage = (code: string) => {
  const cached = qrImageCache.get(code);
  if (cached) return cached;

  const imagePromise = QRCode.toDataURL(code, {
    width: 300,
    margin: 2,
    color: {
      dark: '#000000',
      light: '#FFFFFF'
    }
  }).then(image => {
    if (qrImageCache.size >= qrImageCacheLimit) {
      const oldestCode = qrImageCache.keys().next().value;
      if (oldestCode) qrImageCache.delete(oldestCode);
    }
    return image;
  }).catch(error => {
    qrImageCache.delete(code);
    throw error;
  });

  qrImageCache.set(code, imagePromise);
  return imagePromise;
};

router.use(authMiddleware, requireRoles('administrador', 'admin'));

router.get('/alumnos', async (req, res) => {
  try {
    const { seccion, search, limit = 100 } = req.query;

    const limitNum = Math.min(Math.max(Number(limit) || 100, 1), 500);
    const { data: seccionesActivas, error: seccionesError } = await supabase
      .from('secciones')
      .select('id, nombre, grados!inner(nombre), anios_lectivos!inner(activo)')
      .eq('anios_lectivos.activo', true);

    if (seccionesError) throw seccionesError;

    const seccionSolicitada = normalizeLabel(seccion);
    const seccionIds = (seccionesActivas || [])
      .filter((item: any) => {
        if (!seccionSolicitada) return true;
        const grado = Array.isArray(item.grados) ? item.grados[0] : item.grados;
        return normalizeLabel(`${grado?.nombre || ''} ${item.nombre}`) === seccionSolicitada;
      })
      .map((item: any) => item.id);

    if (!seccionIds.length) {
      return res.json({ success: true, data: [], total: 0 });
    }

    const alumnosFiltrados: any[] = [];
    const pageSize = search ? 1000 : limitNum;
    const searchLower = String(search || '').trim().toLowerCase();

    for (let offset = 0; ; offset += pageSize) {
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
            anio_lectivo_id,
            seccion_id,
            secciones!inner (
              id,
              nombre,
              grados!inner (
                nombre
              )
            )
          )
        `)
        .eq('estado', 'activo')
        .eq('matriculas.estado', 'activo')
        .in('matriculas.seccion_id', seccionIds)
        .order('apellidos', { ascending: true, referencedTable: 'personas' })
        .order('nombres', { ascending: true, referencedTable: 'personas' })
        .range(offset, offset + pageSize - 1);

      const { data: alumnos, error } = await query;
      if (error) throw error;
      if (!alumnos?.length) break;

      for (const alumno of alumnos as any[]) {
        if (searchLower) {
          const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
          const matriculas = Array.isArray(alumno.matriculas) ? alumno.matriculas : [alumno.matriculas];
          const matriculaActiva = matriculas[0];
          const secciones = Array.isArray(matriculaActiva?.secciones) ? matriculaActiva.secciones[0] : matriculaActiva?.secciones;
          const grados = Array.isArray(secciones?.grados) ? secciones.grados[0] : secciones?.grados;
          const seccionNombre = secciones && grados ? `${grados.nombre} ${secciones.nombre}` : 'Sin sección';
          const nombreCompleto = `${persona?.nombres || ''} ${persona?.apellidos || ''}`.toLowerCase();
          const coincide = nombreCompleto.includes(searchLower) ||
            String(alumno.codigo_alumno || '').toLowerCase().includes(searchLower) ||
            seccionNombre.toLowerCase().includes(searchLower) ||
            String(persona?.dni || '').toLowerCase().includes(searchLower);
          if (!coincide) continue;
        }

        alumnosFiltrados.push(alumno);
        if (alumnosFiltrados.length >= limitNum) break;
      }

      if (!searchLower || alumnosFiltrados.length >= limitNum || alumnos.length < pageSize) break;
    }

    const alumnosPaginados = alumnosFiltrados.slice(0, limitNum);
    const personaIds = alumnosPaginados
      .map((alumno: any) => Array.isArray(alumno.personas) ? alumno.personas[0]?.id : alumno.personas?.id)
      .filter(Boolean);
    const codigosQrPorPersona = new Map<string, string>();

    if (personaIds.length > 0) {
      const { data: codigosQr, error: qrError } = await supabase
        .from('codigos_qr')
        .select('persona_id, codigo, generado_en')
        .in('persona_id', personaIds)
        .eq('activo', true)
        .order('generado_en', { ascending: false });

      if (qrError) throw qrError;
      for (const codigoQr of codigosQr || []) {
        if (!codigosQrPorPersona.has(codigoQr.persona_id)) {
          codigosQrPorPersona.set(codigoQr.persona_id, codigoQr.codigo);
        }
      }
    }

    const alumnosFormateados = await Promise.all(alumnosPaginados.map(async (alumno: any) => {
      const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
      const matriculas = Array.isArray(alumno.matriculas) ? alumno.matriculas : [alumno.matriculas];
      const matriculaActiva = matriculas[0];
      const secciones = Array.isArray(matriculaActiva?.secciones) ? matriculaActiva.secciones[0] : matriculaActiva?.secciones;
      const grados = Array.isArray(secciones?.grados) ? secciones.grados[0] : secciones?.grados;
      
      const seccionNombre = secciones && grados ? `${grados.nombre} ${secciones.nombre}` : 'Sin sección';

      const qrCodeString = codigosQrPorPersona.get(persona.id) || null;
      
      let qrImage = '';
      if (qrCodeString) {
        try {
          qrImage = await getQrImage(qrCodeString);
        } catch (qrError) {
          console.error('QR generation failed:', qrError);
        }
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
    }));

    res.json({
      success: true,
      data: alumnosFormateados,
      total: alumnosFormateados.length
    });
  } catch (error: any) {
    console.error('Admin student list failed:', error.message);
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumnos',
      error: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

router.get('/alumnos/estadisticas', async (req, res) => {
  try {
    const { count: totalAlumnos } = await supabase
      .from('alumnos')
      .select('*', { count: 'exact', head: true });

    const { data: alumnosPorEstado } = await supabase
      .from('alumnos')
      .select('estado')
      .eq('estado', 'activo');

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

router.get('/asistencia/historial', async (req, res) => {
  try {
    const desde = String(req.query.desde || '');
    const hasta = String(req.query.hasta || '');
    const fechaValida = /^\d{4}-\d{2}-\d{2}$/;

    if (!fechaValida.test(desde) || !fechaValida.test(hasta)) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_DATE_RANGE',
        message: 'Las fechas deben tener el formato YYYY-MM-DD'
      });
    }
    if (desde > hasta) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_DATE_RANGE',
        message: 'La fecha inicial no puede ser posterior a la fecha final'
      });
    }

    let rows: any[] = [];
    let resumen = new Map<string, { fecha: string; presentes: number; tardanzas: number; ausentes: number }>();
    const { data: rpcData, error: rpcError } = await supabase.rpc('resumen_asistencias_por_rango', {
      p_desde: desde,
      p_hasta: hasta
    });

    if (!rpcError) {
      for (const row of rpcData || []) {
        const fecha = String(row.fecha);
        resumen.set(fecha, {
          fecha,
          presentes: Number(row.presentes) || 0,
          tardanzas: Number(row.tardanzas) || 0,
          ausentes: Number(row.ausentes) || 0
        });
      }
    } else if (rpcError.code === 'PGRST202' || rpcError.code === '42883') {
      for (let offset = 0; ; offset += 1000) {
        const { data, error } = await supabase
          .from('asistencias')
          .select('fecha, estado')
          .eq('tipo_persona', 'alumno')
          .gte('fecha', desde)
          .lte('fecha', hasta)
          .range(offset, offset + 999);
        if (error) throw error;
        rows.push(...(data || []));
        if (!data || data.length < 1000) break;
      }
    } else {
      throw rpcError;
    }

    if (rpcError) {
      for (const row of rows) {
        const fecha = String(row.fecha);
        const actual = resumen.get(fecha) || { fecha, presentes: 0, tardanzas: 0, ausentes: 0 };
        const estado = String(row.estado || '').toLowerCase();
        if (estado === 'tardanza') actual.tardanzas++;
        else if (estado === 'falta' || estado === 'ausente') actual.ausentes++;
        else if (estado === 'presente') actual.presentes++;
        resumen.set(fecha, actual);
      }
    }

    return res.json({
      success: true,
      data: [...resumen.values()].sort((a, b) => b.fecha.localeCompare(a.fecha)),
      total: resumen.size
    });
  } catch (error: any) {
    console.error('Attendance history summary failed:', error.message);
    return res.status(500).json({
      success: false,
      code: 'ATTENDANCE_HISTORY_FAILED',
      message: 'No se pudo cargar el historial de asistencias',
      error: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

router.get('/asistencia/fecha', async (req, res) => {
  try {
    const { fecha } = req.query;

    if (!fecha) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere el parámetro fecha'
      });
    }

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
      console.error('Attendance query failed:', error);
      throw error;
    }

    if (!asistencias || asistencias.length === 0) {
      return res.json({
        success: true,
        data: [],
        total: 0,
        message: 'No hay asistencias para esta fecha'
      });
    }

    const personaIds = [...new Set(asistencias.map((asistencia: any) => asistencia.persona_id).filter(Boolean))];
    const alumnos: any[] = [];
    for (let index = 0; index < personaIds.length; index += 100) {
      const { data, error: alumnosError } = await supabase
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
        `)
        .in('persona_id', personaIds.slice(index, index + 100));

      if (alumnosError) {
        console.error('Student query failed:', alumnosError);
        throw alumnosError;
      }
      alumnos.push(...(data || []));
    }

    const alumnosPorPersonaId = new Map((alumnos || []).map(a => [a.persona_id, a]));

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

    res.json({
      success: true,
      data: asistenciasFormateadas,
      total: asistenciasFormateadas.length
    });
  } catch (error: any) {
    console.error('Admin attendance endpoint failed:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener asistencias',
      error: error.message
    });
  }
});

export default router;
