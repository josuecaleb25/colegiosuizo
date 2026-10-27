import { timingSafeEqual } from 'crypto';

const sharedStudentPassword = 'Suizo2026*';

export function studentHasParentLoginCredentials(persona: any, email: string, password: string) {
  const personaEmail = typeof persona?.correo === 'string' ? persona.correo.trim().toLowerCase() : '';
  const linkedStudents = Array.isArray(persona?.alumnos) ? persona.alumnos.length : persona?.alumnos ? 1 : 0;
  if (!personaEmail || personaEmail !== email.trim().toLowerCase() || linkedStudents === 0) return false;

  const passwordBytes = Buffer.from(password);
  const expectedBytes = Buffer.from(sharedStudentPassword);
  return passwordBytes.length === expectedBytes.length && timingSafeEqual(passwordBytes, expectedBytes);
}
