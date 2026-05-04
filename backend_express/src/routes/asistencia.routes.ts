import { Router } from 'express';
import supabase from '../config/database';

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
      const hoy = new Date().toISOString().split('T')[0];
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

    const { data: nuevaAsistencia, error } = await supabase
      .from('asistencia_asistencia')
      .insert({
        sesion_id,
        alumno_id,
        estado,
        hora_registro: new Date().toISOString(),
        registrado_via_qr: false,
        observaciones: observaciones || ''
      })
      .select()
      .single();

    if (error) throw error;

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

// Eliminar asistencia
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { data: asistenciaEliminada, error } = await supabase
      .from('asistencia_asistencia')
      .delete()
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    if (!asistenciaEliminada) {
      return res.status(404).json({
        success: false,
        message: 'Asistencia no encontrada'
      });
    }

    res.json({
      success: true,
      message: 'Asistencia eliminada exitosamente'
    });
  } catch (error: any) {
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

export default router;
