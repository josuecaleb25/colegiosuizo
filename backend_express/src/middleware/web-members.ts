import { NextFunction, Response } from 'express';
import supabase from '../config/database';
import { AuthRequest } from './auth';
import { hasWebPermissions } from '../services/web-permissions';

const eligibleWebAccountRoles = new Set(['administrador', 'admin', 'profesor']);

export interface WebMemberRequest extends AuthRequest {
  webMember?: {
    id: string;
    accountId: string;
    personaId: string;
    email: string;
    roleId: string;
    roleKey: string;
    roleName: string;
    permissions: string[];
  };
}

export const webMemberMiddleware = async (req: WebMemberRequest, res: Response, next: NextFunction) => {
  if (!req.user) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
  if (req.user.context !== 'web') return res.status(403).json({ success: false, message: 'Esta sesión no tiene acceso al panel web' });

  try {
    const { data: account, error: accountError } = await supabase
      .from('usuarios')
      .select('id, persona_id, email, rol, activo')
      .eq('persona_id', req.user.id)
      .maybeSingle();

    if (accountError || !account || account.activo === false) {
      return res.status(401).json({ success: false, message: 'La cuenta no está disponible' });
    }
    if (!eligibleWebAccountRoles.has(String(account.rol || '').toLowerCase())) {
      return res.status(403).json({ success: false, message: 'Esta cuenta no tiene acceso al panel web' });
    }

    const { data: member, error: memberError } = await supabase
      .from('web_members')
      .select('id, active, activated_at, web_roles!inner(id, role_key, name, permissions, active)')
      .eq('usuario_id', account.id)
      .maybeSingle();

    if (memberError || !member || member.active !== true || !member.activated_at) {
      return res.status(403).json({ success: false, message: 'Esta cuenta no pertenece al equipo web activo' });
    }

    const role = Array.isArray(member.web_roles) ? member.web_roles[0] : member.web_roles;
    if (!role || role.active !== true) return res.status(403).json({ success: false, message: 'El rol del miembro está desactivado' });

    req.webMember = {
      id: member.id,
      accountId: account.id,
      personaId: account.persona_id,
      email: account.email,
      roleId: role.id,
      roleKey: role.role_key,
      roleName: role.name,
      permissions: Array.isArray(role.permissions) ? role.permissions : []
    };
    return next();
  } catch {
    return res.status(500).json({ success: false, message: 'No se pudo validar el acceso al panel web' });
  }
};

export const requireWebPermissions = (...permissions: string[]) => {
  return (req: WebMemberRequest, res: Response, next: NextFunction) => {
    if (!req.webMember) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    if (req.webMember.roleKey === 'web_administrator') return next();
    if (!hasWebPermissions(req.webMember.permissions, permissions)) {
      return res.status(403).json({ success: false, message: 'No tienes permiso para realizar esta operación' });
    }
    return next();
  };
};

export const requireWebAdministrator = (req: WebMemberRequest, res: Response, next: NextFunction) => {
  if (!req.webMember) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
  if (req.webMember.roleKey !== 'web_administrator') {
    return res.status(403).json({ success: false, message: 'Esta acción requiere el rol Administrador del panel web' });
  }
  return next();
};

export const requireWebAdministratorOrPermissions = (...permissions: string[]) => {
  return (req: WebMemberRequest, res: Response, next: NextFunction) => {
    if (!req.webMember) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    if (req.webMember.roleKey === 'web_administrator' || hasWebPermissions(req.webMember.permissions, permissions)) return next();
    return res.status(403).json({ success: false, message: 'No tienes permiso para realizar esta operación' });
  };
};

export const requireWebAdministratorOrAnyPermissions = (...permissions: string[]) => {
  return (req: WebMemberRequest, res: Response, next: NextFunction) => {
    if (!req.webMember) return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    if (req.webMember.roleKey === 'web_administrator' || permissions.some(permission => req.webMember?.permissions.includes(permission))) return next();
    return res.status(403).json({ success: false, message: 'No tienes permiso para realizar esta operación' });
  };
};
