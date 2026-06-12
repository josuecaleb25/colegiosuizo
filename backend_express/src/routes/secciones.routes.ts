import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// GET /api/secciones - Obtener secciones disponibles según el rol
router.get('/', async (req, res) => {
  try {
    const { usuario_id, rol } = req.query;

    if (!usuario_id || !rol) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere usuario_id y rol'
      });
    }

    let secciones: any[] = [];

    // Si es ADMIN o AUXILIAR, devolver todas las secciones
    if (rol === 'admin' || rol === 'auxiliar' || rol === 'administrador') {
      
      const { data, error } = await supabase
        .from('secciones')
        .select(`
          id,
          nombre,
          grados!inner (
            nombre
          )
        `);

      if (error) throw error;

      secciones = data?.map((s: any) => ({
        id: s.id,
        nombre: `${s.grados.nombre} ${s.nombre}`, // "1ro A"
        grado: s.grados.nombre,
        seccion: s.nombre
      })) || [];

    } 
    // Si es PROFESOR, devolver solo sus secciones asignadas
    else if (rol === 'profesor' || rol === 'docente') {

      // 1. Buscar el docente_id usando usuario_id (que es persona_id)
      const { data: docente, error: docenteError } = await supabase
        .from('docentes')
        .select('id')
        .eq('persona_id', usuario_id)
        .single();

      if (docenteError || !docente) {
        return res.json({
          success: true,
          data: [],
          message: 'No se encontraron secciones asignadas'
        });
      }

      // 2. Obtener las secciones asignadas al docente
      const { data: asignaciones, error: asignError } = await supabase
        .from('asignaciones')
        .select(`
          seccion_id,
          secciones!inner (
            id,
            nombre,
            grados!inner (
              nombre
            )
          )
        `)
        .eq('docente_id', docente.id);

      if (asignError) throw asignError;

      // Eliminar duplicados por seccion_id
      const seccionesUnicas = new Map();
      asignaciones?.forEach((a: any) => {
        const seccionId = a.secciones.id;
        if (!seccionesUnicas.has(seccionId)) {
          seccionesUnicas.set(seccionId, {
            id: a.secciones.id,
            nombre: `${a.secciones.grados.nombre} ${a.secciones.nombre}`, // "1ro A"
            grado: a.secciones.grados.nombre,
            seccion: a.secciones.nombre
          });
        }
      });

      secciones = Array.from(seccionesUnicas.values());

    }
    else if (rol === 'alumno' || rol === 'padre') {
      // Leaderboard público: todos ven todas las secciones
      const { data, error } = await supabase
        .from('secciones')
        .select(`
          id,
          nombre,
          grados!inner (
            nombre
          )
        `);

      if (!error && data) {
        secciones = data.map((s: any) => ({
          id: s.id,
          nombre: `${s.grados.nombre} ${s.nombre}`,
          grado: s.grados.nombre,
          seccion: s.nombre
        }));
      }
    }
    else {
      // Otros roles no tienen acceso a secciones
      return res.json({
        success: true,
        data: [],
        message: 'Rol no autorizado para ver secciones'
      });
    }

    res.json({
      success: true,
      data: secciones,
      total: secciones.length,
      message: `Se encontraron ${secciones.length} secciones`
    });

  } catch (error: any) {
    console.error('❌ Error al obtener secciones:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener secciones',
      error: error.message
    });
  }
});

export default router;
