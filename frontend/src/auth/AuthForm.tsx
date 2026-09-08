import { useState } from 'react'
import type { FormEvent } from 'react'
import { authRequest, getSession, setSession } from './session'
import './auth.css'

export type AuthPage = 'login' | 'signup' | 'withdraw'
interface LoginResponse { accessToken: string; refreshToken: string; expiresIn: number }

export function AuthForm({ page, done }: { page: AuthPage; done: (message: string) => void }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const data = new FormData(event.currentTarget)
    const text = (key: string) => String(data.get(key) ?? '')
    const password = text('password')
    if (page === 'signup' && password !== text('confirm')) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    setBusy(true)
    setError('')
    try {
      if (page === 'signup') {
        await authRequest('/users', 'POST', {
          email: text('email').trim(), password, nickname: text('nickname').trim(),
          name: text('name').trim(), phone: text('phone').replace(/[-\s]/g, ''),
        })
        done('회원가입이 완료되었습니다. 로그인해 주세요.')
        window.location.hash = '/login'
      } else if (page === 'login') {
        const result = await authRequest<LoginResponse>('/auth/login', 'POST', { email: text('email').trim(), password })
        setSession({ accessToken: result.accessToken, refreshToken: result.refreshToken, expiresAt: Date.now() + result.expiresIn * 1000 })
        done('로그인되었습니다.')
        window.location.hash = '/'
      } else {
        const current = getSession()
        if (!current) throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.')
        await authRequest('/users/me', 'DELETE', { password }, current.accessToken)
        setSession(null)
        done('회원탈퇴가 완료되었습니다.')
        window.location.hash = '/'
      }
    } catch (cause) {
      setError(cause instanceof Error && cause.name !== 'TimeoutError' && cause.name !== 'TypeError'
        ? cause.message : '서버에 연결하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.')
    } finally { setBusy(false) }
  }
  return <main id="main" className="auth-layout"><section className="auth-card">
    <h1>VIDSHARE {page === 'signup' ? '회원가입' : page === 'withdraw' ? '회원탈퇴' : '로그인'}</h1>
    <form onSubmit={submit}>
      <fieldset disabled={busy}>
        {page !== 'withdraw' && <label>이메일 주소<input name="email" type="email" autoComplete="email" maxLength={255} placeholder="example@email.com" required /></label>}
        {page === 'signup' && <>
          <label>닉네임<input name="nickname" autoComplete="nickname" pattern="[가-힣A-Za-z0-9_]{2,20}" title="한글, 영문, 숫자, 밑줄 2~20자" placeholder="한글/영문/숫자/밑줄 2~20자" required /></label>
          <label>이름<input name="name" autoComplete="name" maxLength={50} required /></label>
          <label>전화번호<input name="phone" type="tel" autoComplete="tel" pattern="01[016789]-?[0-9]{3,4}-?[0-9]{4}" placeholder="010-0000-0000" title="휴대전화 번호를 입력해 주세요." required /></label>
        </>}
        {page === 'withdraw' && <p className="withdraw-warning">탈퇴하면 계정을 사용할 수 없습니다. 본인 확인을 위해 현재 비밀번호를 입력해 주세요.</p>}
        <label>{page === 'withdraw' ? '현재 비밀번호' : '비밀번호'}<input name="password" type="password" autoComplete={page === 'signup' ? 'new-password' : 'current-password'} required
          {...(page === 'signup' ? { minLength: 8, maxLength: 64, pattern: '(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,64}', title: '영문 대문자·소문자, 숫자, 특수문자를 포함한 8~64자' } : {})} /></label>
        {page === 'signup' && <><p className="field-help">영문 대문자·소문자, 숫자, 특수문자를 포함한 8~64자</p><label>비밀번호 확인<input name="confirm" type="password" autoComplete="new-password" required /></label></>}
        {page === 'withdraw' && <label className="confirmation"><input type="checkbox" required />회원탈퇴를 진행하는 데 동의합니다.</label>}
        {error && <p className="auth-error" role="alert">{error}</p>}
        <button className="button primary auth-submit" type="submit">{busy ? '처리 중…' : page === 'signup' ? '회원가입 완료' : page === 'withdraw' ? '회원탈퇴' : '로그인'}</button>
      </fieldset>
    </form>
    <div className="auth-footer">{page === 'login' ? <>계정이 없으신가요? <a href="#/signup">회원가입</a></> : page === 'signup' ? <>이미 계정이 있으신가요? <a href="#/login">로그인</a></> : <a href="#/">취소하고 홈으로</a>}</div>
  </section></main>
}
