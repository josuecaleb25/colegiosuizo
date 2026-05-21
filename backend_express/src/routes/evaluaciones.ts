import { Router, Request, Response } from 'express';
import supabase from '../config/database';
import { authMiddleware as authenticateToken } from '../middleware/auth';

const router = Router();

// ============================================
// ENDPOINTS DE EVALUACIONES
// ============================================

/**
 * GET /api/evaluaciones/curso/:cursoId
 * Obtener todas las evaluaciones de un curso
 */
router.get('/curso/:cursoId', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { cursoId } = req.params;

    const { data, error } = await supabase
      .from('evaluaciones')
      .select('*')
      .eq('asignacion_id', cursoId)
      .order('orden', { ascending: true });

    if (error) throw error;

    res.json({
      success: true,
      data: data || []
    });
  } catch (error: any) {
    console.error('Error obteniendo evaluaciones:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener evaluaciones',
      error: error.message
    });
  }
});

/**
 * POST /api/evaluaciones
 * Crear una nueva evaluación para un curso
 * Al crear, se generan automáticamente registros en calificaciones para todos los alumnos
 */
router.post('/', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { asignacion_id, nombre, peso, orden, activo } = req.body;

    // Validaciones
    if (!asignacion_id || !nombre) {
      return res.status(400).json({
        success: false,
        message: 'asignacion_id y nombre son requeridos'
      });
    }

    // 1. Crear la evaluación
    const { data: evaluacion, error: errorEvaluacion } = await supabase
      .from('evaluaciones')
      .insert({
        asignacion_id,
        nombre,
        peso: peso || 1,
        orden: orden || 0,
        activo: activo !== undefined ? activo : true
      })
      .select()
      .single();

    if (errorEvaluacion) {
      console.error('❌ Error creando evaluación:', errorEvaluacion);
      throw errorEvaluacion;
    }

    // 2. Obtener el curso_id y seccion_id de la asignación
    const { data: asignacionDetalle, error: errorAsignacionDetalle } = await supabase
      .from('asignaciones')
      .select('curso_id, seccion_id')
      .eq('id', asignacion_id)
      .single();

    if (errorAsignacionDetalle || !asignacionDetalle) {
      console.error('❌ Error obteniendo asignación:', errorAsignacionDetalle);
      throw new Error('No se encontró la asignación');
    }

    // 3. Obtener todos los alumnos del curso y sección a través de matrículas
    const { data: matriculas, error: errorAlumnos } = await supabase
      .from('matriculas')
      .select('alumno_id')
      .eq('seccion_id', asignacionDetalle.seccion_id)
      .eq('estado', 'activo');

    if (errorAlumnos) {
      console.error('❌ Error obteniendo alumnos:', errorAlumnos);
      throw errorAlumnos;
    }

    const alumnos = matriculas || [];

    // 3. Crear registros de calificaciones para cada alumno
    if (alumnos && alumnos.length > 0) {
      const calificaciones = alumnos.map(alumno => ({
        evaluacion_id: evaluacion.id,
        alumno_id: alumno.alumno_id,
        nota: null // Sin calificación inicial, usar 'nota' en lugar de 'calificacion'
      }));

      const { error: errorCalificaciones } = await supabase
        .from('calificaciones')
        .insert(calificaciones);

      if (errorCalificaciones) {
        console.error('❌ Error creando calificaciones:', errorCalificaciones);
      }
    }

    res.status(201).json({
      success: true,
      message: 'Evaluación creada exitosamente',
      data: evaluacion
    });
  } catch (error: any) {
    console.error('Error creando evaluación:', error);
    res.status(500).json({
      success: false,
      message: 'Error al crear evaluación',
      error: error.message
    });
  }
});

/**
 * PUT /api/evaluaciones/:id
 * Actualizar una evaluación
 */
router.put('/:id', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { nombre, peso, orden, activo } = req.body;

    const updateData: any = {};
    if (nombre !== undefined) updateData.nombre = nombre;
    if (peso !== undefined) updateData.peso = peso;
    if (orden !== undefined) updateData.orden = orden;
    if (activo !== undefined) updateData.activo = activo;

    const { data, error } = await supabase
      .from('evaluaciones')
      .update(updateData)
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Evaluación actualizada exitosamente',
      data
    });
  } catch (error: any) {
    console.error('Error actualizando evaluación:', error);
    res.status(500).json({
      success: false,
      message: 'Error al actualizar evaluación',
      error: error.message
    });
  }
});

