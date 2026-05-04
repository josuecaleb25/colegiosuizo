import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// Obtener todos los alumnos
router.get('/', async (req, res) => {
  try {
    const { seccion, search, limit = 200 } = req.query;
    
    let query = supabase
      .from('asistencia_alumno')
      .select(`
        id,
        codigo,
        dni,
        nombres,
        apellidos,
        nombre_completo,
        fecha_nacimiento,
        email_padre,
        qr_token,
        activo,
        seccion_id,
        asistencia_seccion!inner (
          id,
          nombre,
          asistencia_grado!inner (
            id,
            nombre
          )
        )
      `)
      .eq('activo', true);

    // Filtro por sección
    if (seccion) {
      query = query.ilike('asistencia_seccion.nombre', `%${seccion}%`);
    }

    // Filtro por búsqueda
    if (search) {
      query = query.or(`nombres.ilike.%${search}%,apellidos.ilike.%${search}%,codigo.ilike.%${search}%`);
    }

    query = query.order('asistencia_seccion(nombre)', { ascending: true })
                 .order('apellidos', { ascending: true })
                 .order('nombres', { ascending: true })
                 .limit(Number(limit));

    const { data: alumnos, error } = await query;

    if (error) throw error;

    const alumnosFormateados = alumnos?.map((a: any) => ({
      id: a.id,
      codigo: a.codigo,
      dni: a.dni,
      nombres: a.nombres,
      apellidos: a.apellidos,
      nombre_completo: a.nombre_completo,
      fecha_nacimiento: a.fecha_nacimiento,
      email_padre: a.email_padre,
      qr_token: a.qr_token,
      activo: a.activo,
      seccion: `${a.asistencia_seccion.asistencia_grado.nombre} ${a.asistencia_seccion.nombre}`,
      seccion_id: a.seccion_id,
      grado_id: a.asistencia_seccion.asistencia_grado.id,
      grado_nombre: a.asistencia_seccion.asistencia_grado.nombre,
      seccion_nombre: a.asistencia_seccion.nombre
    })) || [];

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

// Obtener alumno por ID
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { data: alumnos, error } = await supabase
      .from('asistencia_alumno')
      .select(`
        *,
        asistencia_seccion!inner (
          id,
          nombre,
          asistencia_grado!inner (
            id,
            nombre
          )
        )
      `)
      .eq('id', id)
      .limit(1);

    if (error) throw error;

    if (!alumnos || alumnos.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    const a = alumnos[0];
    const alumnoFormateado = {
      ...a,
      seccion: `${a.asistencia_seccion.asistencia_grado.nombre} ${a.asistencia_seccion.nombre}`,
      grado_id: a.asistencia_seccion.asistencia_grado.id
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

// Crear alumno
router.post('/', async (req, res) => {
  try {
    const {
      codigo,
      dni,
      nombres,
      apellidos,
      seccion_id,
      fecha_nacimiento,
      email_padre
    } = req.body;

    if (!codigo || !dni || !nombres || !apellidos || !seccion_id) {
      return res.status(400).json({
        success: false,
        message: 'Faltan campos requeridos'
      });
    }

    const nombre_completo = `${nombres} ${apellidos}`;

    const { data: nuevoAlumno, error } = await supabase
      .from('asistencia_alumno')
      .insert({
        codigo,
        dni,
        nombres,
        apellidos,
        nombre_completo,
        seccion_id,
        fecha_nacimiento,
        email_padre,
        activo: true
      })
      .select()
      .single();

    if (error) throw error;

    res.status(201).json({
      success: true,
      message: 'Alumno creado exitosamente',
      data: nuevoAlumno
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al crear alumno',
      error: error.message
    });
  }
});

// Actualizar alumno
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const {
      codigo,
      dni,
      nombres,
      apellidos,
      seccion_id,
      fecha_nacimiento,
      email_padre,
      activo
    } = req.body;

    const nombre_completo = `${nombres} ${apellidos}`;

    const { data: alumnoActualizado, error } = await supabase
      .from('asistencia_alumno')
      .update({
        codigo,
        dni,
        nombres,
        apellidos,
        nombre_completo,
        seccion_id,
        fecha_nacimiento,
        email_padre,
        activo
      })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    if (!alumnoActualizado) {
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    res.json({
      success: true,
      message: 'Alumno actualizado exitosamente',
      data: alumnoActualizado
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al actualizar alumno',
      error: error.message
    });
  }
});

// Eliminar alumno (soft delete)
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { data: alumnoEliminado, error } = await supabase
      .from('asistencia_alumno')
      .update({ activo: false })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    if (!alumnoEliminado) {
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    res.json({
      success: true,
      message: 'Alumno eliminado exitosamente'
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al eliminar alumno',
      error: error.message
    });
  }
});

export default router;
