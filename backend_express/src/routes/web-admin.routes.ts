import { Router } from 'express';
import supabase from '../config/database';
import { WebMemberRequest, requireWebAdministrator, requireWebPermissions, webMemberMiddleware } from '../middleware/web-members';
import { normalizeWebPermissions, createWebRoleKey } from '../services/web-permissions';

const router = Router();
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const eligibleAppRoles = new Set(['administrador', 'admin', 'auxiliar', 'moderador']);

function relation(value: any) {
  return Array.isArray(value) ? value[0] || null : value || null;
}

router.use(webMemberMiddleware);

router.get('/roles', requireWebPermissions('roles.view'), async (_req, res) => {
  try {
    const [{ data: roles, error: rolesError }, { data: members, error: membersError }] = await Promise.all([
      supabase.from('web_roles').select('id, role_key, name, description, permissions, is_system, active, created_at, updated_at').order('name', { ascending: true }),
      supabase.from('web_members').select('role_id, active, activated_at, usuarios!inner(activo), web_roles!inner(active)')
    ]);

    if (rolesError) throw rolesError;
    if (membersError) throw membersError;

    const counts = new Map<string, { total: number; active: number; pending: number }>();
    for (const member of members || []) {
      const count = counts.get(member.role_id) || { total: 0, active: 0, pending: 0 };
      const account = relation(member.usuarios);
      const role = relation(member.web_roles);
      count.total += 1;
      if (member.active && account?.activo && member.activated_at && role?.active) count.active += 1;
      if (member.active && !member.activated_at) count.pending += 1;
      counts.set(member.role_id, count);
    }

    return res.json({ success: true, data: (roles || []).map(role => ({
      ...role,
      member_count: counts.get(role.id)?.total || 0,
      active_member_count: counts.get(role.id)?.active || 0,
      pending_member_count: counts.get(role.id)?.pending || 0
    })) });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudieron consultar los roles web' });
  }
});

router.post('/roles', requireWebAdministrator, async (req, res) => {
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  if (Object.keys(body).some(key => !['name', 'description', 'permissions'].includes(key))) {
    return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  }

  const name = typeof body.name === 'string' ? body.name.trim() : '';
  const description = typeof body.description === 'string' ? body.description.trim() : '';
  const roleKey = name ? createWebRoleKey(name) : null;
  const permissions = normalizeWebPermissions(body.permissions === undefined ? [] : body.permissions);
  if (!name || name.length > 60 || !roleKey || description.length > 240 || !permissions) {
    return res.status(400).json({ success: false, message: 'Nombre, descripción o permisos del rol inválidos' });
  }

  try {
    const { data, error } = await supabase.from('web_roles').insert({
      role_key: roleKey,
      name,
      description,
      permissions,
      is_system: false,
      active: true
    }).select('id, role_key, name, description, permissions, is_system, active, created_at').single();
    if (error?.code === '23505') return res.status(409).json({ success: false, message: 'Ya existe un rol con ese nombre' });
    if (error) throw error;
    return res.status(201).json({ success: true, data });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo crear el rol web' });
  }
});

