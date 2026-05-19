import { Router } from 'express';
import supabase from '../config/database';
import notificationService from '../services/notification.service';

const router = Router();

// GET /api/calificaciones/alumno/:alumnoId/curso/:cursoId - Notas de un alumno en un curso
router.get('/alumno/:alumnoId/curso/:cursoId', async (req, res) => {
  try {
    const { alumnoId, cursoId } = req.params;

    // Obtener asignación del curso
    const { data: matricula } = await supabase
      .from('matriculas')
      .select('seccion_id')
      .eq('alumno_id', alumnoId)
      .eq('estado', 'activo')
      .single();

    if (!matricula) {
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    const { data: asignacion } = await supabase
      .from('asignaciones')
      .select('id')
      .eq('curso_id', cursoId)
      .eq('seccion_id', matricula.seccion_id)
      .single();

    if (!asignacion) {
      return res.json({
        success: true,
        data: {
          evaluaciones: [],
          promedio: 0
        }
      });
    }

    // Obtener evaluaciones y calificaciones
    const { data: evaluaciones, error } = await supabase
      .from('evaluaciones')
      .select(`
        id,
        nombre,
        peso,
        orden,
        calificaciones!left (
          nota,
          observaciones
        )
      `)
      .eq('asignacion_id', asignacion.id)
      .eq('activo', true)
      .eq('calificaciones.alumno_id', alumnoId)
      .order('orden');

    if (error) throw error;

    const evaluacionesFormateadas = evaluaciones?.map((ev: any) => ({
      id: ev.id,
      nombre: ev.nombre,
      peso: ev.peso,
      nota: ev.calificaciones?.[0]?.nota || null,
      observaciones: ev.calificaciones?.[0]?.observaciones || null
    })) || [];

    // Calcular promedio ponderado
    let sumaNotas = 0;
    let sumaPesos = 0;
    evaluacionesFormateadas.forEach((ev: any) => {
      if (ev.nota !== null) {
        sumaNotas += ev.nota * ev.peso;
        sumaPesos += ev.peso;
      }
    });

    const promedio = sumaPesos > 0 ? (sumaNotas / sumaPesos).toFixed(2) : '0.00';

    res.json({
      success: true,
      data: {
        evaluaciones: evaluacionesFormateadas,
        promedio: promedio
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener calificaciones',
      error: error.message
    });
  }
});

// GET /api/calificaciones/curso/:asignacionId - Todas las calificaciones de un curso
router.get('/curso/:asignacionId', async (req, res) => {
  try {
    const { asignacionId } = req.params;

    const { data, error } = await supabase
      .from('evaluaciones')
      .select(`
        id,
        nombre,
        peso,
        orden,
        calificaciones (
          id,
          nota,
          observaciones,
          alumnos!inner (
            id,
            codigo,
            personas!inner (
              nombres,
              apellidos
            )
          )
        )
      `)
      .eq('asignacion_id', asignacionId)
      .eq('activo', true)
      .order('orden');

    if (error) throw error;

    res.json({
      success: true,
      data: data || []
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener calificaciones del curso',
      error: error.message
    });
  }
});

// POST /api/calificaciones - Registrar/actualizar calificación
router.post('/', async (req, res) => {
  try {
    const { evaluacion_id, alumno_id, nota, observaciones } = req.body;

    if (!evaluacion_id || !alumno_id || nota === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Faltan datos requeridos'
      });
    }

    // Verificar si ya existe la calificación
    const { data: existing } = await supabase
      .from('calificaciones')
      .select('id')
      .eq('evaluacion_id', evaluacion_id)
      .eq('alumno_id', alumno_id)
      .single();

    let result;
    if (existing) {
      // Actualizar
      result = await supabase
        .from('calificaciones')
        .update({
          nota,
          observaciones,
          updated_at: new Date().toISOString()
        })
        .eq('id', existing.id)
        .select()
        .single();
    } else {
      // Insertar
      result = await supabase
        .from('calificaciones')
        .insert({
          evaluacion_id,
          alumno_id,
          nota,
          observaciones
        })
        .select()
        .single();
    }

    if (result.error) throw result.error;

    // ========================================
    // ENVIAR NOTIFICACIÓN PUSH
    // ========================================
    try {
      // Obtener información de la evaluación y curso
      const { data: evaluacion } = await supabase
        .from('evaluaciones')
        .select(`
          nombre,
          asignaciones!inner (
            asistencia_curso!inner (
              nombre
            ),
            docentes!inner (
              personas!inner (
                nombres,
                apellidos
              )
            )
          )
        `)
        .eq('id', evaluacion_id)
        .single();

      if (evaluacion) {
        const asignacion = Array.isArray(evaluacion.asignaciones) 
          ? evaluacion.asignaciones[0] 
          : evaluacion.asignaciones;
        
        const curso = asignacion?.asistencia_curso;
        
        // Manejar docentes como objeto o array
        const docenteData = asignacion?.docentes 
          ? (Array.isArray(asignacion.docentes) ? asignacion.docentes[0] : asignacion.docentes)
          : null;
        
        // Manejar personas como objeto o array
        const personaData = docenteData?.personas 
          ? (Array.isArray(docenteData.personas) ? docenteData.personas[0] : docenteData.personas)
          : null;
        
        const nombreProfesor = personaData 
          ? `${personaData.nombres} ${personaData.apellidos}` 
          : 'El profesor';
        
        // Manejar curso como objeto o array
        const cursoData = Array.isArray(curso) ? curso[0] : curso;
        const cursoNombre = cursoData?.nombre || 'el curso';

        await notificationService.enviarAEstudiante(alumno_id, {
          tipo: 'calificacion',
          titulo: '📝 Nueva Calificación',
          mensaje: `${nombreProfesor} calificó ${cursoNombre}: ${nota}`,
          datos: {
            alumno_id: alumno_id.toString(),
            evaluacion_id: evaluacion_id.toString(),
            nota: nota.toString(),
            curso: cursoNombre
          }
        });
      }
    } catch (notifError: any) {
      // No fallar el registro si falla la notificación
      console.error('Error al enviar notificación:', notifError.message);
    }
    // ========================================

    res.json({
      success: true,
      message: 'Calificación registrada exitosamente',
      data: result.data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al registrar calificación',
      error: error.message
    });
  }
});

