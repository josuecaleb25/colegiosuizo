import { Router } from 'express';
import bcrypt from 'bcryptjs';
import supabase from '../config/database';
import QRCode from 'qrcode';
import { AuthRequest, authMiddleware, requireRoles } from '../middleware/auth';
import { createRateLimiter } from '../middleware/rate-limit';
import { createAccountActivationToken, resolveAccountRole } from '../services/account-activation';

const router = Router();
const qrImageCache = new Map<string, Promise<string>>();
const qrImageCacheLimit = 1000;

const normalizeLabel = (value: unknown) => String(value || '')
  .toLowerCase()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/[\s-]+/g, '');

const getQrImage = (code: string) => {
  const cached = qrImageCache.get(code);
  if (cached) return cached;

  const imagePromise = QRCode.toDataURL(code, {
    width: 300,
    margin: 2,
    color: {
      dark: '#000000',
      light: '#FFFFFF'
    }
  }).then(image => {
    if (qrImageCache.size >= qrImageCacheLimit) {
      const oldestCode = qrImageCache.keys().next().value;
      if (oldestCode) qrImageCache.delete(oldestCode);
    }
    return image;
  }).catch(error => {
    qrImageCache.delete(code);
    throw error;
  });

  qrImageCache.set(code, imagePromise);
  return imagePromise;
};

router.use(authMiddleware, requireRoles('administrador', 'admin'));

const activationCodeRateLimit = createRateLimiter(15 * 60 * 1000, 20);

router.post('/accounts/activation-code', activationCodeRateLimit, async (req, res) => {
  try {
    const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254) {
      return res.status(400).json({ success: false, message: 'Correo inválido' });
    }

    const { data: persona, error: personaError } = await supabase
      .from('personas')
      .select('id, correo')
      .eq('correo', email)
      .maybeSingle();

    if (personaError) throw personaError;
    if (!persona?.correo || String(persona.correo).trim().toLowerCase() !== email) {
      return res.status(404).json({ success: false, message: 'No se encontró la persona' });
    }

    const [{ data: student, error: studentError }, { data: teacher, error: teacherError }] = await Promise.all([
      supabase.from('alumnos').select('id, estado').eq('persona_id', persona.id).maybeSingle(),
      supabase.from('docentes').select('id, estado').eq('persona_id', persona.id).maybeSingle()
    ]);

    if (studentError) throw studentError;
    if (teacherError) throw teacherError;

    const role = resolveAccountRole(student?.estado === 'activo', teacher?.estado === 'activo');
    if (!role) {
      return res.status(409).json({ success: false, message: 'La persona no tiene un único rol activo de alumno o profesor' });
    }

    const { data: accountByEmail, error: accountByEmailError } = await supabase
      .from('usuarios')
      .select('id, persona_id')
      .eq('email', persona.correo)
      .maybeSingle();

    if (accountByEmailError) throw accountByEmailError;
    if (accountByEmail && accountByEmail.persona_id !== persona.id) {
      return res.status(409).json({ success: false, message: 'El correo ya pertenece a otra cuenta' });
    }

    const now = new Date().toISOString();
    const { error: revokeError } = await supabase
      .from('account_activation_tokens')
      .update({ revoked_at: now })
      .eq('persona_id', persona.id)
      .is('consumed_at', null)
      .is('revoked_at', null);

    if (revokeError) throw revokeError;

    const { token, tokenHash } = createAccountActivationToken();
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const { error: insertError } = await supabase
      .from('account_activation_tokens')
      .insert({
        persona_id: persona.id,
        token_hash: tokenHash,
        expires_at: expiresAt,
        created_by: (req as AuthRequest).user?.id
      });

    if (insertError) throw insertError;

    return res.status(201).json({
      success: true,
      message: 'Código de activación generado. Solo se mostrará en esta respuesta.',
      data: {
        email: persona.correo,
        rol: role,
        activation_code: token,
        expires_at: expiresAt
      }
    });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo generar el código de activación' });
  }
});

