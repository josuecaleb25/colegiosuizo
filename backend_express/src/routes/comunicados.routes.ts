import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// GET /api/comunicados - Obtener comunicados (filtrado por usuario)
router.get('/', async (req, res) => {
  try {
    const { usuario_id, rol, seccion_id, grado_id } = req.query;

    let query = supabase
      .from('comunicados_nuevos')
      .select(`
        id,
        titulo,
        contenido,
        tipo,
        destinatario_tipo,
        fecha_publicacion,
        usuarios!inner (
          personas!inner (
            nombres,
            apellidos
          )
        ),
        secciones (
          nombre,
          grados (
            nombre
          )
        )
      `)
      .eq('activo', true)
      .order('fecha_publicacion', { ascending: false });

    // Filtrar según el rol
    if (rol === 'alumno' || rol === 'padre') {
      // Ver comunicados globales o de su sección/grado
      query = query.or(`destinatario_tipo.eq.global,and(destinatario_tipo.eq.seccion,seccion_id.eq.${seccion_id}),and(destinatario_tipo.eq.grado,grado_id.eq.${grado_id})`);
    }

    const { data, error } = await query;

    if (error) throw error;

    const comunicadosFormateados = data?.map((com: any) => ({
      id: com.id,
      titulo: com.titulo,
      contenido: com.contenido,
      tipo: com.tipo,
      destinatario_tipo: com.destinatario_tipo,
      fecha_publicacion: com.fecha_publicacion,
      autor: `${com.usuarios.personas.nombres} ${com.usuarios.personas.apellidos}`,
      destinatario: com.destinatario_tipo === 'global' 
        ? 'Todos' 
        : com.secciones 
          ? `${com.secciones.grados.nombre}${com.secciones.nombre}`
          : 'N/A'
    })) || [];

    res.json({
      success: true,
      data: comunicadosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener comunicados',
      error: error.message
    });
  }
});

// GET /api/comunicados/:id - Obtener un comunicado específico
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .select(`
        *,
        usuarios!inner (
          personas!inner (
            nombres,
            apellidos
          )
        ),
        secciones (
          nombre,
          grados (
            nombre
          )
        )
      `)
      .eq('id', id)
      .single();

    if (error) throw error;

    const comunicadoFormateado = {
      id: data.id,
      titulo: data.titulo,
      contenido: data.contenido,
      tipo: data.tipo,
      destinatario_tipo: data.destinatario_tipo,
      fecha_publicacion: data.fecha_publicacion,
      autor: `${data.usuarios.personas.nombres} ${data.usuarios.personas.apellidos}`,
      destinatario: data.destinatario_tipo === 'global' 
        ? 'Todos' 
        : data.secciones 
          ? `${data.secciones.grados.nombre}${data.secciones.nombre}`
          : 'N/A'
    };

    res.json({
      success: true,
      data: comunicadoFormateado
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener comunicado',
      error: error.message
    });
  }
});

// POST /api/comunicados - Crear comunicado
router.post('/', async (req, res) => {
  try {
    const { 
      usuario_id, 
      titulo, 
      contenido, 
      tipo, 
      destinatario_tipo, 
      seccion_id, 
      grado_id 
    } = req.body;

    if (!usuario_id || !titulo || !contenido || !destinatario_tipo) {
      return res.status(400).json({
        success: false,
        message: 'Faltan datos requeridos'
      });
    }

    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .insert({
        usuario_id,
        titulo,
        contenido,
        tipo: tipo || 'general',
        destinatario_tipo,
        seccion_id: destinatario_tipo === 'seccion' ? seccion_id : null,
        grado_id: destinatario_tipo === 'grado' ? grado_id : null
      })
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Comunicado publicado exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al crear comunicado',
      error: error.message
    });
  }
});

// POST /api/comunicados/:id/leer - Marcar comunicado como leído
router.post('/:id/leer', async (req, res) => {
  try {
    const { id } = req.params;
    const { usuario_id } = req.body;

    if (!usuario_id) {
      return res.status(400).json({
        success: false,
        message: 'usuario_id es requerido'
      });
    }

    // Verificar si ya fue leído
    const { data: existing } = await supabase
      .from('lecturas_comunicados')
      .select('id')
      .eq('comunicado_id', id)
      .eq('usuario_id', usuario_id)
      .single();

    if (existing) {
      return res.json({
        success: true,
        message: 'Comunicado ya fue marcado como leído'
      });
    }

    const { error } = await supabase
      .from('lecturas_comunicados')
      .insert({
        comunicado_id: id,
        usuario_id
      });

    if (error) throw error;

    res.json({
      success: true,
      message: 'Comunicado marcado como leído'
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al marcar comunicado como leído',
      error: error.message
    });
  }
});

