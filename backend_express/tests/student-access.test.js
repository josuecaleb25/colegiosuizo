const test = require('node:test');
const assert = require('node:assert/strict');

const queriedTables = [];

class Query {
  constructor(table) {
    this.table = table;
    this.filters = [];
    queriedTables.push(table);
  }

  select() { return this; }
  eq(column, value) { this.filters.push([column, value]); return this; }

  maybeSingle() {
    const idFilter = this.filters.find(([column]) => column === 'id');
    const row = this.table === 'alumnos' && idFilter?.[1] === 'student-id'
      ? { id: 'student-id', persona_id: 'child-id' }
      : null;
    return Promise.resolve({ data: row, error: null });
  }
}

const databasePath = require.resolve('../dist/config/database');
require.cache[databasePath] = {
  id: databasePath,
  filename: databasePath,
  loaded: true,
  exports: { __esModule: true, default: { from: table => new Query(table) } }
};

const { canAccessStudent } = require('../dist/middleware/student-access');

test('a student cannot read another student without a family relationship', async () => {
  queriedTables.length = 0;

  const allowed = await canAccessStudent({ id: 'other-student-id', email: 'other@example.test', rol: 'alumno' }, 'student-id');

  assert.equal(allowed, false);
  assert.deepEqual(queriedTables, ['alumnos']);
});

test('a student identity linked to its own persona can access its student record', async () => {
  queriedTables.length = 0;

  const allowed = await canAccessStudent({ id: 'child-id', email: 'student@example.test', rol: 'alumno' }, 'student-id');

  assert.equal(allowed, true);
  assert.deepEqual(queriedTables, ['alumnos']);
});
