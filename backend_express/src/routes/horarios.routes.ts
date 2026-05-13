import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// GET /api/horarios/alumno/:alumnoId - Horario de un alumno
router.get('/alumno/:alumnoId', async (req, res) => {
  try {
    const { alumnoId } = req.params;

    // Obtener sección del alumno
    const { data: matricula, error: matriculaError } = await supabase
      .from('matriculas')
      .select('seccion_id')
      .eq('alumno_id', alumnoId)
      .eq('estado', 'activo')
      .single();

    if (matriculaError) throw matriculaError;

    // Obtener horarios de la sección
    const { data, error } = await supabase
      .from('horarios')
      .select(`
        id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon,
        asignaciones!inner (
          cursos!inner (
            nombre,
            color,
            icono
          ),
          docentes!inner (
            personas!inner (
              nombres,
              apellidos
            )
          ),
          secciones!inner (
            nombre,
            grados!inner (
              nombre
            )
          )
        )
      `)
      .eq('asignaciones.seccion_id', matricula.seccion_id)
      .eq('activo', true)
      .order('dia_semana')
      .order('hora_inicio');

    if (error) throw error;

    const horariosFormateados = data?.map((horario: any) => ({
      id: horario.id,
      dia_semana: horario.dia_semana,
      hora_inicio: horario.hora_inicio,
      hora_fin: horario.hora_fin,
      salon: horario.salon,
      curso: horario.asignaciones.cursos.nombre,
      color: horario.asignaciones.cursos.color,
      icono: horario.asignaciones.cursos.icono,
      profesor: `${horario.asignaciones.docentes.personas.nombres} ${horario.asignaciones.docentes.personas.apellidos}`,
      seccion: `${horario.asignaciones.secciones.grados.nombre}${horario.asignaciones.secciones.nombre}`
    })) || [];

    res.json({
      success: true,
      data: horariosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener horario del alumno',
      error: error.message
    });
  }
});

// GET /api/horarios/profesor/:docenteId - Horario de un profesor
router.get('/profesor/:docenteId', async (req, res) => {
  try {
    const { docenteId } = req.params;

    const { data, error } = await supabase
      .from('horarios')
      .select(`
        id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon,
        asignaciones!inner (
          cursos!inner (
            nombre,
            color,
            icono
          ),
          secciones!inner (
            nombre,
            grados!inner (
              nombre
            )
          )
        )
      `)
      .eq('asignaciones.docente_id', docenteId)
      .eq('activo', true)
      .order('dia_semana')
      .order('hora_inicio');

    if (error) throw error;

    const horariosFormateados = data?.map((horario: any) => ({
      id: horario.id,
      dia_semana: horario.dia_semana,
      hora_inicio: horario.hora_inicio,
      hora_fin: horario.hora_fin,
      salon: horario.salon,
      curso: horario.asignaciones.cursos.nombre,
      color: horario.asignaciones.cursos.color,
      icono: horario.asignaciones.cursos.icono,
      seccion: `${horario.asignaciones.secciones.grados.nombre}${horario.asignaciones.secciones.nombre}`
    })) || [];

    res.json({
      success: true,
      data: horariosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener horario del profesor',
      error: error.message
    });
  }
});

// GET /api/horarios/seccion/:seccionId - Horario de una sección
router.get('/seccion/:seccionId', async (req, res) => {
  try {
    const { seccionId } = req.params;

    const { data, error } = await supabase
      .from('horarios')
      .select(`
        id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon,
        asignaciones!inner (
          cursos!inner (
            nombre,
            color,
            icono
          ),
          docentes!inner (
            personas!inner (
              nombres,
              apellidos
            )
          )
        )
      `)
      .eq('asignaciones.seccion_id', seccionId)
      .eq('activo', true)
      .order('dia_semana')
      .order('hora_inicio');

    if (error) throw error;

    const horariosFormateados = data?.map((horario: any) => ({
      id: horario.id,
      dia_semana: horario.dia_semana,
      hora_inicio: horario.hora_inicio,
      hora_fin: horario.hora_fin,
      salon: horario.salon,
      curso: horario.asignaciones.cursos.nombre,
      color: horario.asignaciones.cursos.color,
      icono: horario.asignaciones.cursos.icono,
      profesor: `${horario.asignaciones.docentes.personas.nombres} ${horario.asignaciones.docentes.personas.apellidos}`
    })) || [];

    res.json({
      success: true,
      data: horariosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener horario de la sección',
      error: error.message
    });
  }
});

// POST /api/horarios - Crear horario
router.post('/', async (req, res) => {
  try {
    const { asignacion_id, dia_semana, hora_inicio, hora_fin, salon } = req.body;

    if (!asignacion_id || !dia_semana || !hora_inicio || !hora_fin) {
      return res.status(400).json({
        success: false,
        message: 'Faltan datos requeridos'
      });
    }

    const { data, error } = await supabase
      .from('horarios')
      .insert({
        asignacion_id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon
      })
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Horario creado exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al crear horario',
      error: error.message
    });
  }
});

// PUT /api/horarios/:id - Actualizar horario
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { dia_semana, hora_inicio, hora_fin, salon } = req.body;

    const { data, error } = await supabase
      .from('horarios')
      .update({
        dia_semana,
        hora_inicio,
        hora_fin,
        salon
      })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Horario actualizado exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al actualizar horario',
      error: error.message
    });
  }
});

// DELETE /api/horarios/:id - Eliminar horario
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { error } = await supabase
      .from('horarios')
      .update({ activo: false })
      .eq('id', id);

    if (error) throw error;

    res.json({
      success: true,
      message: 'Horario eliminado exitosamente'
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al eliminar horario',
      error: error.message
    });
  }
});

export default router;