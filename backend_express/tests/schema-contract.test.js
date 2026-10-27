const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const sourceRoot = path.join(__dirname, '..', 'src');
const schemaTables = new Set([
  'alumnos',
  'anios_lectivos',
  'asignaciones',
  'asistencia_sesiones',
  'asistencias',
  'calificaciones',
  'codigos_qr',
  'colegio',
  'comunicados_nuevos',
  'device_tokens',
  'docentes',
  'evaluaciones',
  'grados',
  'horarios',
  'justificaciones',
  'lecturas_comunicados',
  'matriculas',
  'notificaciones_historial',
  'personas',
  'secciones',
  'tutor_seccion',
  'usuarios'
]);

function listSourceFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const entryPath = path.join(directory, entry.name);
    return entry.isDirectory() ? listSourceFiles(entryPath) : entry.name.endsWith('.ts') ? [entryPath] : [];
  });
}

test('all literal Supabase table references exist in the connected public schema', () => {
  const unknownReferences = [];
  const tableReference = /\.from\(\s*['"]([^'"]+)['"]\s*\)/g;

  for (const file of listSourceFiles(sourceRoot)) {
    const contents = fs.readFileSync(file, 'utf8');
    for (const match of contents.matchAll(tableReference)) {
      if (!schemaTables.has(match[1])) unknownReferences.push(`${path.relative(sourceRoot, file)}: ${match[1]}`);
    }
  }

  assert.deepEqual(unknownReferences, []);
});

test('mobile QR attendance has one route and the alumni router exposes only implemented reads', () => {
  const mobilePath = require.resolve('../dist/routes/mobile.routes');
  const alumniPath = require.resolve('../dist/routes/alumnos.routes');
  const databasePath = require.resolve('../dist/config/database');
  const mockDatabase = { from: () => { throw new Error('Unexpected database query during router registration'); } };

  require.cache[databasePath] = {
    id: databasePath,
    filename: databasePath,
    loaded: true,
    exports: { __esModule: true, default: mockDatabase }
  };
  delete require.cache[mobilePath];
  delete require.cache[alumniPath];

  const mobileRouter = require(mobilePath).default;
  const alumniRouter = require(alumniPath).default;
  const qrRoutes = mobileRouter.stack.filter(layer => layer.route?.path === '/asistencia/escanear-qr' && layer.route.methods.post);
  const alumniMethods = alumniRouter.stack.filter(layer => layer.route).map(layer => Object.keys(layer.route.methods)[0]);

  assert.equal(qrRoutes.length, 1);
  assert.deepEqual(alumniMethods, ['get', 'get']);
});
