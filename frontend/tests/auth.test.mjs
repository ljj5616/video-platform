import { test } from 'node:test'
import assert from 'node:assert/strict'

const storage = new Map()
globalThis.sessionStorage = {
  getItem: key => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, value),
  removeItem: key => storage.delete(key),
}
globalThis.window = { setInterval: () => 0 }
const { setSession, getSession, authRequest } = await import('../src/auth/session.ts')

test('session survives storage, expires and is removed', () => {
  const value = { accessToken: 'access', refreshToken: 'refresh', expiresAt: Date.now() + 60000 }
  setSession(value)
  assert.equal(getSession(), value)
  assert.equal(JSON.parse(storage.get('vidshare.session')).accessToken, 'access')
  setSession({ ...value, expiresAt: Date.now() - 1 })
  assert.equal(getSession(), null)
  assert.equal(storage.has('vidshare.session'), false)
})

test('withdrawal sends password body and bearer token, handles 204', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/v1/users/me')
    assert.equal(options.method, 'DELETE')
    assert.equal(options.headers.Authorization, 'Bearer access')
    assert.deepEqual(JSON.parse(options.body), { password: 'example' })
    return new Response(null, { status: 204 })
  }
  assert.equal(await authRequest('/users/me', 'DELETE', { password: 'example' }, 'access'), undefined)
})

test('unauthorized authenticated request clears the matching session', async () => {
  setSession({ accessToken: 'access', refreshToken: 'refresh', expiresAt: Date.now() + 60000 })
  globalThis.fetch = async () => new Response(JSON.stringify({ message: '인증 만료' }), { status: 401 })
  await assert.rejects(authRequest('/users/me', 'DELETE', {}, 'access'), /인증 만료/)
  assert.equal(getSession(), null)
})

test('failed login presents API error', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({ message: '이메일 또는 비밀번호가 올바르지 않습니다.' }), { status: 400 })
  await assert.rejects(authRequest('/auth/login', 'POST', {}), /이메일 또는 비밀번호/)
})