router.get('/alumnos', async (req, res) => {
  try {
    const { seccion, search, limit = 100 } = req.query;

    const limitNum = Math.min(Math.max(Number(limit) || 100, 1), 500);
    const { data: seccionesActivas, error: seccionesError } = await supabase
      .from('secciones')
      .select('id, nombre, grados!inner(nombre), anios_lectivos!inner(activo)')
      .eq('anios_lectivos.activo', true);

    if (seccionesError) throw seccionesError;

    const seccionSolicitada = normalizeLabel(seccion);
    const seccionIds = (seccionesActivas || [])
      .filter((item: any) => {
        if (!seccionSolicitada) return true;
        const grado = Array.isArray(item.grados) ? item.grados[0] : item.grados;
        return normalizeLabel(`${grado?.nombre || ''} ${item.nombre}`) === seccionSolicitada;
      })
      .map((item: any) => item.id);

    if (!seccionIds.length) {
      return res.json({ success: true, data: [], total: 0 });
    }

    const alumnosFiltrados: any[] = [];
    const pageSize = search ? 1000 : limitNum;
    const searchLower = String(search || '').trim().toLowerCase();

    for (let offset = 0; ; offset += pageSize) {
      let query = supabase
        .from('alumnos')
        .select(`
          id,
          codigo_alumno,
          estado,
          persona_id,
          personas!inner (
            id,
            nombres,
            apellidos,
            dni,
            telefono,
            correo
          ),
          matriculas!inner (
            id,
            anio_lectivo_id,
            seccion_id,
            secciones!inner (
              id,
              nombre,
              grados!inner (
                nombre
              )
            )
          )
        `)
        .eq('estado', 'activo')
        .eq('matriculas.estado', 'activo')
        .in('matriculas.seccion_id', seccionIds)
        .order('apellidos', { ascending: true, referencedTable: 'personas' })
        .order('nombres', { ascending: true, referencedTable: 'personas' })
        .range(offset, offset + pageSize - 1);

      const { data: alumnos, error } = await query;
      if (error) throw error;
      if (!alumnos?.length) break;

      for (const alumno of alumnos as any[]) {
        if (searchLower) {
          const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
          const matriculas = Array.isArray(alumno.matriculas) ? alumno.matriculas : [alumno.matriculas];
          const matriculaActiva = matriculas[0];
          const secciones = Array.isArray(matriculaActiva?.secciones) ? matriculaActiva.secciones[0] : matriculaActiva?.secciones;
          const grados = Array.isArray(secciones?.grados) ? secciones.grados[0] : secciones?.grados;
          const seccionNombre = secciones && grados ? `${grados.nombre} ${secciones.nombre}` : 'Sin sección';
          const nombreCompleto = `${persona?.nombres || ''} ${persona?.apellidos || ''}`.toLowerCase();
          const coincide = nombreCompleto.includes(searchLower) ||
            String(alumno.codigo_alumno || '').toLowerCase().includes(searchLower) ||
            seccionNombre.toLowerCase().includes(searchLower) ||
            String(persona?.dni || '').toLowerCase().includes(searchLower);
          if (!coincide) continue;
        }

        alumnosFiltrados.push(alumno);
        if (alumnosFiltrados.length >= limitNum) break;
      }

      if (!searchLower || alumnosFiltrados.length >= limitNum || alumnos.length < pageSize) break;
    }

    const alumnosPaginados = alumnosFiltrados.slice(0, limitNum);
    const personaIds = alumnosPaginados
      .map((alumno: any) => Array.isArray(alumno.personas) ? alumno.personas[0]?.id : alumno.personas?.id)
      .filter(Boolean);
    const codigosQrPorPersona = new Map<string, string>();

    if (personaIds.length > 0) {
      const { data: codigosQr, error: qrError } = await supabase
        .from('codigos_qr')
        .select('persona_id, codigo, generado_en')
        .in('persona_id', personaIds)
        .eq('activo', true)
        .order('generado_en', { ascending: false });

      if (qrError) throw qrError;
      for (const codigoQr of codigosQr || []) {
        if (!codigosQrPorPersona.has(codigoQr.persona_id)) {
          codigosQrPorPersona.set(codigoQr.persona_id, codigoQr.codigo);
        }
      }
    }

    const alumnosFormateados = await Promise.all(alumnosPaginados.map(async (alumno: any) => {
      const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
      const matriculas = Array.isArray(alumno.matriculas) ? alumno.matriculas : [alumno.matriculas];
      const matriculaActiva = matriculas[0];
      const secciones = Array.isArray(matriculaActiva?.secciones) ? matriculaActiva.secciones[0] : matriculaActiva?.secciones;
      const grados = Array.isArray(secciones?.grados) ? secciones.grados[0] : secciones?.grados;
      
      const seccionNombre = secciones && grados ? `${grados.nombre} ${secciones.nombre}` : 'Sin sección';

      const qrCodeString = codigosQrPorPersona.get(persona.id) || null;
      
      let qrImage = '';
      if (qrCodeString) {
        try {
          qrImage = await getQrImage(qrCodeString);
        } catch (qrError) {
          console.error('QR generation failed:', qrError);
        }
      }

      return {
        id: alumno.id,
        codigo_alumno: alumno.codigo_alumno,
        nombres: persona.nombres,
        apellidos: persona.apellidos,
        nombre_completo: `${persona.nombres} ${persona.apellidos}`,
        dni: persona.dni,
        telefono: persona.telefono,
        email: persona.correo,
        seccion: seccionNombre,
        seccion_id: matriculaActiva?.seccion_id || null,
        estado: alumno.estado,
        qr_code: qrCodeString,
        qr_image: qrImage
      };
    }));

    res.json({
      success: true,
      data: alumnosFormateados,
      total: alumnosFormateados.length
    });
  } catch (error: any) {
    console.error('Admin student list failed:', error.message);
    res.status(500).json({
      success: false,
      message: 'Error al obtener alumnos',
      error: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

router.post('/alumnos', async (req, res) => {
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  const allowed = new Set(['dni','nombres','apellidos','telefono','correo','seccion_id','estado']);
  if (Object.keys(body).some(key => !allowed.has(key))) return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  const dni = typeof body.dni === 'string' ? body.dni.trim() : '';
  const nombres = typeof body.nombres === 'string' ? body.nombres.trim() : '';
  const apellidos = typeof body.apellidos === 'string' ? body.apellidos.trim() : '';
  const telefono = typeof body.telefono === 'string' ? body.telefono.trim() : '';
  const correo = typeof body.correo === 'string' ? body.correo.trim().toLowerCase() : '';
  const seccionId = typeof body.seccion_id === 'string' ? body.seccion_id : '';
  const estado = typeof body.estado === 'string' ? body.estado.trim().toLowerCase() : 'activo';
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!/^\d{8}$/.test(dni)) return res.status(400).json({ success: false, message: 'El DNI debe tener 8 dígitos' });
  if (!nombres || nombres.length > 120 || !apellidos || apellidos.length > 120) return res.status(400).json({ success: false, message: 'Nombres y apellidos son obligatorios y deben tener hasta 120 caracteres' });
  if (telefono.length > 20 || (telefono && !/^[0-9+()\s-]+$/.test(telefono))) return res.status(400).json({ success: false, message: 'El teléfono no tiene un formato válido' });
  if (correo && (correo.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo))) return res.status(400).json({ success: false, message: 'El correo no tiene un formato válido' });
  if (!uuidPattern.test(seccionId) || !['activo','inactivo'].includes(estado)) return res.status(400).json({ success: false, message: 'Selecciona una sección y estado válidos' });
  let personaId = '';
  let alumnoId = '';
  let matriculaId = '';
  let qrCode = '';
  let qrCreated = false;
  try {
    const [{ data: duplicateDni, error: dniError }, { data: duplicateEmail, error: emailError }, { data: year, error: yearError }] = await Promise.all([
      supabase.from('personas').select('id').eq('dni', dni).maybeSingle(),
      correo ? supabase.from('personas').select('id').eq('correo', correo).maybeSingle() : Promise.resolve({ data: null, error: null }),
      supabase.from('anios_lectivos').select('id').eq('activo', true).maybeSingle()
    ]);
    if (dniError) throw dniError;
    if (emailError) throw emailError;
    if (yearError) throw yearError;
    if (duplicateDni) return res.status(409).json({ success: false, message: 'El DNI ya está registrado' });
    if (duplicateEmail) return res.status(409).json({ success: false, message: 'El correo ya está registrado' });
    if (!year) return res.status(409).json({ success: false, message: 'No hay un año lectivo activo' });
    const { data: section, error: sectionError } = await supabase.from('secciones').select('id,nombre,grados!inner(nombre)').eq('id', seccionId).eq('anio_lectivo_id', year.id).maybeSingle();
    if (sectionError) throw sectionError;
    if (!section) return res.status(400).json({ success: false, message: 'La sección no pertenece al año lectivo activo' });
    const grade = Array.isArray(section.grados) ? section.grados[0] : section.grados;
    const gradeNumber = String(grade?.nombre || '').match(/\d+/)?.[0];
    const letter = String(section.nombre || '').match(/[A-E]$/i)?.[0]?.toUpperCase();
    if (!gradeNumber || !letter) return res.status(409).json({ success: false, message: 'No se pudo generar el código para esta sección' });
    const prefix = gradeNumber + letter;
    const { data: codes, error: codeError } = await supabase.from('alumnos').select('codigo_alumno').like('codigo_alumno', prefix + '%').limit(1000);
    if (codeError) throw codeError;
    const nextCode = (codes || []).reduce((max: number, row: any) => Math.max(max, Number(String(row.codigo_alumno || '').match(/(\d+)$/)?.[1]) || 0), 0) + 1;
    const codigoAlumno = prefix + String(nextCode).padStart(3, '0');
    const today = new Date().toISOString().slice(0, 10);
    const { data: person, error: personError } = await supabase.from('personas').insert({ dni, nombres, apellidos, telefono: telefono || null, correo: correo || null }).select('id').single();
    if (personError) throw personError;
    personaId = person.id;
    const { data: student, error: studentError } = await supabase.from('alumnos').insert({ persona_id: personaId, codigo_alumno: codigoAlumno, fecha_ingreso: today, estado }).select('id').single();
    if (studentError) throw studentError;
    alumnoId = student.id;
    const { data: enrollment, error: enrollmentError } = await supabase.from('matriculas').insert({ alumno_id: alumnoId, seccion_id: seccionId, anio_lectivo_id: year.id, fecha_matricula: today, estado: estado === 'activo' ? 'activo' : 'retirado' }).select('id').single();
    if (enrollmentError) throw enrollmentError;
    matriculaId = enrollment.id;
    let qrImage = '';
    if (estado === 'activo') {
      qrCode = 'QR' + codigoAlumno;
      const { error: qrError } = await supabase.from('codigos_qr').insert({ persona_id: personaId, codigo: qrCode, activo: true });
      if (qrError) throw qrError;
      qrCreated = true;
      qrImage = await getQrImage(qrCode);
    }
    return res.status(201).json({ success: true, message: 'Estudiante creado', data: {
      id: alumnoId, codigo_alumno: codigoAlumno, nombres, apellidos, nombre_completo: (nombres + ' ' + apellidos).trim(),
      dni, telefono, email: correo, seccion: (grade?.nombre + ' ' + section.nombre).trim(), seccion_id: seccionId,
      estado, qr_code: qrCode || null, qr_image: qrImage
    } });
  } catch (error: any) {
    if (qrCreated && personaId) await supabase.from('codigos_qr').delete().eq('persona_id', personaId).eq('codigo', qrCode);
    if (matriculaId) await supabase.from('matriculas').delete().eq('id', matriculaId);
    if (alumnoId) await supabase.from('alumnos').delete().eq('id', alumnoId);
    if (personaId) await supabase.from('personas').delete().eq('id', personaId);
    console.error('Admin student creation failed:', error);
    if (error?.code === '23505') return res.status(409).json({ success: false, message: 'El DNI, correo o código ya está registrado' });
    return res.status(500).json({ success: false, message: 'No se pudo crear al estudiante' });
  }
});

router.get('/docentes', async (req, res) => {
  try {
    const limit = Math.min(Math.max(Number(req.query.limit) || 100, 1), 500);
    const { data: teachers, error } = await supabase.from('docentes')
      .select('id,codigo_docente,especialidad,estado,personas!inner(id,dni,nombres,apellidos,correo,usuarios(id,email,rol,activo))')
      .order('apellidos', { ascending: true, referencedTable: 'personas' }).order('nombres', { ascending: true, referencedTable: 'personas' }).limit(limit);
    if (error) throw error;
    const data = (teachers || []).map((teacher: any) => {
      const person = Array.isArray(teacher.personas) ? teacher.personas[0] : teacher.personas;
      const accounts = Array.isArray(person?.usuarios) ? person.usuarios : person?.usuarios ? [person.usuarios] : [];
      const account = accounts.find((item: any) => item.rol === 'profesor') || accounts[0];
      return { id: teacher.id, usuario_id: account?.id || null, dni: person?.dni || '', nombres: person?.nombres || '', apellidos: person?.apellidos || '', nombre_completo: ((person?.nombres || '') + ' ' + (person?.apellidos || '')).trim(), email: account?.email || person?.correo || '', cargo: teacher.especialidad || 'Profesor', activo: teacher.estado === 'activo' && (account ? account.activo !== false : true), codigo_docente: teacher.codigo_docente };
    });
    return res.json({ success: true, data, total: data.length });
  } catch (error: any) {
    console.error('Admin teacher list failed:', error);
    return res.status(500).json({ success: false, message: 'No se pudieron cargar los docentes' });
  }
});

router.post('/docentes', async (req, res) => {
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  const allowed = new Set(['dni','nombres','apellidos','telefono','correo','password','cargo']);
  if (Object.keys(body).some(key => !allowed.has(key))) return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  const dni = typeof body.dni === 'string' ? body.dni.trim() : '';
  const nombres = typeof body.nombres === 'string' ? body.nombres.trim() : '';
  const apellidos = typeof body.apellidos === 'string' ? body.apellidos.trim() : '';
  const telefono = typeof body.telefono === 'string' ? body.telefono.trim() : '';
  const correo = typeof body.correo === 'string' ? body.correo.trim().toLowerCase() : '';
  const password = typeof body.password === 'string' ? body.password : '';
  const cargo = typeof body.cargo === 'string' ? body.cargo.trim() : '';
  if (!/^\d{8}$/.test(dni) || !nombres || nombres.length > 120 || !apellidos || apellidos.length > 120) return res.status(400).json({ success: false, message: 'DNI, nombres y apellidos son obligatorios y deben ser válidos' });
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo) || correo.length > 254) return res.status(400).json({ success: false, message: 'Ingresa un correo institucional válido' });
  if (password.length < 12 || password.length > 256) return res.status(400).json({ success: false, message: 'La contraseña debe tener entre 12 y 256 caracteres' });
  if (!cargo || cargo.length > 100 || telefono.length > 20 || (telefono && !/^[0-9+()\s-]+$/.test(telefono))) return res.status(400).json({ success: false, message: 'Cargo o teléfono inválido' });
  let personaId = '';
  let docenteId = '';
  try {
    const [{ data: duplicateDni, error: dniError }, { data: duplicateEmail, error: emailError }, { data: duplicateAccount, error: accountError }] = await Promise.all([
      supabase.from('personas').select('id').eq('dni', dni).maybeSingle(),
      supabase.from('personas').select('id').eq('correo', correo).maybeSingle(),
      supabase.from('usuarios').select('id').eq('email', correo).maybeSingle()
    ]);
    if (dniError) throw dniError;
    if (emailError) throw emailError;
    if (accountError) throw accountError;
    if (duplicateDni) return res.status(409).json({ success: false, message: 'El DNI ya está registrado' });
    if (duplicateEmail || duplicateAccount) return res.status(409).json({ success: false, message: 'El correo ya está registrado' });
    const { data: person, error: personError } = await supabase.from('personas').insert({ dni, nombres, apellidos, telefono: telefono || null, correo }).select('id').single();
    if (personError) throw personError;
    personaId = person.id;
    const codigoDocente = 'DOC' + dni.slice(4);
    const { data: teacher, error: teacherError } = await supabase.from('docentes').insert({ persona_id: personaId, codigo_docente: codigoDocente, especialidad: cargo, estado: 'activo' }).select('id').single();
    if (teacherError) throw teacherError;
    docenteId = teacher.id;
    const passwordHash = await bcrypt.hash(password, 12);
    const { error: userError } = await supabase.from('usuarios').insert({ persona_id: personaId, email: correo, password: passwordHash, rol: 'profesor', activo: true });
    if (userError) throw userError;
    return res.status(201).json({ success: true, message: 'Docente creado', data: { id: docenteId, usuario_id: null, dni, nombres, apellidos, nombre_completo: (nombres + ' ' + apellidos).trim(), email: correo, cargo, activo: true, codigo_docente: codigoDocente } });
  } catch (error: any) {
    if (docenteId) await supabase.from('docentes').delete().eq('id', docenteId);
    if (personaId) {
      await supabase.from('usuarios').delete().eq('persona_id', personaId);
      await supabase.from('personas').delete().eq('id', personaId);
    }
    console.error('Admin teacher creation failed:', error);
    if (error?.code === '23505') return res.status(409).json({ success: false, message: 'El DNI, correo o código ya está registrado' });
    return res.status(500).json({ success: false, message: 'No se pudo crear al docente' });
  }
});

router.patch('/docentes/:id', async (req, res) => {
  const id = String(req.params.id || '');
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!uuidPattern.test(id)) return res.status(400).json({ success: false, message: 'Identificador de docente inválido' });
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  const allowed = new Set(['dni','nombres','apellidos','telefono','correo','cargo','password']);
  if (Object.keys(body).some(key => !allowed.has(key))) return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  const dni = typeof body.dni === 'string' ? body.dni.trim() : '';
  const nombres = typeof body.nombres === 'string' ? body.nombres.trim() : '';
  const apellidos = typeof body.apellidos === 'string' ? body.apellidos.trim() : '';
  const telefono = typeof body.telefono === 'string' ? body.telefono.trim() : '';
  const correo = typeof body.correo === 'string' ? body.correo.trim().toLowerCase() : '';
  const cargo = typeof body.cargo === 'string' ? body.cargo.trim() : '';
  const password = typeof body.password === 'string' ? body.password : '';
  if (!/^\d{8}$/.test(dni) || !nombres || nombres.length > 120 || !apellidos || apellidos.length > 120) return res.status(400).json({ success: false, message: 'DNI, nombres y apellidos son obligatorios y deben ser válidos' });
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo) || correo.length > 254 || !cargo || cargo.length > 100) return res.status(400).json({ success: false, message: 'Correo y cargo son obligatorios y deben ser válidos' });
  if (telefono.length > 20 || (telefono && !/^[0-9+()\s-]+$/.test(telefono)) || (password && (password.length < 12 || password.length > 256))) return res.status(400).json({ success: false, message: 'Teléfono o contraseña inválidos' });
  try {
    const { data: teacher, error: teacherError } = await supabase.from('docentes').select('id,persona_id,especialidad').eq('id', id).maybeSingle();
    if (teacherError) throw teacherError;
    if (!teacher) return res.status(404).json({ success: false, message: 'Docente no encontrado' });
    const [{ data: person, error: personError }, { data: accounts, error: accountsError }, { data: duplicateDni, error: duplicateDniError }, { data: duplicateEmail, error: duplicateEmailError }] = await Promise.all([
      supabase.from('personas').select('id,dni,nombres,apellidos,telefono,correo').eq('id', teacher.persona_id).maybeSingle(),
      supabase.from('usuarios').select('id,email,password').eq('persona_id', teacher.persona_id),
      supabase.from('personas').select('id').eq('dni', dni).neq('id', teacher.persona_id).maybeSingle(),
      supabase.from('personas').select('id').eq('correo', correo).neq('id', teacher.persona_id).maybeSingle()
    ]);
    if (personError) throw personError;
    if (accountsError) throw accountsError;
    if (duplicateDniError) throw duplicateDniError;
    if (duplicateEmailError) throw duplicateEmailError;
    if (!person) return res.status(404).json({ success: false, message: 'No se encontró la información del docente' });
    if (duplicateDni || duplicateEmail) return res.status(409).json({ success: false, message: 'El DNI o correo ya está registrado' });
    if (!accounts?.length) return res.status(409).json({ success: false, message: 'El docente no tiene una cuenta vinculada' });
    const oldPerson = { dni: person.dni, nombres: person.nombres, apellidos: person.apellidos, telefono: person.telefono, correo: person.correo };
    let personUpdated = false;
    let teacherUpdated = false;
    let accountUpdated = false;
    try {
      const { error } = await supabase.from('personas').update({ dni, nombres, apellidos, telefono: telefono || null, correo, actualizado_en: new Date().toISOString() }).eq('id', teacher.persona_id);
      if (error) throw error;
      personUpdated = true;
      const { error: specializationError } = await supabase.from('docentes').update({ especialidad: cargo, actualizado_en: new Date().toISOString() }).eq('id', id);
      if (specializationError) throw specializationError;
      teacherUpdated = true;
      const accountChanges: any = { email: correo, actualizado_en: new Date().toISOString() };
      if (password) accountChanges.password = await bcrypt.hash(password, 12);
      const { error: accountUpdateError } = await supabase.from('usuarios').update(accountChanges).eq('persona_id', teacher.persona_id);
      if (accountUpdateError) throw accountUpdateError;
      accountUpdated = true;
    } catch (error: any) {
      if (accountUpdated) await supabase.from('usuarios').update({ email: accounts[0].email, password: accounts[0].password }).eq('persona_id', teacher.persona_id);
      if (teacherUpdated) await supabase.from('docentes').update({ especialidad: teacher.especialidad }).eq('id', id);
      if (personUpdated) await supabase.from('personas').update(oldPerson).eq('id', teacher.persona_id);
      if (error?.code === '23505') return res.status(409).json({ success: false, message: 'El DNI o correo ya está registrado' });
      throw error;
    }
    return res.json({ success: true, message: 'Docente actualizado', data: { id } });
  } catch (error: any) {
    console.error('Admin teacher update failed:', error);
    if (error?.code === '23505') return res.status(409).json({ success: false, message: 'El DNI o correo ya está registrado' });
    return res.status(500).json({ success: false, message: 'No se pudo actualizar al docente' });
  }
});

