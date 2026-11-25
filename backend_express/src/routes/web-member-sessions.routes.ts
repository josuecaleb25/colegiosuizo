import { Router } from 'express';
import bcrypt from 'bcryptjs';
import supabase from '../config/database';
import { signAccessToken } from '../middleware/auth';
import { createRateLimiter } from '../middleware/rate-limit';

const router = Router();
const webSessionRateLimit = createRateLimiter(15 * 60 * 1000, 10, req => {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : 'invalid';
  return `${req.ip || 'unknown'}:${email}`;
});
const eligibleWebAccountRoles = new Set(['administrador', 'admin', 'profesor']);

router.post('/sessions', webSessionRateLimit, async (req, res) => {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
  const password = typeof req.body?.password === 'string' ? req.body.password : '';
  if (!email || !password || email.length > 254 || password.length > 256 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ success: false, message: 'Credenciales inválidas' });
  }

  try {
    const { data: account, error: accountError } = await supabase
      .from('usuarios')
      .select('id, persona_id, email, rol, activo, password, session_version')
      .eq('email', email)
      .maybeSingle();
    if (accountError) throw accountError;
    if (!account || account.activo === false) return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });

    const validHash = typeof account.password === 'string' && /^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$/.test(account.password);
    if (!validHash || !account.persona_id || !account.rol || !await bcrypt.compare(password, account.password)) {
      return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });
    }

    if (!eligibleWebAccountRoles.has(String(account.rol || '').toLowerCase())) {
      return res.status(403).json({ success: false, message: 'Esta cuenta no tiene acceso al panel web' });
    }

    const { data: member, error: memberError } = await supabase
      .from('web_members')
      .select('id, active, activated_at, web_roles!inner(id, role_key, name, active)')
      .eq('usuario_id', account.id)
      .maybeSingle();
    if (memberError) throw memberError;
    const webRole = Array.isArray(member?.web_roles) ? member.web_roles[0] : member?.web_roles;
    if (!member || member.active !== true || !member.activated_at || webRole?.active !== true) {
      return res.status(403).json({ success: false, message: 'Esta cuenta no tiene acceso al panel web' });
    }

    const { data: person, error: personError } = await supabase
      .from('personas')
      .select('id, nombres, apellidos, correo')
      .eq('id', account.persona_id)
      .maybeSingle();
    if (personError) throw personError;
    if (!person) return res.status(401).json({ success: false, message: 'Credenciales incorrectas' });

    const accessToken = signAccessToken({
      id: person.id,
      email: account.email,
      rol: account.rol,
      sessionVersion: account.session_version ?? 0,
      context: 'web'
    });
    const nombreCompleto = `${person.nombres || ''} ${person.apellidos || ''}`.trim();
    return res.json({
      success: true,
      message: 'Inicio de sesión del panel web exitoso',
      data: {
        user: {
          id: person.id,
          email: account.email,
          nombres: person.nombres || '',
          apellidos: person.apellidos || '',
          rol: account.rol,
          nombre_completo: nombreCompleto,
          access_context: 'web'
        },
        tokens: { access: accessToken }
      }
    });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo iniciar sesión en el panel web' });
  }
});

export default router;
