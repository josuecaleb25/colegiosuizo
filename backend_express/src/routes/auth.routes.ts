import { Router } from 'express';
import bcrypt from 'bcryptjs';
import { studentHasParentLoginCredentials } from '../services/student-parent-login';
import supabase from '../config/database';
import { signAccessToken } from '../middleware/auth';
import { createRateLimiter } from '../middleware/rate-limit';

const router = Router();
const loginRateLimit = createRateLimiter(15 * 60 * 1000, 10, req => {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : 'invalid';
  return `${req.ip || 'unknown'}:${email}`;
});
const registerRateLimit = createRateLimiter(60 * 60 * 1000, 5);

// Login
router.post('/login', loginRateLimit, async (req, res) => {
  try {
    const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    const password = typeof req.body?.password === 'string' ? req.body.password : '';

    if (!email || !password || email.length > 254 || password.length > 256 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({
        success: false,
        message: 'Credenciales inválidas'
      });
    }

    const { data: usuario, error: usuarioError } = await supabase
      .from('usuarios')
      .select('id, persona_id, email, rol, activo, password')
      .ilike('email', email)
      .maybeSingle();

    if (usuarioError) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales incorrectas'
      });
    }

    let persona: any;
    let rol: string;

    if (usuario) {
      if (usuario.activo === false) return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });

      const validHash = typeof usuario.password === 'string' && /^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$/.test(usuario.password);
      if (!validHash || !usuario.persona_id || !usuario.rol) {
        return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
      }

      const passwordOk = await bcrypt.compare(password, usuario.password);
      if (!passwordOk) return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });

      const result = await supabase
        .from('personas')
        .select(`
          id,
          nombres,
          apellidos,
          correo,
          alumnos (
            matriculas (
              seccion_id,
              secciones ( nombre, grados ( nombre ) )
            )
          )
        `)
        .eq('id', usuario.persona_id)
        .maybeSingle();

      if (result.error || !result.data) return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
      persona = result.data;
      rol = usuario.rol;
    } else {
      const result = await supabase
        .from('personas')
        .select(`
          id,
          nombres,
          apellidos,
          correo,
          alumnos (
            matriculas (
              seccion_id,
              secciones ( nombre, grados ( nombre ) )
            )
          )
        `)
        .eq('correo', email)
        .maybeSingle();

      if (result.error || !result.data || !studentHasParentLoginCredentials(result.data, email, password)) {
        return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
      }
      persona = result.data;
      rol = 'padre';
    }

    const token = signAccessToken({ id: persona.id, email: persona.correo, rol });

    // Obtener sección si es alumno/padre
    let seccion = '';
    if (persona.alumnos && persona.alumnos.length > 0) {
      const alumno = persona.alumnos[0];
      if (alumno.matriculas && alumno.matriculas.length > 0) {
        const matricula = alumno.matriculas[0];
        
        // Manejar secciones como objeto o array
        const seccionData = Array.isArray(matricula.secciones) 
          ? matricula.secciones[0] 
          : matricula.secciones;
        
        // Manejar grados como objeto o array
        const gradoData = seccionData?.grados 
          ? (Array.isArray(seccionData.grados) ? seccionData.grados[0] : seccionData.grados)
          : null;
        
        const gradoNombre = gradoData?.nombre || '';
        const seccionNombre = seccionData?.nombre || '';
        seccion = `${gradoNombre} ${seccionNombre}`.trim();
      }
    }

    res.json({
      success: true,
      message: 'Login exitoso',
      data: {
        user: {
          id: persona.id,
          email: persona.correo,
          nombres: persona.nombres,
          apellidos: persona.apellidos,
          rol: rol,
          nombre_completo: `${persona.nombres} ${persona.apellidos}`,
          seccion: seccion
        },
        tokens: {
          access: token
        }
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error en el login',
    });
  }
});

// Register
router.post('/register', registerRateLimit, async (req, res) => {
  try {
    const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    const password = typeof req.body?.password === 'string' ? req.body.password : '';
    const { nombres, apellidos, telefono, dni } = req.body || {};

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254 || password.length < 12 || password.length > 256 || !nombres || !apellidos || String(nombres).length > 100 || String(apellidos).length > 100) {
      return res.status(400).json({
        success: false,
        message: 'Datos de registro inválidos'
      });
    }

    // Verificar si el usuario ya existe
    const { data: existingUsers } = await supabase
      .from('usuarios')
      .select('id')
      .eq('email', email)
      .limit(1);

    if (existingUsers && existingUsers.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'El email ya está registrado'
      });
    }

    // Crear persona primero
    const { data: newPersona, error: personaError } = await supabase
      .from('personas')
      .insert({
        dni: dni || null,
        nombres,
        apellidos,
        telefono: telefono || null,
        correo: email
      })
      .select()
      .single();

    if (personaError) throw personaError;

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash(password, 12);

    // Crear usuario
    const { data: newUser, error: userError } = await supabase
      .from('usuarios')
      .insert({
        persona_id: newPersona.id,
        email,
        password: hashedPassword,
        rol: 'padre',
        activo: true
      })
      .select()
      .single();

    if (userError) throw userError;

    res.json({
      success: true,
      message: 'Usuario registrado exitosamente',
      data: {
        id: newUser.id,
        email: newUser.email,
        nombres,
        apellidos,
        rol: newUser.rol
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al registrar usuario'
    });
  }
});

export default router;
