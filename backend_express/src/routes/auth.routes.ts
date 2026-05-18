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
        alumnos (id, codigo_alumno, estado),
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

    // Determinar rol y contraseña esperada
    let rol = 'padre';
    let passwordEsperada = 'Suizo2026*';

    // Verificar si es admin
    if (email === 'admin@colegio.com') {
      rol = 'administrador';
      passwordEsperada = 'admin123';
    }
    // Verificar si es profesor
    else if (email === 'profesor@colegio.com' || (persona.docentes && persona.docentes.length > 0)) {
      rol = 'profesor';
      passwordEsperada = 'profesor123';
    }
    // Si tiene alumno asociado, es un padre (no alumno)
    else if (persona.alumnos && persona.alumnos.length > 0) {
      rol = 'padre';
      passwordEsperada = 'Suizo2026*';
    }

    // Verificar contraseña (comparación directa por ahora)
    if (password !== passwordEsperada) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales incorrectas'
      });
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
          nombre_completo: `${persona.nombres} ${persona.apellidos}`
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
