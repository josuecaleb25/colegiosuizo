const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const repositoryRoot = path.resolve(__dirname, '..', '..');
const apiServicePath = path.join(repositoryRoot, 'IEPeruanoSuizo-master/app/src/main/java/com/example/ieperuanosuizoapp/api/ApiService.java');
const routeFiles = new Map([
  ['auth', 'backend_express/src/routes/auth.routes.ts'],
  ['alumnos', 'backend_express/src/routes/alumnos.routes.ts'],
  ['mobile', 'backend_express/src/routes/mobile.routes.ts'],
  ['comunicados', 'backend_express/src/modules/announcements/announcements.routes.ts'],
  ['cursos', 'backend_express/src/routes/cursos.routes.ts'],
  ['horarios', 'backend_express/src/routes/horarios.routes.ts'],
  ['secciones', 'backend_express/src/routes/secciones.routes.ts'],
  ['usuarios', 'backend_express/src/routes/usuarios.routes.ts'],
  ['admin', 'backend_express/src/routes/admin.routes.ts'],
  ['notificaciones', 'backend_express/src/routes/notificaciones.routes.ts']
]);
const attendanceRouteFiles = [
  ['backend_express/src/modules/attendance/sessions/sessions.routes.ts', '/api/asistencia/sesiones'],
  ['backend_express/src/modules/attendance/device-tokens/device-tokens.routes.ts', '/api/asistencia/device-token'],
  ['backend_express/src/modules/attendance/records/records.routes.ts', '/api/asistencia'],
  ['backend_express/src/modules/attendance/records/records.mutations.routes.ts', '/api/asistencia'],
  ['backend_express/src/modules/attendance/reports/reports.routes.ts', '/api/asistencia']
];

function extractRoutes(source, prefix) {
  return [...source.matchAll(/router\.(get|post|put|delete)\(\s*['"]([^'"]+)['"]/gi)]
    .map(match => [match[1].toUpperCase(), `${prefix}${match[2]}`]);
}

function normalize(pathValue) {
  return pathValue.replace(/\{[^}]+\}|:[^/]+/g, '{}').replace(/\/+$/, '') || '/';
}

test('every Android API method and path is registered by the backend', () => {
  const appSource = fs.readFileSync(apiServicePath, 'utf8');
  const appCalls = [...appSource.matchAll(/@(?:retrofit2\.http\.)?(GET|POST|PUT|DELETE)\("([^"]+)"\)/g)]
    .map(match => [match[1].toUpperCase(), `/api/${match[2]}`]);
  const backendRoutes = [];

  for (const [prefix, relativePath] of routeFiles) {
    const source = fs.readFileSync(path.join(repositoryRoot, relativePath), 'utf8');
    backendRoutes.push(...extractRoutes(source, `/api/${prefix}`));
  }

  for (const [relativePath, prefix] of attendanceRouteFiles) {
    const source = fs.readFileSync(path.join(repositoryRoot, relativePath), 'utf8');
    backendRoutes.push(...extractRoutes(source, prefix));
  }

  const missing = appCalls.filter(([method, route]) => !backendRoutes.some(([backendMethod, backendRoute]) =>
    method === backendMethod && normalize(route) === normalize(backendRoute)
  ));

  assert.deepEqual(missing, []);
  assert.equal(appCalls.length, 31);
});