router.patch('/roles/:id', requireWebAdministrator, async (req, res) => {
  const id = String(req.params.id || '');
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  if (!uuidPattern.test(id)) return res.status(400).json({ success: false, message: 'Identificador de rol inválido' });
  if (Object.keys(body).some(key => !['name', 'description', 'permissions', 'active'].includes(key))) {
    return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  }

  try {
    const { data: role, error: roleError } = await supabase.from('web_roles').select('id, role_key, is_system').eq('id', id).maybeSingle();
    if (roleError) throw roleError;
    if (!role) return res.status(404).json({ success: false, message: 'Rol no encontrado' });
    if (role.role_key === 'web_administrator') return res.status(409).json({ success: false, message: 'El rol Administrador está protegido' });

    const updates: Record<string, unknown> = { updated_at: new Date().toISOString() };
    if (body.name !== undefined) {
      const name = typeof body.name === 'string' ? body.name.trim() : '';
      if (!name || name.length > 60) return res.status(400).json({ success: false, message: 'El nombre debe tener entre 1 y 60 caracteres' });
      updates.name = name;
    }
    if (body.description !== undefined) {
      if (typeof body.description !== 'string' || body.description.trim().length > 240) return res.status(400).json({ success: false, message: 'La descripción no puede superar 240 caracteres' });
      updates.description = body.description.trim();
    }
    if (body.permissions !== undefined) {
      const permissions = normalizeWebPermissions(body.permissions);
      if (!permissions) return res.status(400).json({ success: false, message: 'La lista de permisos contiene valores inválidos' });
      updates.permissions = permissions;
    }
    if (body.active !== undefined) {
      if (typeof body.active !== 'boolean') return res.status(400).json({ success: false, message: 'El estado debe ser booleano' });
      if (!body.active) {
        const { count, error } = await supabase.from('web_members').select('id', { count: 'exact', head: true }).eq('role_id', id).eq('active', true);
        if (error) throw error;
        if ((count || 0) > 0) return res.status(409).json({ success: false, message: 'Reasigna o desactiva los miembros antes de desactivar el rol' });
      }
      updates.active = body.active;
    }

    const { data, error } = await supabase.from('web_roles').update(updates).eq('id', id).select('id, role_key, name, description, permissions, is_system, active, updated_at').single();
    if (error?.code === '23505') return res.status(409).json({ success: false, message: 'Ya existe un rol con ese nombre' });
    if (error) throw error;
    return res.json({ success: true, data });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo actualizar el rol web' });
  }
});

router.delete('/roles/:id', requireWebAdministrator, async (req, res) => {
  const id = String(req.params.id || '');
  if (!uuidPattern.test(id)) return res.status(400).json({ success: false, message: 'Identificador de rol inválido' });

  try {
    const { data: role, error: roleError } = await supabase.from('web_roles').select('id, is_system, role_key').eq('id', id).maybeSingle();
    if (roleError) throw roleError;
    if (!role) return res.status(404).json({ success: false, message: 'Rol no encontrado' });
    if (role.is_system || role.role_key === 'web_administrator') return res.status(409).json({ success: false, message: 'Los roles predeterminados no se pueden eliminar' });

    const { count, error: countError } = await supabase.from('web_members').select('id', { count: 'exact', head: true }).eq('role_id', id);
    if (countError) throw countError;
    if ((count || 0) > 0) return res.status(409).json({ success: false, message: 'Reasigna los miembros antes de eliminar este rol' });

    const { error } = await supabase.from('web_roles').delete().eq('id', id);
    if (error) throw error;
    return res.json({ success: true, message: 'Rol eliminado' });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo eliminar el rol web' });
  }
});

