import { timingSafeEqual } from 'crypto';

const sharedStudentPassword = 'Suizo2026*';

export function personaHasStudentLoginCredentials(persona: any, email: string, password: string) {
  const personaEmail = typeof persona?.correo === 'string' ? persona.correo.trim().toLowerCase() : '';
  if (!personaEmail || personaEmail !== email.trim().toLowerCase()) return false;
  if (!Array.isArray(persona.alumnos) || persona.alumnos.length === 0) return false;

  const passwordBytes = Buffer.from(password);
  const expectedBytes = Buffer.from(sharedStudentPassword);
  return passwordBytes.length === expectedBytes.length && timingSafeEqual(passwordBytes, expectedBytes);
}