/**
 * DELETE /api/evaluaciones/:id
 * Eliminar una evaluación (también elimina sus calificaciones en cascada)
 */
router.delete('/:id', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Primero eliminar las calificaciones asociadas
    const { error: errorCalificaciones } = await supabase
      .from('calificaciones')
      .delete()
      .eq('evaluacion_id', id);

    if (errorCalificaciones) {
      console.error('❌ Error eliminando calificaciones:', errorCalificaciones);
      throw errorCalificaciones;
    }

    // Luego eliminar la evaluación
    const { error } = await supabase
      .from('evaluaciones')
      .delete()
      .eq('id', id);

    if (error) {
      console.error('❌ Error eliminando evaluación:', error);
      throw error;
    }

    res.json({
      success: true,
      message: 'Evaluación eliminada exitosamente'
    });
  } catch (error: any) {
    console.error('Error eliminando evaluación:', error);
    res.status(500).json({
      success: false,
      message: 'Error al eliminar evaluación',
      error: error.message
    });
  }
});

// ============================================
// ENDPOINTS DE CALIFICACIONES
// ============================================

/**
 * GET /api/evaluaciones/calificaciones/alumno/:alumnoId/curso/:cursoId
 * Obtener todas las calificaciones de un alumno en un curso específico
 * Si no existen calificaciones para alguna evaluación, las crea automáticamente
 * NOTA: alumnoId puede ser persona_id, se convierte automáticamente al id de alumnos
 */
router.get('/calificaciones/alumno/:alumnoId/curso/:cursoId', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { alumnoId, cursoId } = req.params;

    // Intentar convertir persona_id a alumno.id, o usar directamente si ya es alumno.id
    let alumnoIdReal = alumnoId;
    
    // Primero intentar como persona_id
    const { data: alumnoPorPersona, error: errorPersona } = await supabase
      .from('alumnos')
      .select('id, persona_id')
      .eq('persona_id', alumnoId)
      .eq('estado', 'activo')
      .single();

    if (alumnoPorPersona) {
      alumnoIdReal = alumnoPorPersona.id;
    } else {
      // Si no se encuentra por persona_id, intentar como alumno.id directo
      const { data: alumnoPorId, error: errorId } = await supabase
        .from('alumnos')
        .select('id, persona_id')
        .eq('id', alumnoId)
        .eq('estado', 'activo')
        .single();

      if (alumnoPorId) {
        alumnoIdReal = alumnoPorId.id;
      } else {
        console.error('❌ No se encontró alumno con persona_id ni alumno.id:', alumnoId);
        return res.status(404).json({
          success: false,
          message: 'Alumno no encontrado'
        });
      }
    }

    // Primero obtener las evaluaciones del curso (cursoId es realmente asignacion_id)
    const { data: evaluaciones, error: errorEval } = await supabase
      .from('evaluaciones')
      .select('*')
      .eq('asignacion_id', cursoId)
      .eq('activo', true)
      .order('orden', { ascending: true });

    if (errorEval) throw errorEval;

    if (!evaluaciones || evaluaciones.length === 0) {
      return res.json({
        success: true,
        data: []
      });
    }

    const evaluacionIds = evaluaciones.map(e => e.id);

    // Obtener las calificaciones existentes del alumno usando alumno.id
    const { data: calificacionesExistentes, error: errorCalif } = await supabase
      .from('calificaciones')
      .select('*')
      .eq('alumno_id', alumnoIdReal) // Usar alumno.id, no persona_id
      .in('evaluacion_id', evaluacionIds);

    if (errorCalif) throw errorCalif;

    // Crear un mapa de calificaciones existentes por evaluacion_id
    const calificacionesMap = new Map();
    calificacionesExistentes?.forEach(calif => {
      calificacionesMap.set(calif.evaluacion_id, calif);
    });

    // Identificar evaluaciones sin calificación
    const evaluacionesSinCalificacion = evaluaciones.filter(
      ev => !calificacionesMap.has(ev.id)
    );

    // Crear calificaciones faltantes
    if (evaluacionesSinCalificacion.length > 0) {
      const nuevasCalificaciones = evaluacionesSinCalificacion.map(ev => ({
        evaluacion_id: ev.id,
        alumno_id: alumnoIdReal, // Usar alumno.id, no persona_id
        nota: null
      }));

      const { data: califCreadas, error: errorCrear } = await supabase
        .from('calificaciones')
        .insert(nuevasCalificaciones)
        .select();

      if (errorCrear) {
        console.error('❌ Error creando calificaciones:', errorCrear);
      } else {
        // Agregar las nuevas calificaciones al mapa
        califCreadas?.forEach(calif => {
          calificacionesMap.set(calif.evaluacion_id, calif);
        });
      }
    }

    // Construir respuesta con todas las evaluaciones y sus calificaciones
    const resultado = evaluaciones.map(evaluacion => {
      const calificacion = calificacionesMap.get(evaluacion.id);
      
      return {
        id: calificacion?.id || null,
        evaluacion_id: evaluacion.id,
        alumno_id: alumnoId, // Devolver persona_id para compatibilidad con la app
        calificacion: calificacion?.nota || null, // Transformar 'nota' a 'calificacion'
        evaluaciones: {
          id: evaluacion.id,
          nombre: evaluacion.nombre,
          peso: evaluacion.peso,
          orden: evaluacion.orden,
          asignacion_id: evaluacion.asignacion_id,
          activo: evaluacion.activo
        }
      };
    });

    res.json({
      success: true,
      data: resultado
    });
  } catch (error: any) {
    console.error('Error obteniendo calificaciones:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener calificaciones',
      error: error.message
    });
  }
});