router.get('/members', requireWebPermissions('members.view'), async (req, res) => {
  const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
  const offset = Math.max(Number(req.query.offset) || 0, 0);
  const roleId = typeof req.query.role_id === 'string' ? req.query.role_id : '';
  const activeFilter = typeof req.query.active === 'string' ? req.query.active : '';
  if (roleId && !uuidPattern.test(roleId)) return res.status(400).json({ success: false, message: 'Identificador de rol inválido' });
  if (activeFilter && !['true', 'false', 'pending'].includes(activeFilter)) return res.status(400).json({ success: false, message: 'Filtro de estado inválido' });

  try {
    let query = supabase.from('web_members').select('id, role_id, active, activated_at, created_at, usuarios!inner(id, persona_id, email, rol, activo, personas!inner(nombres, apellidos)), web_roles!inner(id, role_key, name, active)', { count: 'exact' });
    if (roleId) query = query.eq('role_id', roleId);
    if (activeFilter === 'true') query = query.eq('active', true).not('activated_at', 'is', null);
    if (activeFilter === 'false') query = query.eq('active', false);
    if (activeFilter === 'pending') query = query.is('activated_at', null).eq('active', true);
    const { data: rows, error, count } = await query.order('created_at', { ascending: false }).range(offset, offset + limit - 1);
    if (error) throw error;

    const data = (rows || []).map((row: any) => {
      const account = relation(row.usuarios);
      const person = relation(account?.personas);
      const role = relation(row.web_roles);
      return {
        id: row.id,
        usuario_id: account?.id,
        nombres: person?.nombres || '',
        apellidos: person?.apellidos || '',
        nombre_completo: `${person?.nombres || ''} ${person?.apellidos || ''}`.trim(),
        email: account?.email || '',
        role: role ? { id: role.id, key: role.role_key, name: role.name } : null,
        active: row.active === true && account?.activo === true && Boolean(row.activated_at) && role?.active === true,
        pending_activation: row.active === true && !row.activated_at,
        created_at: row.created_at
      };
    });

    return res.json({ success: true, data, total: count || 0, limit, offset });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudieron consultar los miembros web' });
  }
});

router.post('/members', requireWebAdministrator, async (req: WebMemberRequest, res) => {
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  if (Object.keys(body).some(key => !['usuario_id', 'role_id'].includes(key))) return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  const usuarioId = typeof body.usuario_id === 'string' ? body.usuario_id : '';
  const roleId = typeof body.role_id === 'string' ? body.role_id : '';
  if (!uuidPattern.test(usuarioId) || !uuidPattern.test(roleId)) return res.status(400).json({ success: false, message: 'Cuenta o rol inválido' });

  try {
    const [{ data: account, error: accountError }, { data: role, error: roleError }] = await Promise.all([
      supabase.from('usuarios').select('id, rol, activo').eq('id', usuarioId).maybeSingle(),
      supabase.from('web_roles').select('id, active').eq('id', roleId).maybeSingle()
    ]);
    if (accountError) throw accountError;
    if (roleError) throw roleError;
    if (!account || account.activo !== true || !eligibleAppRoles.has(String(account.rol).toLowerCase())) {
      return res.status(400).json({ success: false, message: 'Solo se pueden agregar cuentas activas de administración o auxiliares; estudiantes y profesores no son miembros web' });
    }
    if (!role || role.active !== true) return res.status(400).json({ success: false, message: 'El rol web no existe o está desactivado' });

    const now = new Date().toISOString();
    const { data, error } = await supabase.from('web_members').insert({
      usuario_id: usuarioId,
      role_id: roleId,
      active: true,
      activated_at: now,
      created_by: req.webMember?.personaId
    }).select('id, usuario_id, role_id, active, activated_at, created_at').single();
    if (error?.code === '23505') return res.status(409).json({ success: false, message: 'La cuenta ya es miembro web' });
    if (error) throw error;
    return res.status(201).json({ success: true, data });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo agregar el miembro web' });
  }
});

