const autocannon = require('autocannon');

const baseUrl = process.env.LOAD_BASE_URL || 'http://127.0.0.1:8000';
const path = process.env.LOAD_PATH || '/healthz';
const connections = parseInteger('LOAD_CONNECTIONS', 10, 1, 1000);
const duration = parseInteger('LOAD_DURATION_SECONDS', 15, 1, 300);
const allowRemote = process.env.LOAD_ALLOW_REMOTE === '1';
const highConcurrencyConfirmed = process.env.LOAD_CONFIRM_HIGH_CONCURRENCY === '1';
const bearerToken = process.env.LOAD_BEARER_TOKEN;

function parseInteger(name, fallback, minimum, maximum) {
  const rawValue = process.env[name];
  if (rawValue === undefined) return fallback;
  const value = Number(rawValue);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} debe ser un entero entre ${minimum} y ${maximum}`);
  }
  return value;
}

function isAllowedPath(value) {
  return value === '/healthz'
    || value === '/api/horarios/test'
    || value === '/api/mobile/test'
    || /^\/api\/horarios\/(alumno|profesor|seccion)\/[a-zA-Z0-9_-]+$/.test(value);
}

function validateTarget() {
  let target;
  try {
    target = new URL(baseUrl);
  } catch {
    throw new Error('LOAD_BASE_URL debe ser una URL válida');
  }

  if (!['http:', 'https:'].includes(target.protocol)) {
    throw new Error('LOAD_BASE_URL debe usar HTTP o HTTPS');
  }

  if (!allowRemote && !['localhost', '127.0.0.1', '::1'].includes(target.hostname)) {
    throw new Error('La prueba solo acepta localhost. Para habilitar un host remoto establece LOAD_ALLOW_REMOTE=1');
  }

  if (!path.startsWith('/') || path.startsWith('//') || !isAllowedPath(path)) {
    throw new Error('LOAD_PATH debe ser una ruta GET de lectura permitida; revisa tests/load-api.md');
  }

  if (connections > 100 && !highConcurrencyConfirmed) {
    throw new Error('Más de 100 conexiones requiere LOAD_CONFIRM_HIGH_CONCURRENCY=1');
  }

  return target;
}

function formatRate(value) {
  return Number.isFinite(value) ? value.toFixed(2) : 'n/d';
}

function formatLatency(value) {
  return Number.isFinite(value) ? `${value.toFixed(2)} ms` : 'n/d';
}

function run() {
  const target = validateTarget();
  const requestUrl = new URL(path, target);
  const options = {
    url: requestUrl.origin,
    connections,
    duration,
    pipelining: 1,
    method: 'GET',
    requests: [{ path: `${requestUrl.pathname}${requestUrl.search}` }],
    title: 'API local'
  };

  if (bearerToken) options.headers = { authorization: `Bearer ${bearerToken}` };

  if (process.env.LOAD_DRY_RUN === '1') {
    process.stdout.write(`${JSON.stringify({
      method: options.method,
      target: options.url,
      path: options.requests[0].path,
      connections,
      durationSeconds: duration,
      pipelining: options.pipelining,
      tokenConfigured: Boolean(bearerToken)
    }, null, 2)}\n`);
    return;
  }

  process.stdout.write(`Prueba local GET ${options.path}: ${connections} conexiones durante ${duration}s\n`);

  autocannon(options, (error, result) => {
    if (error) {
      process.stderr.write(`Falló la prueba: ${error.message}\n`);
      process.exitCode = 1;
      return;
    }

    process.stdout.write(`Solicitudes completadas: ${result.requests.total}\n`);
    process.stdout.write(`Tasa promedio: ${formatRate(result.requests.average)} req/s\n`);
    process.stdout.write(`Latencia p50/p90/p97.5/p99: ${formatLatency(result.latency.p50)} / ${formatLatency(result.latency.p90)} / ${formatLatency(result.latency.p97_5)} / ${formatLatency(result.latency.p99)}\n`);
    process.stdout.write(`Respuestas no 2xx: ${result.non2xx}\n`);
    process.stdout.write(`Errores de conexión: ${result.errors}\n`);
    process.stdout.write(`Timeouts: ${result.timeouts}\n`);
    process.stdout.write(`Datos recibidos: ${formatRate(result.throughput.average / 1024)} KiB/s\n`);

    if (result.non2xx > 0 || result.errors > 0 || result.timeouts > 0) process.exitCode = 1;
  });
}

try {
  run();
} catch (error) {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
}
