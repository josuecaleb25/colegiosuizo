require('dotenv').config();

const { randomUUID } = require('node:crypto');
const autocannon = require('autocannon');
const jwt = require('jsonwebtoken');

function parseInteger(name, fallback, minimum, maximum) {
  const rawValue = process.env[name];
  if (rawValue === undefined) return fallback;
  const value = Number(rawValue);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} debe ser un entero entre ${minimum} y ${maximum}`);
  }
  return value;
}

function formatLatency(value) {
  return Number.isFinite(value) ? `${value.toFixed(2)} ms` : 'n/d';
}

function resolveTarget() {
  const baseUrl = process.env.LOAD_BASE_URL || 'http://127.0.0.1:8000';
  const target = new URL(baseUrl);
  if (!['http:', 'https:'].includes(target.protocol)) throw new Error('LOAD_BASE_URL debe usar HTTP o HTTPS');
  if (!['localhost', '127.0.0.1', '[::1]'].includes(target.hostname)) throw new Error('La prueba solo admite localhost');
  if (target.username || target.password) throw new Error('LOAD_BASE_URL no debe incluir credenciales');
  return target;
}

function createUsers(count) {
  const secret = process.env.JWT_SECRET;
  if (!secret || Buffer.byteLength(secret, 'utf8') < 32) throw new Error('JWT_SECRET debe tener al menos 32 bytes');
  return Array.from({ length: count }, (_, index) => {
    const id = randomUUID();
    return {
      id,
      token: jwt.sign({ id, email: `load-${index + 1}@invalid.local`, rol: 'alumno' }, secret, { algorithm: 'HS256', expiresIn: '20m' })
    };
  });
}

function run() {
  const target = resolveTarget();
  const connections = parseInteger('LOAD_CONNECTIONS', 100, 1, 1000);
  const duration = parseInteger('LOAD_DURATION_SECONDS', 15, 1, 300);
  const overallRate = parseInteger('LOAD_REQUESTS_PER_SECOND', 100, 1, 1000);
  const confirmed = process.env.LOAD_CONFIRM_HIGH_CONCURRENCY === '1';
  if (connections > 100 && !confirmed) throw new Error('Más de 100 conexiones requiere LOAD_CONFIRM_HIGH_CONCURRENCY=1');
  const users = createUsers(connections);
  const endpointPaths = [
    '/api/horarios/test',
    '/api/comunicados',
    '/api/secciones',
    '/api/notificaciones?limit=20&page=1',
    '/api/notificaciones/no-leidas',
    userId => `/api/mobile/asistencia/dias-asistidos/${userId}?semana=actual`
  ];
  const statusCounts = new Map(endpointPaths.map((path) => [typeof path === 'string' ? path : '/api/mobile/asistencia/dias-asistidos/:personaId?semana=actual', new Map()]));
  let nextUserIndex = 0;
  const requests = endpointPaths.map((path) => {
    const label = typeof path === 'string' ? path : '/api/mobile/asistencia/dias-asistidos/:personaId?semana=actual';
    return {
      method: 'GET',
      path: typeof path === 'string' ? path : '/api/mobile/asistencia/dias-asistidos/:personaId?semana=actual',
      onResponse(status) {
        const counts = statusCounts.get(label);
        counts.set(status, (counts.get(status) || 0) + 1);
      }
    };
  });

  if (process.env.LOAD_DRY_RUN === '1') {
    process.stdout.write(`${JSON.stringify({
      target: target.origin,
      virtualUsers: connections,
      role: 'alumno',
      durationSeconds: duration,
      maxRequestsPerSecond: overallRate,
      endpoints: endpointPaths.map((path) => typeof path === 'string' ? path : '/api/mobile/asistencia/dias-asistidos/:personaId?semana=actual'),
      identityMode: 'synthetic, unique JWT per connection',
      writes: false
    }, null, 2)}\n`);
    return;
  }

  process.stdout.write(`Simulación de ${connections} alumnos virtuales: ${duration}s, máximo ${overallRate} solicitudes/s\n`);
  autocannon({
    url: target.origin,
    connections,
    duration,
    overallRate,
    pipelining: 1,
    requests,
    setupClient(client) {
      const user = users[nextUserIndex++ % users.length];
      client.setHeaders({ authorization: `Bearer ${user.token}` });
      client.setRequests(requests.map((request) => ({
        ...request,
        path: request.path.includes(':personaId')
          ? request.path.replace(':personaId', user.id)
          : request.path
      })));
    }
  }, (error, result) => {
    if (error) {
      process.stderr.write(`Falló la prueba: ${error.message}\n`);
      process.exitCode = 1;
      return;
    }

    process.stdout.write(`Solicitudes completadas: ${result.requests.total}\n`);
    process.stdout.write(`Tasa promedio: ${result.requests.average.toFixed(2)} req/s\n`);
    process.stdout.write(`Latencia p50/p90/p97.5/p99: ${formatLatency(result.latency.p50)} / ${formatLatency(result.latency.p90)} / ${formatLatency(result.latency.p97_5)} / ${formatLatency(result.latency.p99)}\n`);
    process.stdout.write(`Respuestas no 2xx: ${result.non2xx}\n`);
    process.stdout.write(`Errores de conexión: ${result.errors}\n`);
    process.stdout.write(`Timeouts: ${result.timeouts}\n`);
    process.stdout.write('Estados por endpoint:\n');
    for (const [path, counts] of statusCounts) {
      const summary = [...counts.entries()].map(([status, count]) => `${status}=${count}`).join(', ') || 'sin respuestas';
      process.stdout.write(`${path}: ${summary}\n`);
    }
    if (result.non2xx > 0 || result.errors > 0 || result.timeouts > 0) process.exitCode = 1;
  });
}

try {
  run();
} catch (error) {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
}
