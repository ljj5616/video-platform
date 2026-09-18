import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/**', route => route.fulfill({ json: route.request().url().endsWith('/categories') ? [] : { content: [], totalPages: 0 } }))
})

test('find ID normalizes identity, supports resend and verification retry, and displays masked email', async ({ page }) => {
  let sends = 0
  let verifies = 0
  await page.route('**/api/v1/auth/find-id/*', async route => {
    const body = route.request().postDataJSON()
    expect(body.name).toBe('홍길동')
    expect(body.phone).toBe('01012345678')
    expect(route.request().method()).toBe('POST')
    if (route.request().url().endsWith('/code')) {
      sends++
      return route.fulfill({ status: 204 })
    }
    expect(body.verificationCode).toBe('123456')
    verifies++
    return verifies === 1
      ? route.fulfill({ status: 400, json: { message: '인증번호가 일치하지 않습니다.' } })
      : route.fulfill({ json: { email: 'ho***@example.com' } })
  })
  await page.goto('/#/login')
  await page.getByRole('link', { name: '아이디 찾기', exact: true }).click()
  await page.getByLabel('이름', { exact: true }).fill(' 홍길동 ')
  await page.getByLabel('전화번호').fill('010-1234-5678')
  await page.getByRole('button', { name: '인증번호 받기' }).click()
  await page.getByRole('button', { name: '인증번호 재전송' }).click()
  await page.getByLabel('인증번호', { exact: true }).fill('123456')
  await page.getByRole('button', { name: '인증번호 확인' }).click()
  await expect(page.getByRole('alert')).toHaveText('인증번호가 일치하지 않습니다.')
  await page.getByRole('button', { name: '인증번호 확인' }).click()
  await expect(page.getByText('ho***@example.com', { exact: true })).toBeVisible()
  expect(sends).toBe(2)
})

test('password reset sends token and new password only after confirmation matches', async ({ page }) => {
  let updates = 0
  await page.route('**/api/v1/auth/password-reset**', route => {
    const body = route.request().postDataJSON()
    if (route.request().url().endsWith('/request')) {
      expect(body).toEqual({ email: 'user@example.com' })
      return route.fulfill({ status: 204 })
    }
    if (route.request().url().endsWith('/verify')) {
      expect(body).toEqual({ email: 'user@example.com', verificationCode: '012345' })
      return route.fulfill({ json: { resetToken: 'private-reset-token', expiresIn: 600 } })
    }
    expect(route.request().method()).toBe('PATCH')
    expect(body).toEqual({ resetToken: 'private-reset-token', newPassword: 'NewPassword1!' })
    updates++
    return route.fulfill({ status: 204 })
  })
  await page.goto('/#/login')
  await page.getByRole('link', { name: '비밀번호 재설정', exact: true }).click()
  await expect(page.getByRole('heading', { name: '비밀번호 재설정', exact: true })).toBeVisible()
  await page.getByLabel('이메일 주소').fill('user@example.com')
  await page.getByRole('button', { name: '인증번호 받기' }).click()
  await expect(page.getByRole('status')).toContainText('등록된 이메일이라면')
  await page.getByLabel('인증번호', { exact: true }).fill('012345')
  await page.getByRole('button', { name: '인증번호 확인' }).click()
  await page.getByLabel('새 비밀번호', { exact: true }).fill('NewPassword1!')
  await page.getByLabel('새 비밀번호 확인').fill('Different1!')
  await page.getByRole('button', { name: '비밀번호 변경', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('비밀번호가 일치하지 않습니다.')
  expect(updates).toBe(0)
  expect(await page.evaluate(() => JSON.stringify(sessionStorage))).not.toContain('private-reset-token')
  await page.getByLabel('새 비밀번호 확인').fill('NewPassword1!')
  await page.getByRole('button', { name: '비밀번호 변경', exact: true }).click()
  await expect(page).toHaveURL(/#\/login$/)
  await expect(page.getByRole('status')).toContainText('비밀번호가 변경되었습니다.')
  expect(updates).toBe(1)
})

test('expired reset grant returns to request without updating password', async ({ page }) => {
  let updates = 0
  await page.route('**/api/v1/auth/password-reset**', route => {
    if (route.request().url().endsWith('/request')) return route.fulfill({ status: 204 })
    if (route.request().url().endsWith('/verify')) return route.fulfill({ json: { resetToken: 'expired', expiresIn: 0 } })
    updates++
    return route.fulfill({ status: 204 })
  })
  await page.goto('/#/password-reset')
  await page.getByLabel('이메일 주소').fill('user@example.com')
  await page.getByRole('button', { name: '인증번호 받기' }).click()
  await page.getByLabel('인증번호', { exact: true }).fill('123456')
  await page.getByRole('button', { name: '인증번호 확인' }).click()
  await page.getByLabel('새 비밀번호', { exact: true }).fill('NewPassword1!')
  await page.getByLabel('새 비밀번호 확인').fill('NewPassword1!')
  await page.getByRole('button', { name: '비밀번호 변경', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('유효 시간이 지났습니다')
  await expect(page.getByRole('button', { name: '인증번호 받기' })).toBeVisible()
  expect(updates).toBe(0)
})
