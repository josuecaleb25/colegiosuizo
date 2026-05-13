import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// GET /api/secciones - Obtener secciones disponibles según el rol
router.get('/', async (req, res) => {
  try {
    const { usuario_id, rol } = req.query;

    let secciones: any[] = [];

    if (rol === 'administrador' || rol === 'auxiliar') {
      // Admin y auxiliar ven todas las secciones
      const { data, error } = await supabase
        .from('secciones')
        .select(`
          id,
          nombre,
          grados!inner (
            id,
            nombre
          )
        `)
        .order('grados(nombre)', { ascending: true });

      if (error) throw error;

      secciones = data?.map((sec: any) => {
        const grado = Array.isArray(sec.grados) ? sec.grados[0] : sec.grados;
        return {
          id: sec.id,
          nombre: `${grado.nombre} ${sec.nombre}`,
          grado_id: grado.id,
          seccion_nombre: sec.nombre,
          grado_nombre: grado.nombre
        };
      }) || [];

    } else if (rol === 'profesor' || rol === 'docente') {
      // Profesor solo ve las secciones donde dicta clases
      const { data, error } = await supabase
        .from('asignaciones_docentes')
        .select(`
          secciones!inner (
            id,
            nombre,
            grados!inner (
              id,
              nombre
            )
          ),
          docentes!inner (
            usuarios!inner (
              id
            )
          )
        `)
        .eq('docentes.usuarios.id', usuario_id);

      if (error) throw error;

      const seccionesUnicas = new Map();
      data?.forEach((asig: any) => {
        const seccion = Array.isArray(asig.secciones) ? asig.secciones[0] : asig.secciones;
        const grado = Array.isArray(seccion.grados) ? seccion.grados[0] : seccion.grados;
        
        if (!seccionesUnicas.has(seccion.id)) {
          seccionesUnicas.set(seccion.id, {
            id: seccion.id,
            nombre: `${grado.nombre} ${seccion.nombre}`,
            grado_id: grado.id,
            seccion_nombre: seccion.nombre,
            grado_nombre: grado.nombre
          });
        }
      });

      secciones = Array.from(seccionesUnicas.values());
    }

    res.json({
      success: true,
      data: secciones
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener secciones',
      error: error.message
    });
  }
});

export default router;
