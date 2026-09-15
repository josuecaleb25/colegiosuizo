const test = require('node:test');
const assert = require('node:assert/strict');
const { resolveNotificationRecipients } = require('../dist/modules/notifications/notification-recipients.service.js');

test('attendance notification only targets the student account used by the parent', () => {
  const result = resolveNotificationRecipients({
    kind: 'asistencia',
    studentPersonaId: 'student-persona-1',
    sectionTeacherPersonaIds: ['teacher-1'],
    globalUsers: [{ personaId: 'admin-1', rol: 'administrador' }]
  });

  assert.deepEqual(result, ['student-persona-1']);
});

test('section announcement targets students and assigned teachers without duplicates', () => {
  const result = resolveNotificationRecipients({
    kind: 'comunicado',
    sectionStudentPersonaIds: ['student-1', 'student-2', 'student-1'],
    sectionTeacherPersonaIds: ['teacher-1']
  });

  assert.deepEqual(result, ['student-1', 'student-2', 'teacher-1']);
});

test('global announcement excludes inactive users and users without notifications', () => {
  const result = resolveNotificationRecipients({
    kind: 'comunicado',
    globalUsers: [
      { personaId: 'admin-1', rol: 'administrador' },
      { personaId: 'teacher-1', rol: 'profesor', activo: true },
      { personaId: 'disabled-1', rol: 'profesor', activo: false },
      { personaId: 'muted-1', rol: 'alumno', notificacionesHabilitadas: false }
    ]
  });

  assert.deepEqual(result, ['admin-1', 'teacher-1']);
});
