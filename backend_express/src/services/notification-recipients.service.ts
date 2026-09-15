export type NotificationKind = 'asistencia' | 'comunicado' | 'calificacion';

export interface NotificationUser {
  personaId: string;
  rol: string;
  activo?: boolean;
  notificacionesHabilitadas?: boolean;
}

export interface NotificationRecipientContext {
  kind: NotificationKind;
  studentPersonaId?: string;
  sectionStudentPersonaIds?: string[];
  sectionTeacherPersonaIds?: string[];
  globalUsers?: NotificationUser[];
}

/**
 * Resolves recipients without touching the database. Database adapters can
 * prepare this context and use the same policy for push and history.
 */
export function resolveNotificationRecipients(context: NotificationRecipientContext): string[] {
  const recipients: string[] = [];

  if (context.kind === 'asistencia') {
    if (context.studentPersonaId) recipients.push(context.studentPersonaId);
    return uniqueIds(recipients);
  }

  if (context.kind === 'comunicado' && context.globalUsers) {
    recipients.push(
      ...context.globalUsers
        .filter((user) => user.activo !== false && user.notificacionesHabilitadas !== false)
        .map((user) => user.personaId)
    );
  }

  if (context.kind === 'comunicado') {
    recipients.push(...(context.sectionStudentPersonaIds || []));
    recipients.push(...(context.sectionTeacherPersonaIds || []));
  }

  return uniqueIds(recipients);
}

function uniqueIds(ids: string[]): string[] {
  return [...new Set(ids.filter(Boolean))];
}