/**
 * GET /api/evaluaciones/calificaciones/curso/:cursoId
 * Obtener todas las calificaciones de todos los alumnos de un curso
 */
router.get('/calificaciones/curso/:cursoId', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { cursoId } = req.params;

    // Obtener evaluaciones del curso
    const { data: evaluaciones, error: errorEval } = await supabase
      .from('evaluaciones')
      .select('id')
      .eq('asignacion_id', cursoId);

    if (errorEval) throw errorEval;

    if (!evaluaciones || evaluaciones.length === 0) {
      return res.json({
        success: true,
        data: []
      });
    }

    const evaluacionIds = evaluaciones.map(e => e.id);

    // Obtener calificaciones
    const { data, error } = await supabase
      .from('calificaciones')
      .select(`
        id,
        nota,
        alumno_id,
        evaluacion_id,
        evaluaciones (
          id,
          nombre,
          peso,
          orden,
          activo
        ),
        personas!calificaciones_alumno_id_fkey (
          id,
          nombre_completo
        )
      `)
      .in('evaluacion_id', evaluacionIds)
      .order('evaluaciones(orden)', { ascending: true });

    if (error) throw error;

    // Transformar 'nota' a 'calificacion' para compatibilidad
    const dataTransformada = (data || []).map(item => ({
      ...item,
      calificacion: item.nota,
      nota: undefined
    }));

    res.json({
      success: true,
      data: dataTransformada
    });
  } catch (error: any) {
    console.error('Error obteniendo calificaciones del curso:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener calificaciones del curso',
      error: error.message
    });
  }
});

/**
 * PUT /api/evaluaciones/calificaciones/:id
 * Actualizar una calificación específica
 */
router.put('/calificaciones/:id', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { calificacion } = req.body;

    // Validar que la calificación esté entre 0 y 20 (permitir null/undefined para borrar)
    if (calificacion !== null && calificacion !== undefined && (calificacion < 0 || calificacion > 20)) {
      return res.status(400).json({
        success: false,
        message: 'La calificación debe estar entre 0 y 20'
      });
    }

    // Verificar que la calificación existe antes de actualizar
    const { data: existente, error: errorCheck } = await supabase
      .from('calificaciones')
      .select('id')
      .eq('id', id)
      .single();

    if (errorCheck || !existente) {
      console.error('❌ Calificación no encontrada:', id);
      return res.status(404).json({
        success: false,
        message: 'Calificación no encontrada'
      });
    }

    // Actualizar usando 'nota' en lugar de 'calificacion'
    const { data, error } = await supabase
      .from('calificaciones')
      .update({ nota: calificacion })
      .eq('id', id)
      .select()
      .single();

    if (error) {
      console.error('❌ Error de Supabase:', error);
      throw error;
    }

    // Transformar 'nota' a 'calificacion' en la respuesta
    const dataTransformada = {
      ...data,
      calificacion: data.nota,
      nota: undefined
    };

    res.json({
      success: true,
      message: 'Calificación actualizada exitosamente',
      data: dataTransformada
    });
  } catch (error: any) {
    console.error('Error actualizando calificación:', error);
    res.status(500).json({
      success: false,
      message: 'Error al actualizar calificación',
      error: error.message
    });
  }
});