router.delete('/docentes/:id', async (req, res) => {
  const id = String(req.params.id || '');
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!uuidPattern.test(id)) return res.status(400).json({ success: false, message: 'Identificador de docente inválido' });
  try {
    const { data: teacher, error: teacherError } = await supabase.from('docentes').select('id,persona_id,estado').eq('id', id).maybeSingle();
    if (teacherError) throw teacherError;
    if (!teacher) return res.status(404).json({ success: false, message: 'Docente no encontrado' });
    const { data: accounts, error: accountsError } = await supabase.from('usuarios').select('id,activo').eq('persona_id', teacher.persona_id);
    if (accountsError) throw accountsError;
    const activeAccountIds = (accounts || []).filter((account: any) => account.activo !== false).map((account: any) => account.id);
    const now = new Date().toISOString();
    const { error: accountError } = await supabase.from('usuarios').update({ activo: false, actualizado_en: now }).eq('persona_id', teacher.persona_id);
    if (accountError) throw accountError;
    const { error: teacherUpdateError } = await supabase.from('docentes').update({ estado: 'inactivo', actualizado_en: now }).eq('id', id);
    if (teacherUpdateError) {
      if (activeAccountIds.length) await supabase.from('usuarios').update({ activo: true }).in('id', activeAccountIds);
      throw teacherUpdateError;
    }
    return res.json({ success: true, message: 'Docente desactivado; se conservó el historial', data: { id, activo: false } });
  } catch (error: any) {
    console.error('Admin teacher deactivation failed:', error);
    return res.status(500).json({ success: false, message: 'No se pudo desactivar al docente' });
  }
});


