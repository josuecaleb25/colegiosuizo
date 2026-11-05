export const webPermissionKeys = [
  'dashboard.view',
  'members.view',
  'members.create',
  'members.update',
  'members.assign_role',
  'members.deactivate',
  'roles.view',
  'roles.create',
  'roles.update',
  'roles.delete',
  'institution.view',
  'institution.update',
  'profile.view',
  'profile.update'
] as const;

export function normalizeWebPermissions(value: unknown) {
  if (!Array.isArray(value) || value.some(permission => typeof permission !== 'string' || !webPermissionKeys.includes(permission as any))) return null;
  return [...new Set(value as string[])];
}

export function createWebRoleKey(name: string) {
  const slug = name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
  return slug ? `custom_${slug}` : null;
}

export function hasWebPermissions(granted: unknown, required: string[]) {
  if (!Array.isArray(granted)) return false;
  return required.every(permission => granted.includes(permission));
}
