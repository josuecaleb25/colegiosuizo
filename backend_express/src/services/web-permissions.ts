export function hasWebPermissions(granted: unknown, required: string[]) {
  if (!Array.isArray(granted)) return false;
  return required.every(permission => granted.includes(permission));
}