// GET /api/calificaciones/evaluaciones/:asignacionId - Evaluaciones de un curso
router.get('/evaluaciones/:asignacionId', async (req, res) => {
  try {
    const { asignacionId } = req.params;

    const { data, error } = await supabase
      .from('evaluaciones')
      .select('*')
      .eq('asignacion_id', asignacionId)
      .eq('activo', true)
      .order('orden');

    if (error) throw error;

    res.json({
      success: true,
      data: data || []
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener evaluaciones',
      error: error.message
    });
  }
});

// POST /api/calificaciones/evaluaciones - Crear evaluación
router.post('/evaluaciones', async (req, res) => {
  try {
    const { asignacion_id, nombre, peso, orden } = req.body;

    if (!asignacion_id || !nombre) {
      return res.status(400).json({
        success: false,
        message: 'Faltan datos requeridos'
      });
    }

    const { data, error } = await supabase
      .from('evaluaciones')
      .insert({
        asignacion_id,
        nombre,
        peso: peso || 1.0,
        orden: orden || 0
      })
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Evaluación creada exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al crear evaluación',
      error: error.message
    });
  }
});

// DELETE /api/calificaciones/evaluaciones/:id - Eliminar evaluación
router.delete('/evaluaciones/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { error } = await supabase
      .from('evaluaciones')
      .update({ activo: false })
      .eq('id', id);

    if (error) throw error;

    res.json({
      success: true,
      message: 'Evaluación eliminada exitosamente'
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al eliminar evaluación',
      error: error.message
    });
  }
});

export default router;