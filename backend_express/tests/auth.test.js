const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');

process.env.JWT_SECRET = 'test-only-secret-with-more-than-32-bytes';

const { authMiddleware, requireRoles, requireSelfOrAdmin, signAccessToken } = require('../dist/middleware/auth');
const supabase = require('../dist/config/database').default;
const { createRateLimiter } = require('../dist/middleware/rate-limit');
const { createAccountActivationToken, hashAccountActivationToken, resolveAccountRole } = require('../dist/services/account-activation');
const { isPasswordChangeValid } = require('../dist/services/password-change');

function makeResponse() {
  return {
    statusCode: 200,
    headers: {},
    body: null,
    status(code) {
      this.statusCode = code;
      return this;
    },
    json(value) {
      this.body = value;
      return this;
    },
    setHeader(name, value) {
      this.headers[name] = value;
    }
  };
}

test('accepts a current access token and attaches the identity', async () => {
  const token = signAccessToken({ id: 'person-1', email: 'teacher@example.test', rol: 'profesor', sessionVersion: 3 });
  const req = { headers: { authorization: `Bearer ${token}` } };
  const res = makeResponse();
  let continued = false;
  const originalFrom = supabase.from;
  supabase.from = () => ({
    select: () => ({
      eq: () => ({
        maybeSingle: async () => ({ data: { session_version: 3, activo: true }, error: null })
      })
    })
  });

  try {
    await authMiddleware(req, res, () => { continued = true; });
  } finally {
    supabase.from = originalFrom;
  }

  assert.equal(continued, true);
  assert.deepEqual(req.user, { id: 'person-1', email: 'teacher@example.test', rol: 'profesor' });
});

test('rejects a token from a revoked session version', async () => {
  const token = signAccessToken({ id: 'person-1', email: 'teacher@example.test', rol: 'profesor', sessionVersion: 2 });
  const req = { headers: { authorization: `Bearer ${token}` } };
  const res = makeResponse();
  const originalFrom = supabase.from;
  supabase.from = () => ({
    select: () => ({
      eq: () => ({
        maybeSingle: async () => ({ data: { session_version: 3, activo: true }, error: null })
      })
    })
  });

  try {
    await authMiddleware(req, res, () => assert.fail('revoked request should not continue'));
  } finally {
    supabase.from = originalFrom;
  }

  assert.equal(res.statusCode, 401);
});

test('rejects a missing or malformed token', () => {
  for (const authorization of [undefined, 'Bearer not-a-token']) {
    const req = { headers: authorization ? { authorization } : {} };
    const res = makeResponse();
    authMiddleware(req, res, () => assert.fail('request should not continue'));
    assert.equal(res.statusCode, 401);
  }
});

test('does not accept tokens signed with a different algorithm or secret', () => {
  const token = jwt.sign({ id: 'person-1', email: 'teacher@example.test', rol: 'profesor' }, 'other-secret');
  const req = { headers: { authorization: `Bearer ${token}` } };
  const res = makeResponse();

  authMiddleware(req, res, () => assert.fail('request should not continue'));

  assert.equal(res.statusCode, 401);
});

test('enforces role and self-or-admin checks', () => {
  const req = { user: { id: 'person-1', rol: 'alumno' }, params: { id: 'person-2' } };
  const roleResponse = makeResponse();
  requireRoles('admin')(req, roleResponse, () => assert.fail('request should not continue'));
  assert.equal(roleResponse.statusCode, 403);

  const selfResponse = makeResponse();
  requireSelfOrAdmin(request => request.params.id)(req, selfResponse, () => assert.fail('request should not continue'));
  assert.equal(selfResponse.statusCode, 403);

  req.params.id = 'person-1';
  let continued = false;
  requireSelfOrAdmin(request => request.params.id)(req, makeResponse(), () => { continued = true; });
  assert.equal(continued, true);
});

test('rate limiter blocks requests after the configured limit', () => {
  const limiter = createRateLimiter(60000, 1, request => request.ip);
  const req = { ip: '192.0.2.1' };
  const firstResponse = makeResponse();
  let continued = false;

  limiter(req, firstResponse, () => { continued = true; });
  assert.equal(continued, true);

  const secondResponse = makeResponse();
  limiter(req, secondResponse, () => assert.fail('request should be limited'));
  assert.equal(secondResponse.statusCode, 429);
  assert.ok(secondResponse.headers['Retry-After']);
});

test('account activation codes are random, one-way stored, and single role identities', () => {
  const first = createAccountActivationToken();
  const second = createAccountActivationToken();

  assert.equal(first.token.length, 43);
  assert.notEqual(first.token, second.token);
  assert.equal(first.tokenHash, hashAccountActivationToken(first.token));
  assert.notEqual(first.tokenHash, first.token);
  assert.equal(resolveAccountRole(true, false), 'alumno');
  assert.equal(resolveAccountRole(false, true), 'profesor');
  assert.equal(resolveAccountRole(false, false), null);
  assert.equal(resolveAccountRole(true, true), null);
});

test('password changes require a current password and a different password of at least 12 characters', () => {
  assert.equal(isPasswordChangeValid('actual', 'nueva-clave-segura'), true);
  assert.equal(isPasswordChangeValid('', 'nueva-clave-segura'), false);
  assert.equal(isPasswordChangeValid('actual', 'corta'), false);
  assert.equal(isPasswordChangeValid('misma-clave-123', 'misma-clave-123'), false);
  assert.equal(isPasswordChangeValid('x'.repeat(257), 'nueva-clave-segura'), false);
  assert.equal(isPasswordChangeValid('actual', 'x'.repeat(257)), false);
});