/**
 * POST /api/evaluaciones/inicializar-curso/:cursoId
 * Inicializar evaluaciones por defecto para una asignación
 * NOTA: cursoId es realmente asignacion_id
 */
router.post('/inicializar-curso/:cursoId', authenticateToken, async (req: Request, res: Response) => {
  try {
    const { cursoId } = req.params;

    // Evaluaciones por defecto
    const evaluacionesDefault = [
      { nombre: 'Actitudes', peso: 1, orden: 1, activo: true },
      { nombre: 'Participacion', peso: 1, orden: 2, activo: true },
      { nombre: 'Proyecto', peso: 1, orden: 3, activo: true },
      { nombre: 'Examen I', peso: 1, orden: 4, activo: true },
      { nombre: 'Examen II', peso: 1, orden: 5, activo: true },
      { nombre: 'Examen final', peso: 1, orden: 6, activo: true }
    ];

    // Verificar si ya existen evaluaciones
    const { data: existentes, error: errorCheck } = await supabase
      .from('evaluaciones')
      .select('id')
      .eq('asignacion_id', cursoId)
      .limit(1);

    if (errorCheck) throw errorCheck;

    if (existentes && existentes.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'El curso ya tiene evaluaciones creadas'
      });
    }

    // Crear evaluaciones
    const evaluacionesConCurso = evaluacionesDefault.map(e => ({
      asignacion_id: cursoId,
      nombre: e.nombre,
      peso: e.peso,
      orden: e.orden,
      activo: e.activo
    }));

    const { data: evaluacionesCreadas, error: errorEval } = await supabase
      .from('evaluaciones')
      .insert(evaluacionesConCurso)
      .select();

    if (errorEval) throw errorEval;

    // Obtener el curso_id y seccion_id de la asignación
    const { data: asignacionDetalle, error: errorAsignacionDetalle } = await supabase
      .from('asignaciones')
      .select('curso_id, seccion_id')
      .eq('id', cursoId)
      .single();

    if (errorAsignacionDetalle || !asignacionDetalle) {
      throw new Error('No se encontró la asignación');
    }

    // Obtener alumnos del curso y sección a través de matrículas
    const { data: matriculas, error: errorAlumnos } = await supabase
      .from('matriculas')
      .select('alumno_id')
      .eq('seccion_id', asignacionDetalle.seccion_id)
      .eq('estado', 'activo');

    if (errorAlumnos) throw errorAlumnos;

    const alumnos = matriculas || [];

    // Crear calificaciones para cada alumno y cada evaluación
    if (alumnos && alumnos.length > 0 && evaluacionesCreadas) {
      const calificaciones: any[] = [];
      
      evaluacionesCreadas.forEach(evaluacion => {
        alumnos.forEach(alumno => {
          calificaciones.push({
            evaluacion_id: evaluacion.id,
            alumno_id: alumno.alumno_id,
            nota: null // Usar 'nota' en lugar de 'calificacion'
          });
        });
      });

      const { error: errorCalif } = await supabase
        .from('calificaciones')
        .insert(calificaciones);

      if (errorCalif) throw errorCalif;
    }

    res.status(201).json({
      success: true,
      message: 'Evaluaciones inicializadas exitosamente',
      data: evaluacionesCreadas
    });
  } catch (error: any) {
    console.error('Error inicializando evaluaciones:', error);
    res.status(500).json({
      success: false,
      message: 'Error al inicializar evaluaciones',
      error: error.message
    });
  }
});

export default router;
