const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');

process.env.JWT_SECRET = 'test-only-secret-with-more-than-32-bytes';

const { authMiddleware, requireRoles, requireSelfOrAdmin, signAccessToken } = require('../dist/middleware/auth');
const { createRateLimiter } = require('../dist/middleware/rate-limit');

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

test('accepts a valid access token and attaches the identity', () => {
  const token = signAccessToken({ id: 'person-1', email: 'teacher@example.test', rol: 'profesor' });
  const req = { headers: { authorization: `Bearer ${token}` } };
  const res = makeResponse();
  let continued = false;

  authMiddleware(req, res, () => { continued = true; });

  assert.equal(continued, true);
  assert.deepEqual(req.user, { id: 'person-1', email: 'teacher@example.test', rol: 'profesor' });
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
  const req = { user: { id: 'person-1', rol: 'padre' }, params: { id: 'person-2' } };
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
