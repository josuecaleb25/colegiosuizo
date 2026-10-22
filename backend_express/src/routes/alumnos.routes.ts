import { Router } from 'express';
import supabase from '../config/database';
import { authMiddleware, requireRoles } from '../middleware/auth';

const router = Router();
const normalizeLabel = (value: unknown) => String(value || '')
  .toLowerCase()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/[\s-]+/g, '');

router.use(authMiddleware, requireRoles('administrador', 'admin'));

router.get('/', async (req, res) => {
  try {
    const { seccion, search, limit = 200 } = req.query;
    const limitNum = Math.min(Math.max(Number(limit) || 200, 1), 500);
    const normalizedSearch = String(search || '').trim().toLowerCase();
    const normalizedSection = normalizeLabel(seccion);
    const alumnosFormateados: any[] = [];
    const pageSize = normalizedSearch || normalizedSection ? 500 : limitNum;
    let offset = 0;

    while (alumnosFormateados.length < limitNum) {
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
            fecha_nacimiento,
            correo,
            codigos_qr (codigo, activo, generado_en)
          ),
          matriculas!inner (
            estado,
            seccion_id,
            secciones!inner (
              id,
              nombre,
              grados!inner (id, nombre)
            )
          )
        `)
        .eq('estado', 'activo')
        .eq('matriculas.estado', 'activo')
        .order('codigo_alumno', { ascending: true })
        .range(offset, offset + pageSize - 1);

      const { data: alumnos, error } = await query;
      if (error) throw error;
      if (!alumnos?.length) break;

      for (const alumno of alumnos as any[]) {
        const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
        const matriculas = Array.isArray(alumno.matriculas) ? alumno.matriculas : [alumno.matriculas];
        const matricula = matriculas.find((item: any) => item?.estado === 'activo') || matriculas[0];
        const seccionData = Array.isArray(matricula?.secciones) ? matricula.secciones[0] : matricula?.secciones;
        const grado = Array.isArray(seccionData?.grados) ? seccionData.grados[0] : seccionData?.grados;
        if (!persona || !seccionData || !grado) continue;

        const nombreCompleto = `${persona.nombres} ${persona.apellidos}`.trim();
        const nombreSeccion = `${grado.nombre} ${seccionData.nombre}`.trim();
        const codigoQr = (persona.codigos_qr || [])
          .filter((item: any) => item.activo)
          .sort((a: any, b: any) => String(b.generado_en || '').localeCompare(String(a.generado_en || '')))[0]?.codigo || null;

        if (normalizedSection && !normalizeLabel(nombreSeccion).includes(normalizedSection)) continue;

        if (normalizedSearch && ![
          nombreCompleto,
          alumno.codigo_alumno,
          nombreSeccion,
          persona.dni
        ].some(value => String(value || '').toLowerCase().includes(normalizedSearch))) continue;

        alumnosFormateados.push({
          id: alumno.id,
          codigo: alumno.codigo_alumno,
          dni: persona.dni,
          nombres: persona.nombres,
          apellidos: persona.apellidos,
          nombre_completo: nombreCompleto,
          fecha_nacimiento: persona.fecha_nacimiento,
          email_padre: null,
          qr_token: codigoQr,
          activo: alumno.estado === 'activo',
          seccion: nombreSeccion,
          seccion_id: seccionData.id,
          grado_id: grado.id,
          grado_nombre: grado.nombre,
          seccion_nombre: seccionData.nombre
        });
        if (alumnosFormateados.length >= limitNum) break;
      }

      if (alumnos.length < pageSize) break;
      offset += pageSize;
    }

    res.json({
      success: true,
      data: alumnosFormateados,
      total: alumnosFormateados.length
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumnos',
      error: error.message
    });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { data: alumno, error } = await supabase
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
          fecha_nacimiento,
          correo,
          codigos_qr (codigo, activo, generado_en)
        ),
        matriculas!inner (
          estado,
          seccion_id,
          secciones!inner (
            id,
            nombre,
            grados!inner (id, nombre)
          )
        )
      `)
      .eq('id', id)
      .eq('estado', 'activo')
      .eq('matriculas.estado', 'activo')
      .maybeSingle();

    if (error) throw error;

    if (!alumno) {
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    const persona = Array.isArray((alumno as any).personas) ? (alumno as any).personas[0] : (alumno as any).personas;
    const matriculas = Array.isArray((alumno as any).matriculas) ? (alumno as any).matriculas : [(alumno as any).matriculas];
    const matricula = matriculas.find((item: any) => item?.estado === 'activo') || matriculas[0];
    const seccion = Array.isArray(matricula?.secciones) ? matricula.secciones[0] : matricula?.secciones;
    const grado = Array.isArray(seccion?.grados) ? seccion.grados[0] : seccion?.grados;
    const codigosQr = (persona?.codigos_qr || [])
      .filter((item: any) => item.activo)
      .sort((a: any, b: any) => String(b.generado_en || '').localeCompare(String(a.generado_en || '')));
    const alumnoFormateado = {
      id: (alumno as any).id,
      codigo: (alumno as any).codigo_alumno,
      dni: persona?.dni,
      nombres: persona?.nombres,
      apellidos: persona?.apellidos,
      nombre_completo: `${persona?.nombres || ''} ${persona?.apellidos || ''}`.trim(),
      fecha_nacimiento: persona?.fecha_nacimiento,
      email_padre: null,
      qr_token: codigosQr[0]?.codigo || null,
      activo: (alumno as any).estado === 'activo',
      seccion: seccion && grado ? `${grado.nombre} ${seccion.nombre}` : null,
      seccion_id: seccion?.id,
      grado_id: grado?.id,
      grado_nombre: grado?.nombre,
      seccion_nombre: seccion?.nombre
    };

    res.json({
      success: true,
      data: alumnoFormateado
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumno',
      error: error.message
    });
  }
});

export default router;
