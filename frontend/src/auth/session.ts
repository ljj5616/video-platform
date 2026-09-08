import { useSyncExternalStore } from 'react'

export interface Session { accessToken: string; refreshToken: string; expiresAt: number }
const storageKey = 'vidshare.session'
function read(): Session | null {
  try {
    const value = JSON.parse(sessionStorage.getItem(storageKey) ?? 'null') as Session | null
    return value && typeof value.accessToken === 'string' && typeof value.refreshToken === 'string'
      && Number.isFinite(value.expiresAt) && value.expiresAt > Date.now() ? value : null
  } catch { return null }
}
let session = read()
const listeners = new Set<() => void>()
export function setSession(value: Session | null) {
  session = value
  try {
    if (value) sessionStorage.setItem(storageKey, JSON.stringify(value))
    else sessionStorage.removeItem(storageKey)
  } catch { /* 브라우저 저장이 제한되어도 현재 탭에서는 사용할 수 있습니다. */ }
  listeners.forEach(listener => listener())
}
export function getSession() {
  if (session && session.expiresAt <= Date.now()) setSession(null)
  return session
}
function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}
export function useSession() {
  return useSyncExternalStore(subscribe, () => session)
}
window.setInterval(() => { getSession() }, 1000)

export async function authRequest<T>(path: string, method: string, body: unknown, token?: string): Promise<T> {
  const response = await fetch('/api/v1' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(15000),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null
    if (response.status === 401 && token && getSession()?.accessToken === token) setSession(null)
    throw new Error(error?.message ?? '요청에 실패했습니다. 잠시 후 다시 시도해 주세요.')
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