router.patch('/alumnos/:id', async (req, res) => {
  const id = String(req.params.id || '');
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!uuidPattern.test(id)) {
    return res.status(400).json({ success: false, message: 'Identificador de estudiante inválido' });
  }

  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  const allowedFields = new Set(['dni', 'nombres', 'apellidos', 'telefono', 'correo', 'seccion_id']);
  if (Object.keys(body).some(key => !allowedFields.has(key))) {
    return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  }

  const dni = typeof body.dni === 'string' ? body.dni.trim() : '';
  const nombres = typeof body.nombres === 'string' ? body.nombres.trim() : '';
  const apellidos = typeof body.apellidos === 'string' ? body.apellidos.trim() : '';
  const telefono = typeof body.telefono === 'string' ? body.telefono.trim() : '';
  const correo = typeof body.correo === 'string' ? body.correo.trim().toLowerCase() : '';
  const seccionId = typeof body.seccion_id === 'string' ? body.seccion_id : '';

  if (!/^\d{8}$/.test(dni)) {
    return res.status(400).json({ success: false, message: 'El DNI debe tener 8 dígitos' });
  }
  if (!nombres || nombres.length > 120 || !apellidos || apellidos.length > 120) {
    return res.status(400).json({ success: false, message: 'Nombres y apellidos son obligatorios y deben tener hasta 120 caracteres' });
  }
  if (telefono.length > 20 || (telefono && !/^[0-9+()\s-]+$/.test(telefono))) {
    return res.status(400).json({ success: false, message: 'El teléfono no tiene un formato válido' });
  }
  if (correo && (correo.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo))) {
    return res.status(400).json({ success: false, message: 'El correo no tiene un formato válido' });
  }
  if (!uuidPattern.test(seccionId)) {
    return res.status(400).json({ success: false, message: 'Selecciona una sección válida' });
  }

  try {
    const { data: alumno, error: alumnoError } = await supabase
      .from('alumnos')
      .select('id, persona_id, estado')
      .eq('id', id)
      .eq('estado', 'activo')
      .maybeSingle();
    if (alumnoError) throw alumnoError;
    if (!alumno) return res.status(404).json({ success: false, message: 'Estudiante no encontrado o inactivo' });

    const [{ data: anioActivo, error: anioError }, { data: personaAnterior, error: personaError }, { data: usuarios, error: usuariosError }] = await Promise.all([
      supabase.from('anios_lectivos').select('id').eq('activo', true).maybeSingle(),
      supabase.from('personas').select('id, nombres, apellidos, dni, telefono, correo').eq('id', alumno.persona_id).maybeSingle(),
      supabase.from('usuarios').select('id, email, activo').eq('persona_id', alumno.persona_id)
    ]);
    if (anioError) throw anioError;
    if (personaError) throw personaError;
    if (usuariosError) throw usuariosError;
    if (!anioActivo) return res.status(409).json({ success: false, message: 'No hay un año lectivo activo para cambiar de sección' });
    if (!personaAnterior) return res.status(404).json({ success: false, message: 'No se encontró la información personal del estudiante' });
    if ((usuarios || []).length && !correo) {
      return res.status(409).json({ success: false, message: 'No se puede dejar vacío el correo de una cuenta vinculada' });
    }

    const [{ data: seccion, error: seccionError }, { data: matricula, error: matriculaError }] = await Promise.all([
      supabase.from('secciones').select('id').eq('id', seccionId).eq('anio_lectivo_id', anioActivo.id).maybeSingle(),
      supabase.from('matriculas').select('id, seccion_id').eq('alumno_id', id).eq('anio_lectivo_id', anioActivo.id).eq('estado', 'activo').maybeSingle()
    ]);
    if (seccionError) throw seccionError;
    if (matriculaError) throw matriculaError;
    if (!seccion) return res.status(400).json({ success: false, message: 'La sección seleccionada no pertenece al año lectivo activo' });
    if (!matricula) return res.status(409).json({ success: false, message: 'El estudiante no tiene una matrícula activa en el año lectivo actual' });

    const personaActualizada = {
      nombres,
      apellidos,
      dni,
      telefono: telefono || null,
      correo: correo || null,
      actualizado_en: new Date().toISOString()
    };
    const personaAnteriorValues = {
      nombres: personaAnterior.nombres,
      apellidos: personaAnterior.apellidos,
      dni: personaAnterior.dni,
      telefono: personaAnterior.telefono,
      correo: personaAnterior.correo
    };
    let sectionUpdated = false;
    let personUpdated = false;
    let usersUpdated = false;

    try {
      if (matricula.seccion_id !== seccionId) {
        const { error } = await supabase.from('matriculas').update({ seccion_id: seccionId, actualizado_en: new Date().toISOString() }).eq('id', matricula.id);
        if (error) throw error;
        sectionUpdated = true;
      }

      const { error: updatePersonError } = await supabase.from('personas').update(personaActualizada).eq('id', alumno.persona_id);
      if (updatePersonError) throw updatePersonError;
      personUpdated = true;

      if ((usuarios || []).length && correo && usuarios.some((user: any) => user.email !== correo)) {
        const { error: updateUsersError } = await supabase.from('usuarios').update({ email: correo, actualizado_en: new Date().toISOString() }).eq('persona_id', alumno.persona_id);
        if (updateUsersError) throw updateUsersError;
        usersUpdated = true;
      }
    } catch (error: any) {
      if (usersUpdated) {
        await supabase.from('usuarios').update({ email: usuarios?.[0]?.email }).eq('persona_id', alumno.persona_id);
      }
      if (personUpdated) {
        await supabase.from('personas').update(personaAnteriorValues).eq('id', alumno.persona_id);
      }
      if (sectionUpdated) {
        await supabase.from('matriculas').update({ seccion_id: matricula.seccion_id }).eq('id', matricula.id);
      }
      if (error?.code === '23505') {
        return res.status(409).json({ success: false, message: 'El DNI o correo ya está registrado' });
      }
      throw error;
    }

    return res.json({ success: true, message: 'Estudiante actualizado', data: { id } });
  } catch (error: any) {
    console.error('Admin student update failed:', error);
    if (error?.code === '23505') {
      return res.status(409).json({ success: false, message: 'El DNI o correo ya está registrado' });
    }
    return res.status(500).json({ success: false, message: 'No se pudo actualizar al estudiante' });
  }
});

