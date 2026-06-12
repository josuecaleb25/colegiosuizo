import { Router } from 'express';
import supabase from '../config/database';
import QRCode from 'qrcode';

const router = Router();

// Obtener perfil de usuario por ID
router.get('/perfil/:id', async (req, res) => {
  try {
    const { id } = req.params;

    // Buscar persona por ID (login usa personas, no necesariamente usuarios)
    const { data: personas, error: personaError } = await supabase
      .from('personas')
      .select(`
        id,
        dni,
        nombres,
        apellidos,
        correo,
        telefono,
        fecha_nacimiento,
        alumnos (
          id,
          codigo_alumno,
          estado,
          matriculas (
            seccion_id,
            secciones (
              nombre,
              grados ( nombre )
            )
          )
        ),
        docentes (id, codigo_docente, estado)
      `)
      .eq('id', id)
      .limit(1);

    if (personaError) throw personaError;

    if (!personas || personas.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Persona no encontrada'
      });
    }

    const persona = personas[0];

    // Determinar rol igual que en login
    let rol = 'padre';
    if (persona.docentes && persona.docentes.length > 0) {
      rol = 'profesor';
    }

    let perfilCompleto: any = {
      id: persona.id,
      email: persona.correo,
      rol: rol,
      activo: true,
      persona: {
        id: persona.id,
        nombres: persona.nombres,
        apellidos: persona.apellidos,
        dni: persona.dni,
        telefono: persona.telefono,
        fecha_nacimiento: persona.fecha_nacimiento
      }
    };

    // Función para generar QR y armar objeto alumno
    async function generarQRCompleto(alumnoRow: any, personaId: string): Promise<any> {
      const matriculas = Array.isArray(alumnoRow.matriculas) ? alumnoRow.matriculas : [alumnoRow.matriculas];
      const matriculaActiva = matriculas[0];
      const secciones = Array.isArray(matriculaActiva.secciones) ? matriculaActiva.secciones[0] : matriculaActiva.secciones;
      const grados = Array.isArray(secciones.grados) ? secciones.grados[0] : secciones.grados;

      // Buscar QR token en tabla codigos_qr
      const { data: qrData } = await supabase
        .from('codigos_qr')
        .select('codigo')
        .eq('alumno_id', alumnoRow.id)
        .eq('activo', true)
        .limit(1);
      const qrCodeString = qrData && qrData.length > 0 ? qrData[0].codigo : null;

      // Si no hay QR en BD, usar el codigo_alumno como contenido
      const qrContent = qrCodeString || alumnoRow.codigo_alumno || '';

      let qrImage = null;
      if (qrContent) {
        try {
          qrImage = await QRCode.toDataURL(qrContent, {
            width: 300, margin: 2,
            color: { dark: '#000000', light: '#FFFFFF' }
          });
        } catch (qrError) {
          console.error('Error generando QR image en perfil:', qrError);
        }
      }

      const { data: alumnoPersona } = await supabase
        .from('personas')
        .select('nombres, apellidos')
        .eq('id', personaId)
        .single();

      return {
        id: alumnoRow.id,
        codigo: alumnoRow.codigo_alumno || '',
        seccion: `${grados.nombre} ${secciones.nombre}`,
        seccion_id: secciones.id,
        codigo_qr: qrCodeString,
        qr_image: qrImage,
        nombre_completo: alumnoPersona ? `${alumnoPersona.nombres} ${alumnoPersona.apellidos}` : ''
      };
    }

    // Si ya trajimos alumnos en la consulta principal (ALUMNO directo)
    if (persona.alumnos && persona.alumnos.length > 0) {
      const alumnoRow = persona.alumnos[0];
      if (alumnoRow.estado === 'activo') {
        perfilCompleto.alumno = await generarQRCompleto(alumnoRow, persona.id);
      }
    }

    // Si es padre, buscar datos del hijo a través de padres_alumnos
    if (rol === 'padre' && (!persona.alumnos || persona.alumnos.length === 0)) {
      const { data: relacion } = await supabase
        .from('padres_alumnos')
        .select('alumno_id, alumnos!inner(persona_id, estado)')
        .eq('padre_id', persona.id)
        .eq('alumnos.estado', 'activo')
        .maybeSingle();

      if (relacion) {
        const alumnosData = relacion.alumnos as any;
        const { data: hijoAlumno } = await supabase
          .from('alumnos')
          .select('id, codigo_alumno, matriculas!inner(secciones!inner(id, nombre, grados!inner(nombre)))')
          .eq('persona_id', alumnosData.persona_id)
          .eq('activo', true)
          .limit(1);

        if (hijoAlumno && hijoAlumno.length > 0) {
          perfilCompleto.alumno = await generarQRCompleto(hijoAlumno[0], alumnosData.persona_id);
        }
      }
    }

    // Si es docente, obtener datos adicionales
    if (rol === 'profesor' && persona.docentes && persona.docentes.length > 0) {
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

    const { data: usuario, error: errorUsuario } = await supabase
      .from('usuarios')
      .select('persona_id')
      .eq('persona_id', id)
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
