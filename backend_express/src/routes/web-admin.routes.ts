import { Router } from 'express';
import supabase from '../config/database';
import { WebMemberRequest, requireWebPermissions, webMemberMiddleware } from '../middleware/web-members';

const router = Router();

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

router.get('/members', requireWebPermissions('members.view'), async (req: WebMemberRequest, res) => {
  const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
  const offset = Math.max(Number(req.query.offset) || 0, 0);
  const roleId = typeof req.query.role_id === 'string' ? req.query.role_id : '';
  const activeFilter = typeof req.query.active === 'string' ? req.query.active : '';
  if (activeFilter && !['true', 'false', 'pending'].includes(activeFilter)) {
    return res.status(400).json({ success: false, message: 'Filtro de estado inválido' });
  }

  try {
    let query = supabase.from('web_members').select('id, role_id, active, activated_at, created_at, usuarios!inner(id, persona_id, email, activo, personas!inner(nombres, apellidos)), web_roles!inner(id, role_key, name, active)', { count: 'exact' });
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

export default router;
