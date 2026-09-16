import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

async function setup(page: Page, authenticated = true) {
  if (authenticated) await page.addInitScript(() => sessionStorage.setItem('vidshare.session', JSON.stringify({
    accessToken: 'upload-test-token', refreshToken: 'test', expiresAt: Date.now() + 3600000,
  })))
  await page.route('**/api/v1/**', route => {
    if (route.request().url().endsWith('/categories')) return route.fulfill({ json: [{ id: 3, name: '요리' }] })
    return route.fulfill({ json: { content: [], totalPages: 0 } })
  })
  await page.goto('/')
  await page.getByRole('link', { name: '영상 업로드' }).click()
}

async function fill(page: Page) {
  await page.getByLabel('영상 파일', { exact: true }).setInputFiles({ name: 'recipe.mp4', mimeType: 'video/mp4', buffer: Buffer.from('test video payload') })
  await page.getByLabel('제목', { exact: true }).fill('  파스타 만들기  ')
  await page.getByLabel('설명', { exact: true }).fill('맛있는 레시피')
  await page.getByLabel('카테고리', { exact: true }).selectOption('3')
  await page.getByLabel('비공개', { exact: true }).check()
}

test('upload sends authenticated multipart fields and thumbnail; 202 shows processing, not publication', async ({ page }) => {
  await setup(page)
  await fill(page)
  await page.getByLabel('썸네일 직접 업로드').setInputFiles({ name: 'cover.png', mimeType: 'image/png', buffer: Buffer.from('thumbnail payload') })
  let writes = 0
  await page.route('**/api/v1/videos', async route => {
    if (route.request().method() !== 'POST') return route.fallback()
    writes++
    const request = route.request()
    expect(request.headers().authorization).toBe('Bearer upload-test-token')
    expect(request.headers()['content-type']).toContain('multipart/form-data; boundary=')
    const body = request.postDataBuffer()!.toString()
    for (const field of ['title', 'description', 'categoryId', 'visibility', 'videoFile', 'thumbnailFile']) expect(body).toContain(`name="${field}"`)
    for (const value of ['파스타 만들기', '맛있는 레시피', 'PRIVATE', 'recipe.mp4', 'cover.png', 'test video payload', 'thumbnail payload']) expect(body).toContain(value)
    expect(body).toContain('name="categoryId"\r\n\r\n3')
    await route.fulfill({ status: 202, json: { videoId: 42, status: 'PROCESSING', createdAt: '2026-09-16T12:00:00' } })
  })
  await page.getByRole('button', { name: '업로드', exact: true }).click()
  await expect(page.getByRole('heading', { name: '업로드 완료! 영상 변환을 요청했습니다' })).toBeVisible()
  await expect(page.getByRole('link', { name: '업로드한 영상 보기' })).toHaveAttribute('href', '#/videos/42')
  expect(writes).toBe(1)
  await page.screenshot({ path: 'test-results/upload-complete.png', fullPage: true })
})

test('validation, server failure and retry preserve the form; optional thumbnail omitted', async ({ page }) => {
  await setup(page)
  await fill(page)
  await page.getByLabel('영상 파일', { exact: true }).setInputFiles({ name: 'bad.avi', mimeType: 'video/x-msvideo', buffer: Buffer.from('bad') })
  await expect(page.getByRole('alert')).toContainText('MP4·MOV·WebM')
  await page.getByLabel('썸네일 직접 업로드').setInputFiles({ name: 'bad.svg', mimeType: 'image/svg+xml', buffer: Buffer.from('<svg/>') })
  await expect(page.getByRole('alert')).toContainText('JPG·PNG·WebP')
  let attempts = 0
  await page.route('**/api/v1/videos', route => {
    expect(route.request().postDataBuffer()!.toString()).not.toContain('name="thumbnailFile"')
    attempts++
    return attempts === 1 ? route.fulfill({ status: 503, json: { message: '저장소에 연결할 수 없습니다.' } }) : route.fulfill({ status: 202, json: { videoId: 43, status: 'PROCESSING' } })
  })
  await page.getByRole('button', { name: '업로드', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('저장소에 연결할 수 없습니다.')
  await expect(page.getByLabel('제목', { exact: true })).toHaveValue('  파스타 만들기  ')
  await expect(page.getByText('recipe.mp4', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '업로드', exact: true }).click()
  await expect(page.getByRole('link', { name: '업로드한 영상 보기' })).toBeVisible()
})

test('in-flight cancellation restores the form and does not show completion', async ({ page }) => {
  await setup(page)
  await fill(page)
  let release!: () => void
  const pending = new Promise<void>(resolve => { release = resolve })
  await page.route('**/api/v1/videos', async route => {
    await pending
    await route.fulfill({ status: 202, json: { videoId: 44, status: 'PROCESSING' } }).catch(() => {})
  })
  await page.getByRole('button', { name: '업로드', exact: true }).click()
  await expect(page.getByRole('progressbar')).toBeVisible()
  await page.screenshot({ path: 'test-results/upload-progress.png', fullPage: true })
  await page.getByRole('button', { name: '취소', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('업로드 전송을 취소했습니다.')
  release()
  await expect(page.getByLabel('제목', { exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: '업로드한 영상 보기' })).toHaveCount(0)
})

test('authentication is required and expired sessions are cleared on upload 401', async ({ page }) => {
  await setup(page, false)
  await expect(page.getByText('영상 업로드는 로그인이 필요합니다.')).toBeVisible()
  await expect(page.getByLabel('영상 파일', { exact: true })).toHaveCount(0)
  await setup(page)
  await fill(page)
  await page.route('**/api/v1/videos', route => route.fulfill({ status: 401, json: { message: '인증 만료' } }))
  await page.getByRole('button', { name: '업로드', exact: true }).click()
  await expect(page.getByText('영상 업로드는 로그인이 필요합니다.')).toBeVisible()
  expect(await page.evaluate(() => sessionStorage.getItem('vidshare.session'))).toBeNull()
})

test('desktop and mobile upload forms fit the viewport', async ({ page }) => {
  await setup(page)
  await page.setViewportSize({ width: 1440, height: 1000 })
  await expect(page.getByLabel('카테고리', { exact: true })).toBeEnabled()
  await page.screenshot({ path: 'test-results/upload-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.getByRole('button', { name: '업로드', exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/upload-mobile.png', fullPage: true })
})
