import { Router } from 'express';
import supabase from '../config/database';
import QRCode from 'qrcode';

const router = Router();

// Obtener perfil de usuario por ID
router.get('/perfil/:id', async (req, res) => {
  try {
    const { id } = req.params;

    // Buscar usuario con sus relaciones
    const { data: usuarios, error } = await supabase
      .from('usuarios')
      .select(`
        id,
        email,
        rol,
        activo,
        personas!inner (
          id,
          nombres,
          apellidos,
          dni,
           telefono,
           fecha_nacimiento
        )
      `)
      .eq('id', id)
      .limit(1);

    if (error) throw error;

    if (!usuarios || usuarios.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Usuario no encontrado'
      });
    }

    const usuario = usuarios[0];
    const persona = Array.isArray(usuario.personas) ? usuario.personas[0] : usuario.personas;

    let perfilCompleto: any = {
      id: usuario.id,
      email: usuario.email,
      rol: usuario.rol,
      activo: usuario.activo,
      persona: {
        id: persona.id,
        nombres: persona.nombres,
        apellidos: persona.apellidos,
        dni: persona.dni,
        telefono: persona.telefono,
        fecha_nacimiento: persona.fecha_nacimiento
      }
    };

    // Función auxiliar para obtener datos de alumno por persona_id
    async function obtenerDatosAlumno(personaId: string) {
      const { data: alumno } = await supabase
        .from('alumnos')
        .select(`
          id,
          codigo,
          matriculas!inner (
            id,
            secciones!inner (
              id,
              nombre,
              grados!inner (
                nombre
              )
            )
          ),
          codigos_qr (
            codigo,
            activo
          )
        `)
        .eq('persona_id', personaId)
        .eq('activo', true)
        .limit(1);

      if (alumno && alumno.length > 0) {
        const matriculas = Array.isArray(alumno[0].matriculas) ? alumno[0].matriculas : [alumno[0].matriculas];
        const matriculaActiva = matriculas[0];
        const secciones = Array.isArray(matriculaActiva.secciones) ? matriculaActiva.secciones[0] : matriculaActiva.secciones;
        const grados = Array.isArray(secciones.grados) ? secciones.grados[0] : secciones.grados;
        const codigoQR = alumno[0].codigos_qr?.find((qr: any) => qr.activo);
        const qrCodeString = codigoQR?.codigo || null;
        
        let qrImage = null;
        if (qrCodeString) {
          try {
            qrImage = await QRCode.toDataURL(qrCodeString, {
              width: 300,
              margin: 2,
              color: {
                dark: '#000000',
                light: '#FFFFFF'
              }
            });
          } catch (qrError) {
            console.error('Error generando QR image en perfil:', qrError);
          }
        }

        // Obtener nombre completo del alumno
        const { data: alumnoPersona } = await supabase
          .from('personas')
          .select('nombres, apellidos')
          .eq('id', personaId)
          .single();

        perfilCompleto.alumno = {
          id: alumno[0].id,
          codigo: alumno[0].codigo,
          seccion: `${grados.nombre} ${secciones.nombre}`,
          seccion_id: secciones.id,
          codigo_qr: qrCodeString,
          qr_image: qrImage,
          nombre_completo: alumnoPersona ? `${alumnoPersona.nombres} ${alumnoPersona.apellidos}` : ''
        };
      }
    }

    const rolLower = usuario.rol.toLowerCase();

    if (rolLower === 'alumno') {
      await obtenerDatosAlumno(persona.id);
    }

    if (rolLower === 'padre') {
      const { data: relacion } = await supabase
        .from('padres_alumnos')
        .select('alumno_id, alumnos!inner(persona_id, estado)')
        .eq('padre_id', persona.id)
        .eq('alumnos.estado', 'activo')
        .maybeSingle();

      if (relacion) {
        const alumnosData = relacion.alumnos as any;
        await obtenerDatosAlumno(alumnosData.persona_id);
      }
    }

    // Si es docente, obtener datos adicionales
    if (usuario.rol === 'DOCENTE') {
      const { data: docente } = await supabase
        .from('docentes')
        .select(`
          id,
          especialidad,
          tutor_seccion (
            secciones!inner (
              id,
              nombre,
              grados!inner (
                nombre
              )
            )
          )
        `)
        .eq('persona_id', persona.id)
        .eq('activo', true)
        .limit(1);

      if (docente && docente.length > 0) {
        const tutorSeccion = docente[0].tutor_seccion && docente[0].tutor_seccion.length > 0 ? docente[0].tutor_seccion[0] : null;
        let seccionTutoria = null;

        if (tutorSeccion) {
          const secciones = Array.isArray(tutorSeccion.secciones) ? tutorSeccion.secciones[0] : tutorSeccion.secciones;
          const grados = Array.isArray(secciones.grados) ? secciones.grados[0] : secciones.grados;
          seccionTutoria = `${grados.nombre} ${secciones.nombre}`;
        }

        perfilCompleto.docente = {
          id: docente[0].id,
          especialidad: docente[0].especialidad,
          es_tutor: tutorSeccion !== null,
          seccion_tutoria: seccionTutoria
        };
      }
    }

    res.json({
      success: true,
      data: perfilCompleto
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener perfil',
      error: error.message
    });
  }
});

// Actualizar perfil de usuario
router.put('/perfil/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { telefono } = req.body;

    // Obtener persona_id del usuario
    const { data: usuario, error: errorUsuario } = await supabase
      .from('usuarios')
      .select('persona_id')
      .eq('id', id)
      .single();

    if (errorUsuario) throw errorUsuario;

    if (!usuario) {
      return res.status(404).json({
        success: false,
        message: 'Usuario no encontrado'
      });
    }

    // Actualizar datos de la persona
    const { data: personaActualizada, error: errorActualizar } = await supabase
      .from('personas')
      .update({
        telefono
      })
      .eq('id', usuario.persona_id)
      .select()
      .single();

    if (errorActualizar) throw errorActualizar;

    res.json({
      success: true,
      message: 'Perfil actualizado exitosamente',
      data: personaActualizada
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al actualizar perfil',
      error: error.message
    });
  }
});

// Obtener todos los usuarios (para admin)
router.get('/', async (req, res) => {
  try {
    const { rol, activo, search, limit = 100 } = req.query;

    let query = supabase
      .from('usuarios')
      .select(`
        id,
        email,
        rol,
        activo,
        personas!inner (
          nombres,
          apellidos,
          dni
        )
      `);

    // Filtros
    if (rol) {
      query = query.eq('rol', rol);
    }

    if (activo !== undefined) {
      query = query.eq('activo', activo === 'true');
    }

    if (search) {
      query = query.or(`email.ilike.%${search}%,personas.nombres.ilike.%${search}%,personas.apellidos.ilike.%${search}%`);
    }

    query = query.order('personas(apellidos)', { ascending: true })
      .limit(Number(limit));

    const { data: usuarios, error } = await query;

    if (error) throw error;

    const usuariosFormateados = usuarios?.map((u: any) => ({
      id: u.id,
      email: u.email,
      rol: u.rol,
      activo: u.activo,
      nombre_completo: `${u.personas.nombres} ${u.personas.apellidos}`,
      dni: u.personas.dni
    })) || [];

    res.json({
      success: true,
      data: usuariosFormateados,
      total: usuariosFormateados.length
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener usuarios',
      error: error.message
    });
  }
});

export default router;
