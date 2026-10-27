import { createHash, randomBytes } from 'crypto';

export function hashAccountActivationToken(token: string) {
  return createHash('sha256').update(token, 'utf8').digest('hex');
}

export function createAccountActivationToken() {
  const token = randomBytes(32).toString('base64url');
  return { token, tokenHash: hashAccountActivationToken(token) };
}

export function resolveAccountRole(isActiveStudent: boolean, isActiveTeacher: boolean) {
  if (isActiveStudent === isActiveTeacher) return null;
  return isActiveStudent ? 'alumno' : 'profesor';
}
