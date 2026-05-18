import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// GET /api/cursos - Obtener todos los cursos
router.get('/', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('cursos')
      .select('*')
      .eq('activo', true)
      .order('nombre');

    if (error) throw error;

    res.json({
      success: true,
      data: data || []
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener cursos',
      error: error.message
    });
  }
});

// GET /api/cursos/alumno/:personaId - Cursos de un alumno (usando persona_id)
router.get('/alumno/:personaId', async (req, res) => {
  try {
    const { personaId } = req.params;

    // Buscar alumno por persona_id
    const { data: alumno, error: alumnoError } = await supabase
      .from('alumnos')
      .select('id')
      .eq('persona_id', personaId)
      .eq('estado', 'activo')
      .single();

    if (alumnoError || !alumno) {
      console.log('No se encontró alumno para persona_id:', personaId);
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    // Obtener la matrícula del alumno para saber su sección
    const { data: matricula, error: matriculaError } = await supabase
      .from('matriculas')
      .select('seccion_id, secciones(nombre, grados(nombre))')
      .eq('alumno_id', alumno.id)
      .eq('estado', 'activo')
      .single();

    if (matriculaError || !matricula) {
      return res.status(404).json({
        success: false,
        message: 'No se encontró matrícula activa para el alumno'
      });
    }

    const seccion = `${(matricula.secciones as any).grados.nombre}${(matricula.secciones as any).nombre}`;
    const seccionId = matricula.seccion_id;

    // Obtener las asignaciones de la sección del alumno (cursos con sus profesores)
    const { data: asignaciones, error: asignacionesError } = await supabase
      .from('asignaciones')
      .select(`
        id,
        curso_id,
        cursos!inner (
          id,
          nombre,
          descripcion,
          color,
          icono,
          activo
        ),
        docentes!inner (
          id,
          personas!inner (
            nombres,
            apellidos
          )
        )
      `)
      .eq('seccion_id', seccionId);

    if (asignacionesError) throw asignacionesError;

    // Formatear cursos con información del profesor
    const cursosFormateados = (asignaciones || []).map((asignacion: any) => {
      const curso = asignacion.cursos;
      const docente = asignacion.docentes;
      // Obtener solo el primer nombre
      const primerNombre = docente.personas.nombres.split(' ')[0];

      return {
        id: curso.id,
        nombre: curso.nombre,
        descripcion: curso.descripcion,
        color: curso.color,
        icono: curso.icono,
        profesor: primerNombre,
        salon: seccion,
        seccion: seccion,
        promedio: '0'
      };
    });

    res.json({
      success: true,
      data: cursosFormateados
    });
  } catch (error: any) {
    console.error('Error en /cursos/alumno:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener cursos del alumno',
      error: error.message
    });
  }
});

// GET /api/cursos/profesor/:docenteId - Cursos de un profesor
router.get('/profesor/:docenteId', async (req, res) => {
  try {
    const { docenteId } = req.params; // Este es el persona_id

    // Buscar el docente por persona_id
    const { data: docente, error: docenteError } = await supabase
      .from('docentes')
      .select('id')
      .eq('persona_id', docenteId)
      .single();

    if (docenteError || !docente) {
      return res.json({
        success: true,
        data: [],
        message: 'No se encontró el docente'
      });
    }

    // Obtener las asignaciones del docente (cursos que enseña)
    const { data: asignaciones, error: asignacionesError } = await supabase
      .from('asignaciones')
      .select(`
        id,
        curso_id,
        seccion_id,
        cursos!inner (
          id,
          nombre,
          descripcion,
          color,
          icono,
          activo
        ),
        secciones!inner (
          nombre,
          grados!inner (
            nombre
          )
        )
      `)
      .eq('docente_id', docente.id);

    if (asignacionesError) throw asignacionesError;

    // Formatear cursos con información de la sección
    const cursosFormateados = (asignaciones || []).map((asignacion: any) => {
      const curso = asignacion.cursos;
      const seccion = asignacion.secciones;
      const grado = seccion.grados;
      const salon = `${grado.nombre} ${seccion.nombre}`;

      return {
        id: curso.id,
        nombre: curso.nombre,
        descripcion: curso.descripcion,
        color: curso.color,
        icono: curso.icono,
        profesor: 'Tú',
        salon: salon,
        seccion: salon,
        promedio: '--'
      };
    });

    res.json({
      success: true,
      data: cursosFormateados
    });
  } catch (error: any) {
    console.error('Error en /cursos/profesor:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener cursos del profesor',
      error: error.message
    });
  }
});

// GET /api/cursos/:cursoId/alumnos/:seccionId - Alumnos de un curso
router.get('/:cursoId/alumnos/:seccionId', async (req, res) => {
  try {
    const { seccionId } = req.params;

    const { data, error } = await supabase
      .from('matriculas')
      .select(`
        alumno_id,
        alumnos!inner (
          id,
          codigo,
          personas!inner (
            nombres,
            apellidos,
            correo
          )
        )
      `)
      .eq('seccion_id', seccionId)
      .eq('estado', 'activo');

    if (error) throw error;

    const alumnosFormateados = data?.map((matricula: any) => ({
      id: matricula.alumnos.id,
      codigo: matricula.alumnos.codigo,
      nombre_completo: `${matricula.alumnos.personas.nombres} ${matricula.alumnos.personas.apellidos}`,
      email: matricula.alumnos.personas.correo
    })) || [];

    res.json({
      success: true,
      data: alumnosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumnos del curso',
      error: error.message
    });
  }
});

export default router;