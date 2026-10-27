import { Router } from 'express';
import bcrypt from 'bcryptjs';
import { personaHasStudentLoginCredentials } from '../services/student-login';
import supabase from '../config/database';
import { AuthRequest, authMiddleware, signAccessToken } from '../middleware/auth';
import { createRateLimiter } from '../middleware/rate-limit';
import { hashAccountActivationToken, resolveAccountRole } from '../services/account-activation';
import { isPasswordChangeValid } from '../services/password-change';

const router = Router();
const loginRateLimit = createRateLimiter(15 * 60 * 1000, 10, req => {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : 'invalid';
  return `${req.ip || 'unknown'}:${email}`;
});
const activationRateLimit = createRateLimiter(15 * 60 * 1000, 5, req => {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : 'invalid';
  return `${req.ip || 'unknown'}:${email}`;
});
const passwordChangeRateLimit = createRateLimiter(15 * 60 * 1000, 5, req => {
  const user = (req as AuthRequest).user;
  return `${req.ip || 'unknown'}:${user?.id || 'unauthenticated'}`;
});

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
      .eq('email', email)
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

      if (result.error || !result.data || !personaHasStudentLoginCredentials(result.data, email, password)) {
        return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
      }
      persona = result.data;
      rol = 'alumno';
    }

    const token = signAccessToken({ id: persona.id, email: persona.correo, rol });

    // Obtener sección del alumno
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

router.post('/change-password', authMiddleware, passwordChangeRateLimit, async (req, res) => {
  try {
    const currentPassword = typeof req.body?.current_password === 'string' ? req.body.current_password : '';
    const newPassword = typeof req.body?.new_password === 'string' ? req.body.new_password : '';

    if (!isPasswordChangeValid(currentPassword, newPassword)) {
      return res.status(400).json({ success: false, message: 'La nueva contraseña debe tener al menos 12 caracteres y ser distinta a la actual.' });
    }

    const userId = (req as AuthRequest).user?.id;
    const { data: user, error: userError } = await supabase
      .from('usuarios')
      .select('id, password, activo')
      .eq('persona_id', userId)
      .maybeSingle();

    if (userError) throw userError;
    const validHash = typeof user?.password === 'string' && /^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$/.test(user.password);
    if (!user || user.activo === false || !validHash || !(await bcrypt.compare(currentPassword, user.password))) {
      return res.status(400).json({ success: false, message: 'No se pudo validar la contraseña actual.' });
    }

    const passwordHash = await bcrypt.hash(newPassword, 12);
    const { data: updated, error: updateError } = await supabase
      .from('usuarios')
      .update({ password: passwordHash, actualizado_en: new Date().toISOString() })
      .eq('id', user.id)
      .select('id')
      .maybeSingle();

    if (updateError) throw updateError;
    if (!updated) return res.status(500).json({ success: false, message: 'No se pudo actualizar la contraseña.' });

    return res.json({ success: true, message: 'Contraseña actualizada.' });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo cambiar la contraseña.' });
  }
});

router.post('/activate-account', activationRateLimit, async (req, res) => {
  try {
    const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    const password = typeof req.body?.password === 'string' ? req.body.password : '';
    const activationCode = typeof req.body?.activation_code === 'string' ? req.body.activation_code.trim() : '';

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254 || password.length < 12 || password.length > 256 || !/^[A-Za-z0-9_-]{43}$/.test(activationCode)) {
      return res.status(400).json({ success: false, message: 'Datos de activación inválidos' });
    }

    const { data: activation, error: activationError } = await supabase
      .from('account_activation_tokens')
      .select('id, persona_id, expires_at, consumed_at, revoked_at')
      .eq('token_hash', hashAccountActivationToken(activationCode))
      .maybeSingle();

    if (activationError) throw activationError;
    if (!activation || activation.consumed_at || activation.revoked_at || Date.parse(activation.expires_at) <= Date.now()) {
      return res.status(400).json({ success: false, message: 'El código no es válido o venció' });
    }

    const { data: persona, error: personaError } = await supabase
      .from('personas')
      .select('id, correo, alumnos(id, estado), docentes(id, estado)')
      .eq('id', activation.persona_id)
      .maybeSingle();

    if (personaError) throw personaError;
    if (!persona || String(persona.correo || '').trim().toLowerCase() !== email) {
      return res.status(400).json({ success: false, message: 'El código no es válido o venció' });
    }

    const students = Array.isArray(persona.alumnos) ? persona.alumnos : [];
    const teachers = Array.isArray(persona.docentes) ? persona.docentes : [];
    const role = resolveAccountRole(
      students.some((student: any) => student.estado === 'activo'),
      teachers.some((teacher: any) => teacher.estado === 'activo')
    );

    if (!role) return res.status(400).json({ success: false, message: 'El código no es válido o venció' });

    const [{ data: accountByPersona, error: accountByPersonaError }, { data: accountByEmail, error: accountByEmailError }] = await Promise.all([
      supabase.from('usuarios').select('id, persona_id').eq('persona_id', persona.id).maybeSingle(),
      supabase.from('usuarios').select('id, persona_id').eq('email', email).maybeSingle()
    ]);

    if (accountByPersonaError) throw accountByPersonaError;
    if (accountByEmailError) throw accountByEmailError;
    if (accountByEmail && accountByEmail.persona_id !== persona.id) {
      return res.status(400).json({ success: false, message: 'El código no es válido o venció' });
    }

    const passwordHash = await bcrypt.hash(password, 12);
    const now = new Date().toISOString();
    const { data: consumed, error: consumeError } = await supabase
      .from('account_activation_tokens')
      .update({ consumed_at: now })
      .eq('id', activation.id)
      .is('consumed_at', null)
      .is('revoked_at', null)
      .gt('expires_at', now)
      .select('id')
      .maybeSingle();

    if (consumeError) throw consumeError;
    if (!consumed) return res.status(400).json({ success: false, message: 'El código no es válido o venció' });

    const account = accountByPersona || accountByEmail;
    const accountQuery = account
      ? supabase.from('usuarios').update({ email, password: passwordHash, rol: role, activo: true, actualizado_en: now }).eq('id', account.id)
      : supabase.from('usuarios').insert({ persona_id: persona.id, email, password: passwordHash, rol: role, activo: true });
    const { error: saveError } = await accountQuery;

    if (saveError) throw saveError;

    return res.json({ success: true, message: 'Cuenta activada. Ya puedes iniciar sesión con tu nueva contraseña.' });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo activar la cuenta. Solicita un nuevo código.' });
  }
});

router.post('/register', (_req, res) => {
  res.status(410).json({ success: false, message: 'El registro requiere un código de activación emitido por la institución.' });
});

export default router;
