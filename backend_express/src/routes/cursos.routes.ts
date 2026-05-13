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

// GET /api/cursos/alumno/:alumnoId - Cursos de un alumno
router.get('/alumno/:alumnoId', async (req, res) => {
  try {
    const { alumnoId } = req.params;

    // Obtener todos los cursos activos
    const { data: cursos, error: cursosError } = await supabase
      .from('cursos')
      .select('*')
      .eq('activo', true)
      .order('nombre');

    if (cursosError) throw cursosError;

    // Obtener la matrícula del alumno para saber su sección
    const { data: matricula } = await supabase
      .from('matriculas')
      .select('seccion_id, secciones(nombre, grados(nombre))')
      .eq('alumno_id', alumnoId)
      .eq('estado', 'activo')
      .single();

    const seccion = matricula ? `${(matricula.secciones as any).grados.nombre}${(matricula.secciones as any).nombre}` : 'Sin sección';

    // Formatear cursos con información básica
    const cursosFormateados = (cursos || []).map((curso: any) => ({
      id: curso.id,
      nombre: curso.nombre,
      descripcion: curso.descripcion,
      color: curso.color,
      icono: curso.icono,
      profesor: 'Sin asignar',
      salon: seccion,
      seccion: seccion,
      promedio: '0'
    }));

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
    const { docenteId } = req.params;

    // Obtener todos los cursos activos
    const { data: cursos, error: cursosError } = await supabase
      .from('cursos')
      .select('*')
      .eq('activo', true)
      .order('nombre');

    if (cursosError) throw cursosError;

    // Formatear cursos con información básica
    const cursosFormateados = (cursos || []).map((curso: any) => ({
      id: curso.id,
      nombre: curso.nombre,
      descripcion: curso.descripcion,
      color: curso.color,
      icono: curso.icono,
      profesor: 'Sin asignar',
      salon: 'Sin asignar',
      seccion: 'Sin asignar',
      promedio: '--'
    }));

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