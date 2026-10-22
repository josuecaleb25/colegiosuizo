const test = require('node:test');
const assert = require('node:assert/strict');

const activeSectionId = 'section-1ro-a';
const inactiveSectionId = 'section-old-1ro-a';
const personId = 'person-1';
const calls = [];

const sectionRows = [
  { id: activeSectionId, nombre: 'A', grados: { nombre: '1ro' }, anios_lectivos: { activo: true } },
  { id: 'section-1ro-b', nombre: 'B', grados: { nombre: '1ro' }, anios_lectivos: { activo: true } },
  { id: inactiveSectionId, nombre: 'A', grados: { nombre: '1ro' }, anios_lectivos: { activo: false } }
];

const studentRows = [
  {
    id: 'student-1',
    codigo_alumno: 'ALU-001',
    estado: 'activo',
    persona_id: personId,
    personas: { id: personId, nombres: 'Ana', apellidos: 'Alva', dni: '12345678', correo: null },
    matriculas: [{
      id: 'enrollment-1',
      anio_lectivo_id: 'year-active',
      estado: 'activo',
      seccion_id: activeSectionId,
      secciones: { id: activeSectionId, nombre: 'A', grados: { nombre: '1ro' } }
    }]
  },
  {
    id: 'student-2',
    codigo_alumno: 'ALU-002',
    estado: 'activo',
    persona_id: 'person-2',
    personas: { id: 'person-2', nombres: 'Beto', apellidos: 'Bravo', dni: '87654321', correo: null },
    matriculas: [{
      id: 'enrollment-2',
      anio_lectivo_id: 'year-active',
      estado: 'activo',
      seccion_id: 'section-1ro-b',
      secciones: { id: 'section-1ro-b', nombre: 'B', grados: { nombre: '1ro' } }
    }]
  },
  {
    id: 'student-old',
    codigo_alumno: 'ALU-OLD',
    estado: 'activo',
    persona_id: 'person-old',
    personas: { id: 'person-old', nombres: 'Ceci', apellidos: 'Cruz', dni: '11111111', correo: null },
    matriculas: [{
      id: 'enrollment-old',
      anio_lectivo_id: 'year-old',
      estado: 'activo',
      seccion_id: inactiveSectionId,
      secciones: { id: inactiveSectionId, nombre: 'A', grados: { nombre: '1ro' } }
    }]
  }
];

class Query {
  constructor(table) {
    this.table = table;
    this.filters = [];
    this.start = 0;
    this.end = Infinity;
    calls.push(this);
  }

  select() { return this; }
  eq(column, value) { this.filters.push(['eq', column, value]); return this; }
  in(column, values) { this.filters.push(['in', column, values]); return this; }
  order() { return this; }
  range(start, end) { this.start = start; this.end = end; return this; }

  then(resolve, reject) {
    try {
      let data = [];
      if (this.table === 'secciones') {
        data = sectionRows.filter(row => this.filters.every(([kind, column, value]) =>
          kind !== 'eq' || row.anios_lectivos.activo === value
        ));
      } else if (this.table === 'alumnos') {
        data = studentRows.filter(row => {
          const enrollment = row.matriculas[0];
          return this.filters.every(([kind, column, value]) => {
            if (kind === 'eq' && column === 'estado') return row.estado === value;
            if (kind === 'eq' && column === 'matriculas.estado') return enrollment.estado === value;
            if (kind === 'in' && column === 'matriculas.seccion_id') return value.includes(enrollment.seccion_id);
            return true;
          });
        });
      } else if (this.table === 'codigos_qr') {
        data = [{ persona_id: personId, codigo: 'qr-token-1', generado_en: '2026-01-01T00:00:00Z' }];
      }
      return Promise.resolve({ data: data.slice(this.start, this.end + 1), error: null }).then(resolve, reject);
    } catch (error) {
      return Promise.reject(error).then(resolve, reject);
    }
  }
}

const mockSupabase = { from: table => new Query(table) };
const databasePath = require.resolve('../dist/config/database');
require.cache[databasePath] = {
  id: databasePath,
  filename: databasePath,
  loaded: true,
  exports: { __esModule: true, default: mockSupabase }
};

const routePath = require.resolve('../dist/routes/admin.routes');
delete require.cache[routePath];
const router = require(routePath).default;
const handler = router.stack.find(layer => layer.route?.path === '/alumnos' && layer.route.methods.get).route.stack[0].handle;

function makeResponse() {
  return {
    statusCode: 200,
    body: null,
    status(code) { this.statusCode = code; return this; },
    json(value) { this.body = value; return this; }
  };
}

test('admin roster returns only the active salon selected in the dropdown', async () => {
  calls.length = 0;
  const req = { query: { seccion: '1ro A', limit: '100' } };
  const res = makeResponse();

  await handler(req, res);

  assert.equal(res.statusCode, 200, JSON.stringify(res.body));
  assert.equal(res.body.total, 1);
  assert.equal(res.body.data[0].seccion, '1ro A');
  assert.equal(res.body.data[0].qr_code, 'qr-token-1');
  assert.match(res.body.data[0].qr_image, /^data:image\/png;base64,/);
  assert.equal(calls.length, 3);
  assert.ok(calls.some(query => query.table === 'secciones' && query.filters.some(filter => filter[1] === 'anios_lectivos.activo' && filter[2] === true)));
  assert.ok(calls.some(query => query.table === 'alumnos' && query.filters.some(filter => filter[1] === 'matriculas.seccion_id' && filter[2].includes(activeSectionId))));
  assert.ok(!calls.find(query => query.table === 'alumnos').filters.some(filter => filter[1] === 'matriculas.seccion_id' && filter[2].includes(inactiveSectionId)));

  calls.length = 0;
  const unmatchedResponse = makeResponse();
  await handler({ query: { seccion: '1ro Z', limit: '100' } }, unmatchedResponse);

  assert.equal(unmatchedResponse.statusCode, 200);
  assert.deepEqual(unmatchedResponse.body.data, []);
  assert.equal(calls.some(query => query.table === 'alumnos'), false);
});