router.delete('/alumnos/:id', async (req, res) => {
  const id = String(req.params.id || '');
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!uuidPattern.test(id)) {
    return res.status(400).json({ success: false, message: 'Identificador de estudiante inválido' });
  }

  try {
    const { data: alumno, error: alumnoError } = await supabase
      .from('alumnos')
      .select('id, persona_id, estado')
      .eq('id', id)
      .eq('estado', 'activo')
      .maybeSingle();
    if (alumnoError) throw alumnoError;
    if (!alumno) return res.status(404).json({ success: false, message: 'Estudiante no encontrado o ya retirado' });

    const [{ data: matriculas, error: matriculasError }, { data: usuarios, error: usuariosError }] = await Promise.all([
      supabase.from('matriculas').select('id, estado').eq('alumno_id', id).eq('estado', 'activo'),
      supabase.from('usuarios').select('id, activo').eq('persona_id', alumno.persona_id)
    ]);
    if (matriculasError) throw matriculasError;
    if (usuariosError) throw usuariosError;

    let accountsDisabled = false;
    let enrollmentsRetired = false;
    try {
      if ((usuarios || []).some((user: any) => user.activo !== false)) {
        const { error } = await supabase.from('usuarios').update({ activo: false, actualizado_en: new Date().toISOString() }).eq('persona_id', alumno.persona_id);
        if (error) throw error;
        accountsDisabled = true;
      }
      if ((matriculas || []).length) {
        const { error } = await supabase.from('matriculas').update({ estado: 'retirado', actualizado_en: new Date().toISOString() }).eq('alumno_id', id).eq('estado', 'activo');
        if (error) throw error;
        enrollmentsRetired = true;
      }
      const { error: disableStudentError } = await supabase.from('alumnos').update({ estado: 'inactivo', actualizado_en: new Date().toISOString() }).eq('id', id).eq('estado', 'activo');
      if (disableStudentError) throw disableStudentError;
    } catch (error) {
      if (enrollmentsRetired) await supabase.from('matriculas').update({ estado: 'activo' }).in('id', (matriculas || []).map((row: any) => row.id));
      if (accountsDisabled) await supabase.from('usuarios').update({ activo: true }).in('id', (usuarios || []).filter((user: any) => user.activo !== false).map((user: any) => user.id));
      throw error;
    }

    return res.json({ success: true, message: 'Estudiante retirado. Su historial se conservó.', data: { id, estado: 'inactivo' } });
  } catch (error: any) {
    console.error('Admin student retirement failed:', error);
    return res.status(500).json({ success: false, message: 'No se pudo retirar al estudiante' });
  }
});