router.patch('/members/:id', requireWebAdministrator, async (req, res) => {
  const id = String(req.params.id || '');
  const body = req.body && typeof req.body === 'object' && !Array.isArray(req.body) ? req.body : {};
  if (!uuidPattern.test(id)) return res.status(400).json({ success: false, message: 'Identificador de miembro inválido' });
  if (Object.keys(body).some(key => !['role_id', 'active'].includes(key))) return res.status(400).json({ success: false, message: 'La solicitud contiene campos no permitidos' });
  if (body.role_id === undefined && body.active === undefined) return res.status(400).json({ success: false, message: 'Indica el rol o el estado que deseas cambiar' });
  if (body.role_id !== undefined && (typeof body.role_id !== 'string' || !uuidPattern.test(body.role_id))) return res.status(400).json({ success: false, message: 'Identificador de rol inválido' });
  if (body.active !== undefined && typeof body.active !== 'boolean') return res.status(400).json({ success: false, message: 'El estado debe ser booleano' });

  try {
    const { data: member, error: memberError } = await supabase.from('web_members')
      .select('id, role_id, active, usuarios!inner(id, activo), web_roles!inner(role_key)')
      .eq('id', id).maybeSingle();
    if (memberError) throw memberError;
    if (!member) return res.status(404).json({ success: false, message: 'Miembro no encontrado' });
    const currentRole = relation(member.web_roles);
    const account = relation(member.usuarios);

    if (body.role_id !== undefined) {
      const { data: role, error } = await supabase.from('web_roles').select('id, active').eq('id', body.role_id).maybeSingle();
      if (error) throw error;
      if (!role || role.active !== true) return res.status(400).json({ success: false, message: 'El rol web no existe o está desactivado' });
    }

    const removesActiveAdministrator = currentRole?.role_key === 'web_administrator'
      && (body.active === false || (body.role_id !== undefined && body.role_id !== member.role_id));
    if (removesActiveAdministrator && member.active && account?.activo) {
      const { data: administrators, error } = await supabase.from('web_members')
        .select('id, active, activated_at, usuarios!inner(activo), web_roles!inner(role_key, active)')
        .eq('active', true);
      if (error) throw error;
      const activeAdmins = (administrators || []).filter((entry: any) => {
        const entryAccount = relation(entry.usuarios);
        const entryRole = relation(entry.web_roles);
        return entryRole?.role_key === 'web_administrator' && entryRole.active && entryAccount?.activo && entry.activated_at;
      });
      if (activeAdmins.length <= 1) return res.status(409).json({ success: false, message: 'Debe quedar al menos un administrador web activo' });
    }

    const updates: Record<string, unknown> = { updated_at: new Date().toISOString() };
    if (body.role_id !== undefined) updates.role_id = body.role_id;
    if (body.active !== undefined) updates.active = body.active;
    const { data, error } = await supabase.from('web_members').update(updates).eq('id', id)
      .select('id, usuario_id, role_id, active, activated_at, created_at').single();
    if (error) throw error;
    return res.json({ success: true, data });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo actualizar el miembro web' });
  }
});

router.delete('/members/:id', requireWebAdministrator, async (req, res) => {
  const id = String(req.params.id || '');
  if (!uuidPattern.test(id)) return res.status(400).json({ success: false, message: 'Identificador de miembro inválido' });

  try {
    const { data: member, error: memberError } = await supabase.from('web_members')
      .select('id, role_id, active, activated_at, usuarios!inner(activo), web_roles!inner(role_key, active)')
      .eq('id', id).maybeSingle();
    if (memberError) throw memberError;
    if (!member) return res.status(404).json({ success: false, message: 'Miembro no encontrado' });
    const role = relation(member.web_roles);
    const account = relation(member.usuarios);

    if (role?.role_key === 'web_administrator' && member.active && account?.activo && role.active && member.activated_at) {
      const { data: administrators, error } = await supabase.from('web_members')
        .select('id, active, activated_at, usuarios!inner(activo), web_roles!inner(role_key, active)')
        .eq('active', true);
      if (error) throw error;
      const activeAdmins = (administrators || []).filter((entry: any) => {
        const entryAccount = relation(entry.usuarios);
        const entryRole = relation(entry.web_roles);
        return entryRole?.role_key === 'web_administrator' && entryRole.active && entryAccount?.activo && entry.activated_at;
      });
      if (activeAdmins.length <= 1) return res.status(409).json({ success: false, message: 'Debe quedar al menos un administrador web activo' });
    }

    const { error } = await supabase.from('web_members').update({ active: false, updated_at: new Date().toISOString() }).eq('id', id);
    if (error) throw error;
    return res.json({ success: true, message: 'Miembro retirado de la administración web' });
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo retirar el miembro web' });
  }
});

export default router;
