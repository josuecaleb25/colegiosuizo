import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import supabase from '../config/database';

const router = Router();

// Login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email y contraseña son requeridos'
      });
    }

    // Buscar persona por correo
    const { data: personas, error: personaError } = await supabase
      .from('personas')
      .select(`
        id,
        dni,
        nombres,
        apellidos,
        correo,
        alumnos (
          id, 
          codigo_alumno, 
          estado,
          matriculas (
            seccion_id,
            secciones (
              nombre,
              grados (
                nombre
              )
            )
          )
        ),
        docentes (id, codigo_docente, estado)
      `)
      .eq('correo', email)
      .limit(1);

    if (personaError || !personas || personas.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales incorrectas'
      });
    }

    const persona = personas[0];

    // Determinar rol y contraseña esperada (fallback al sistema anterior)
    const rolFallback = () => {
      if (email === 'admin@colegio.com') return 'administrador';
      if (email === 'profesor@colegio.com' || (persona.docentes && persona.docentes.length > 0)) return 'profesor';
      return 'padre';
    };

    const passwordFallback = {
      administrador: 'admin123',
      profesor: 'profesor123',
      padre: 'Suizo2026*'
    } as Record<string, string>;

    // Verificar contraseña contra la tabla usuarios (bcrypt) si existe un hash real
    let rol: string;
    let passwordOk = false;

    const { data: usuario } = await supabase
      .from('usuarios')
      .select('id, email, rol, activo, password')
      .eq('email', email)
      .maybeSingle();

    const esHashReal = (h: string | null | undefined) =>
      !!h && (h.startsWith('$2a$') || h.startsWith('$2b$') || h.startsWith('$2y$')) && !h.includes('placeholder');

    if (usuario && esHashReal(usuario.password)) {
      // Usuario con hash real en la BD -> verificar con bcrypt
      if (usuario.activo === false) {
        return res.status(401).json({ success: false, message: 'Usuario inactivo' });
      }
      try {
        passwordOk = await bcrypt.compare(password, usuario.password);
      } catch {
        passwordOk = false;
      }
      if (!passwordOk) {
        return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
      }
      rol = usuario.rol || rolFallback();
    } else {
      // No existe en usuarios (o hash inválido) -> fallback al sistema anterior
      rol = rolFallback();
      passwordOk = password === passwordFallback[rol];
      if (!passwordOk) {
        return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
      }
    }

    // Generar token
    const token = jwt.sign(
      { 
        id: persona.id, 
        email: persona.correo, 
        rol: rol 
      },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: '7d' }
    );

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
          access: token,
          refresh: token
        }
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error en el login',
      error: error.message
    });
  }
});

// Register
router.post('/register', async (req, res) => {
  try {
    const { email, password, nombres, apellidos, telefono, dni } = req.body;

    if (!email || !password || !nombres || !apellidos) {
      return res.status(400).json({
        success: false,
        message: 'Todos los campos son requeridos'
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
    const hashedPassword = await bcrypt.hash(password, 10);

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
      message: 'Error al registrar usuario',
      error: error.message
    });
  }
});

export default router;