router.get('/alumnos/estadisticas', async (req, res) => {
  try {
    const { count: totalAlumnos } = await supabase
      .from('alumnos')
      .select('*', { count: 'exact', head: true });

    const { data: alumnosPorEstado } = await supabase
      .from('alumnos')
      .select('estado')
      .eq('estado', 'activo');

    const { data: alumnosPorSeccion } = await supabase
      .from('matriculas')
      .select(`
        secciones!inner (
          nombre,
          grados!inner (
            nombre
          )
        )
      `);

    res.json({
      success: true,
      data: {
        total: totalAlumnos || 0,
        activos: alumnosPorEstado?.length || 0,
        por_seccion: alumnosPorSeccion || []
      }
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener estadísticas',
      error: error.message
    });
  }
});

router.get('/asistencia/historial', async (req, res) => {
  try {
    const desde = String(req.query.desde || '');
    const hasta = String(req.query.hasta || '');
    const fechaValida = /^\d{4}-\d{2}-\d{2}$/;

    if (!fechaValida.test(desde) || !fechaValida.test(hasta)) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_DATE_RANGE',
        message: 'Las fechas deben tener el formato YYYY-MM-DD'
      });
    }
    if (desde > hasta) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_DATE_RANGE',
        message: 'La fecha inicial no puede ser posterior a la fecha final'
      });
    }

    let rows: any[] = [];
    let resumen = new Map<string, { fecha: string; presentes: number; tardanzas: number; ausentes: number }>();
    const { data: rpcData, error: rpcError } = await supabase.rpc('resumen_asistencias_por_rango', {
      p_desde: desde,
      p_hasta: hasta
    });

    if (!rpcError) {
      for (const row of rpcData || []) {
        const fecha = String(row.fecha);
        resumen.set(fecha, {
          fecha,
          presentes: Number(row.presentes) || 0,
          tardanzas: Number(row.tardanzas) || 0,
          ausentes: Number(row.ausentes) || 0
        });
      }
    } else if (rpcError.code === 'PGRST202' || rpcError.code === '42883') {
      for (let offset = 0; ; offset += 1000) {
        const { data, error } = await supabase
          .from('asistencias')
          .select('fecha, estado')
          .eq('tipo_persona', 'alumno')
          .gte('fecha', desde)
          .lte('fecha', hasta)
          .range(offset, offset + 999);
        if (error) throw error;
        rows.push(...(data || []));
        if (!data || data.length < 1000) break;
      }
    } else {
      throw rpcError;
    }

    if (rpcError) {
      for (const row of rows) {
        const fecha = String(row.fecha);
        const actual = resumen.get(fecha) || { fecha, presentes: 0, tardanzas: 0, ausentes: 0 };
        const estado = String(row.estado || '').toLowerCase();
        if (estado === 'tardanza') actual.tardanzas++;
        else if (estado === 'falta' || estado === 'ausente') actual.ausentes++;
        else if (estado === 'presente') actual.presentes++;
        resumen.set(fecha, actual);
      }
    }

    return res.json({
      success: true,
      data: [...resumen.values()].sort((a, b) => b.fecha.localeCompare(a.fecha)),
      total: resumen.size
    });
  } catch (error: any) {
    console.error('Attendance history summary failed:', error.message);
    return res.status(500).json({
      success: false,
      code: 'ATTENDANCE_HISTORY_FAILED',
      message: 'No se pudo cargar el historial de asistencias',
      error: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

router.get('/asistencia/fecha', async (req, res) => {
  try {
    const { fecha } = req.query;

    if (!fecha) {
      return res.status(400).json({
        success: false,
        message: 'Se requiere el parámetro fecha'
      });
    }

    const { data: asistencias, error } = await supabase
      .from('asistencias')
      .select(`
        id,
        persona_id,
        fecha,
        hora_entrada,
        estado,
        tipo_persona
      `)
      .eq('fecha', fecha)
      .eq('tipo_persona', 'alumno');

    if (error) {
      console.error('Attendance query failed:', error);
      throw error;
    }

    if (!asistencias || asistencias.length === 0) {
      return res.json({
        success: true,
        data: [],
        total: 0,
        message: 'No hay asistencias para esta fecha'
      });
    }

    const personaIds = [...new Set(asistencias.map((asistencia: any) => asistencia.persona_id).filter(Boolean))];
    const alumnos: any[] = [];
    for (let index = 0; index < personaIds.length; index += 100) {
      const { data, error: alumnosError } = await supabase
        .from('alumnos')
        .select(`
          id,
          persona_id,
          personas!inner (
            id,
            nombres,
            apellidos
          ),
          matriculas!inner (
            secciones!inner (
              nombre,
              grados!inner (
                nombre
              )
            )
          )
        `)
        .in('persona_id', personaIds.slice(index, index + 100));

      if (alumnosError) {
        console.error('Student query failed:', alumnosError);
        throw alumnosError;
      }
      alumnos.push(...(data || []));
    }

    const alumnosPorPersonaId = new Map((alumnos || []).map(a => [a.persona_id, a]));

    const asistenciasFormateadas = asistencias.map(asist => {
      const alumno = alumnosPorPersonaId.get(asist.persona_id);
      
      if (!alumno) {
        return null;
      }

      const persona = Array.isArray(alumno.personas) ? alumno.personas[0] : alumno.personas;
      const matricula = Array.isArray(alumno.matriculas) ? alumno.matriculas[0] : alumno.matriculas;
      const seccion = Array.isArray(matricula?.secciones) ? matricula.secciones[0] : matricula?.secciones;
      const grado = Array.isArray(seccion?.grados) ? seccion.grados[0] : seccion?.grados;

      const horaCruda = asist.hora_entrada || '';
      const horaFormateada = horaCruda.includes('AM') || horaCruda.includes('PM')
        ? horaCruda
        : (() => {
            const partes = horaCruda.split(':');
            if (partes.length < 2) return horaCruda;
            let h = parseInt(partes[0], 10);
            const m = partes[1];
            const ampm = h >= 12 ? 'PM' : 'AM';
            if (h > 12) h -= 12;
            if (h === 0) h = 12;
            return `${h.toString().padStart(2, '0')}:${m} ${ampm}`;
          })();

      return {
        id: asist.id,
        persona_id: asist.persona_id,
        nombre_completo: `${persona.nombres} ${persona.apellidos}`,
        salon: `${grado?.nombre || ''} ${seccion?.nombre || ''}`.trim(),
        hora_registro: horaFormateada,
        estado_entrada: asist.estado,
        fecha: asist.fecha
      };
    }).filter(a => a !== null);

    res.json({
      success: true,
      data: asistenciasFormateadas,
      total: asistenciasFormateadas.length
    });
  } catch (error: any) {
    console.error('Admin attendance endpoint failed:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener asistencias',
      error: error.message
    });
  }
});

export default router;