// GET /api/comunicados/historial/enviados - Obtener comunicados enviados por el usuario
router.get('/historial/enviados', async (req, res) => {
  try {
    const { usuario_id } = req.query;

    if (!usuario_id) {
      return res.status(400).json({
        success: false,
        message: 'usuario_id es requerido'
      });
    }

    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .select(`
        id,
        titulo,
        contenido,
        tipo,
        destinatario_tipo,
        fecha_publicacion,
        activo,
        secciones (
          nombre,
          grados (
            nombre
          )
        ),
        grados (
          nombre
        )
      `)
      .eq('usuario_id', usuario_id)
      .eq('activo', true)  // ✅ SOLO mostrar comunicados activos
      .order('fecha_publicacion', { ascending: false });

    if (error) throw error;

    const comunicadosFormateados = data?.map((com: any) => {
      let destinatario = 'Todos';
      
      if (com.destinatario_tipo === 'seccion' && com.secciones) {
        const seccion = Array.isArray(com.secciones) ? com.secciones[0] : com.secciones;
        const grado = Array.isArray(seccion.grados) ? seccion.grados[0] : seccion.grados;
        destinatario = `${grado.nombre} ${seccion.nombre}`;
      } else if (com.destinatario_tipo === 'grado' && com.grados) {
        const grado = Array.isArray(com.grados) ? com.grados[0] : com.grados;
        destinatario = grado.nombre;
      }

      return {
        id: com.id,
        titulo: com.titulo,
        contenido: com.contenido,
        tipo: com.tipo,
        destinatario_tipo: com.destinatario_tipo,
        fecha_publicacion: com.fecha_publicacion,
        destinatario: destinatario,
        estado: 'Enviado'  // ✅ Siempre "Enviado" porque solo mostramos activos
      };
    }) || [];

    res.json({
      success: true,
      data: comunicadosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener historial',
      error: error.message
    });
  }
});

// GET /api/comunicados/:id/lecturas - Obtener lecturas de un comunicado
router.get('/:id/lecturas', async (req, res) => {
  try {
    const { id } = req.params;

    const { data, error } = await supabase
      .from('lecturas_comunicados')
      .select(`
        id,
        fecha_lectura,
        usuarios!inner (
          personas!inner (
            nombres,
            apellidos
          )
        )
      `)
      .eq('comunicado_id', id)
      .order('fecha_lectura', { ascending: false });

    if (error) throw error;

    const lecturasFormateadas = data?.map((lectura: any) => ({
      id: lectura.id,
      fecha_lectura: lectura.fecha_lectura,
      usuario: `${lectura.usuarios.personas.nombres} ${lectura.usuarios.personas.apellidos}`
    })) || [];

    res.json({
      success: true,
      data: lecturasFormateadas
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener lecturas',
      error: error.message
    });
  }
});

// PUT /api/comunicados/:id - Actualizar comunicado
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { 
      titulo, 
      contenido, 
      tipo, 
      destinatario_tipo, 
      seccion_id, 
      grado_id 
    } = req.body;

    const updateData: any = {};
    if (titulo !== undefined) updateData.titulo = titulo;
    if (contenido !== undefined) updateData.contenido = contenido;
    if (tipo !== undefined) updateData.tipo = tipo;
    if (destinatario_tipo !== undefined) {
      updateData.destinatario_tipo = destinatario_tipo;
      updateData.seccion_id = destinatario_tipo === 'seccion' ? seccion_id : null;
      updateData.grado_id = destinatario_tipo === 'grado' ? grado_id : null;
    }

    const { data, error } = await supabase
      .from('comunicados_nuevos')
      .update(updateData)
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Comunicado actualizado exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al actualizar comunicado',
      error: error.message
    });
  }
});

// DELETE /api/comunicados/:id - Eliminar comunicado
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { error } = await supabase
      .from('comunicados_nuevos')
      .update({ activo: false })
      .eq('id', id);

    if (error) throw error;

    res.json({
      success: true,
      message: 'Comunicado eliminado exitosamente'
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al eliminar comunicado',
      error: error.message
    });
  }
});

export default router;