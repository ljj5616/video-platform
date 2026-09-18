import { useState } from 'react'
import type { FormEvent } from 'react'
import { authRequest, setSession } from './session'
import './auth.css'

export function RecoveryForm({ mode, done }: { mode: 'find-id' | 'password-reset'; done: (message: string) => void }) {
  const findingId = mode === 'find-id'
  const [step, setStep] = useState<'request' | 'verify' | 'password' | 'result'>('request')
  const [identity, setIdentity] = useState({ name: '', phone: '', email: '' })
  const [code, setCode] = useState('')
  const [grant, setGrant] = useState({ resetToken: '', expiresAt: 0 })
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  async function sendCode() {
    await authRequest(findingId ? '/auth/find-id/code' : '/auth/password-reset/request', 'POST',
      findingId ? { name: identity.name.trim(), phone: identity.phone.replace(/[-\s]/g, '') } : { email: identity.email.trim() })
    setCode('')
    setStep('verify')
    setNotice(findingId ? '문자로 받은 인증번호 6자리를 입력해 주세요.' : '등록된 이메일이라면 인증번호가 발송됩니다. 받은편지함과 스팸함을 확인해 주세요.')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const data = new FormData(event.currentTarget)
    setError('')
    setBusy(true)
    try {
      if (step === 'request') await sendCode()
      else if (step === 'verify') {
        if (findingId) {
          const result = await authRequest<{ email: string }>('/auth/find-id/verify', 'POST', {
            name: identity.name.trim(), phone: identity.phone.replace(/[-\s]/g, ''), verificationCode: code,
          })
          setEmail(result.email)
          setNotice('')
          setStep('result')
        } else {
          const result = await authRequest<{ resetToken: string; expiresIn: number }>('/auth/password-reset/verify', 'POST', {
            email: identity.email.trim(), verificationCode: code,
          })
          setGrant({ resetToken: result.resetToken, expiresAt: Date.now() + result.expiresIn * 1000 })
          setCode('')
          setNotice('인증되었습니다. 새 비밀번호를 입력해 주세요.')
          setStep('password')
        }
      } else if (step === 'password') {
        if (Date.now() >= grant.expiresAt) {
          restart()
          throw new Error('재설정 유효 시간이 지났습니다. 인증번호를 다시 요청해 주세요.')
        }
        const newPassword = String(data.get('password') ?? '')
        if (newPassword !== data.get('confirm')) throw new Error('비밀번호가 일치하지 않습니다.')
        await authRequest('/auth/password-reset', 'PATCH', { resetToken: grant.resetToken, newPassword })
        setGrant({ resetToken: '', expiresAt: 0 })
        setSession(null)
        done('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.')
        window.location.hash = '/login'
      }
    } catch (cause) { showError(cause) }
    finally { setBusy(false) }
  }

  function showError(cause: unknown) {
    setError(cause instanceof Error && cause.name !== 'TimeoutError' && cause.name !== 'TypeError'
      ? cause.message : '서버에 연결하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.')
  }
  function restart() {
    setStep('request')
    setGrant({ resetToken: '', expiresAt: 0 })
    setCode('')
    setError('')
    setNotice('')
  }
  async function resend() {
    if (busy) return
    setBusy(true)
    setError('')
    setNotice('')
    try { await sendCode() } catch (cause) { showError(cause) }
    finally { setBusy(false) }
  }

  return <main id="main" className="auth-layout"><section className="auth-card">
    <h1>{findingId ? '아이디 찾기' : '비밀번호 재설정'}</h1>
    {notice && <p className="recovery-notice" role="status">{notice}</p>}
    {step === 'result' ? <div className="recovery-result" role="status"><p>가입하신 이메일 아이디입니다.</p><strong>{email}</strong><p>개인정보 보호를 위해 일부가 표시됩니다.</p></div> :
      <form onSubmit={submit}><fieldset disabled={busy}>
        {step === 'request' && (findingId ? <>
          <label>이름<input autoComplete="name" maxLength={50} required value={identity.name} onChange={event => setIdentity({ ...identity, name: event.target.value })} /></label>
          <label>전화번호<input type="tel" autoComplete="tel" pattern="01[016789]-?[0-9]{3,4}-?[0-9]{4}" placeholder="010-0000-0000" title="휴대전화 번호를 입력해 주세요." required value={identity.phone} onChange={event => setIdentity({ ...identity, phone: event.target.value })} /></label>
        </> : <label>이메일 주소<input type="email" autoComplete="email" maxLength={255} required value={identity.email} onChange={event => setIdentity({ ...identity, email: event.target.value })} /></label>)}
        {step === 'verify' && <label>인증번호<input inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} title="인증번호 6자리를 입력해 주세요." required value={code} onChange={event => setCode(event.target.value)} /></label>}
        {step === 'password' && <>
          <label>새 비밀번호<input name="password" type="password" autoComplete="new-password" minLength={8} maxLength={64} pattern={'(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,64}'} title="영문 대문자·소문자, 숫자, 특수문자를 포함한 8~64자" required /></label>
          <p className="field-help">영문 대문자·소문자, 숫자, 특수문자를 포함한 8~64자</p>
          <label>새 비밀번호 확인<input name="confirm" type="password" autoComplete="new-password" required /></label>
        </>}
        {error && <p className="auth-error" role="alert">{error}</p>}
        <button className="button primary auth-submit" type="submit">{busy ? '처리 중…' : step === 'request' ? '인증번호 받기' : step === 'verify' ? '인증번호 확인' : '비밀번호 변경'}</button>
        {step === 'verify' && <button className="button auth-submit" type="button" onClick={resend}>인증번호 재전송</button>}
        {step !== 'request' && <button className="button auth-submit" type="button" onClick={restart}>처음부터 다시 진행</button>}
      </fieldset></form>}
    <div className="auth-footer"><a href="#/login">로그인</a> · <a href={findingId ? '#/password-reset' : '#/find-id'}>{findingId ? '비밀번호 재설정' : '아이디 찾기'}</a></div>
  </section></main>
}
