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

    // Buscar usuario en Supabase
    const { data: users, error } = await supabase
      .from('usuarios')
      .select(`
        *,
        personas!inner (
          nombres,
          apellidos
        )
      `)
      .eq('email', email)
      .eq('activo', true)
      .limit(1);

    if (error || !users || users.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales incorrectas'
      });
    }

    const user = users[0];

    // Verificar contraseña
    const isValidPassword = await bcrypt.compare(password, user.password);

    if (!isValidPassword) {
      return res.status(401).json({
        success: false,
        message: 'Credenciales incorrectas'
      });
    }

    // Generar token
    const token = jwt.sign(
      { 
        id: user.id, 
        email: user.email, 
        rol: user.rol 
      },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    res.json({
      success: true,
      message: 'Login exitoso',
      data: {
        user: {
          id: user.id,
          email: user.email,
          nombres: user.personas.nombres,
          apellidos: user.personas.apellidos,
          rol: user.rol,
          nombre_completo: `${user.personas.nombres} ${user.personas.apellidos}`
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
