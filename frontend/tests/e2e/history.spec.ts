import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

async function setup(page: Page, authenticated = true) {
  if (authenticated) await page.addInitScript(() => sessionStorage.setItem('vidshare.session', JSON.stringify({ accessToken: 'history-token', refreshToken: 'refresh', expiresAt: Date.now() + 3600000 })))
  await page.route('**/api/v1/**', route => route.fulfill({ json: route.request().url().endsWith('/categories') ? [] : { content: [], totalPages: 0 } }))
}
const item = { videoId: 42, title: '시청한 영상', thumbnailUrl: null, duration: 120, positionSeconds: 30, progressPercent: 25, lastWatchedAt: '2026-09-19T12:00:00', author: { nickname: '작가' } }

test('history paginates, retries deletion, and returns from emptied last page', async ({ page }) => {
  await setup(page)
  let deleted = false
  let fail = true
  await page.route('**/api/v1/users/me/watch-history**', route => {
    expect(route.request().headers().authorization).toBe('Bearer history-token')
    if (route.request().method() === 'DELETE') {
      expect(route.request().url()).toContain('/watch-history/42')
      if (fail) return route.fulfill({ status: 503, json: { message: '삭제 실패' } })
      deleted = true
      return route.fulfill({ status: 204 })
    }
    const index = Number(new URL(route.request().url()).searchParams.get('page'))
    return route.fulfill({ json: { content: [index === 1 ? item : { ...item, videoId: 1, title: '첫 페이지 영상' }], totalElements: deleted ? 12 : 13, totalPages: deleted ? 1 : 2 } })
  })
  await page.goto('/')
  await page.getByText('내 계정', { exact: true }).click()
  await page.getByRole('link', { name: '시청 기록', exact: true }).click()
  await page.getByRole('button', { name: '다음', exact: true }).click()
  await expect(page.getByText('0:30 / 2:00 · 25% 시청')).toBeVisible()
  await expect(page.locator('.video-card a')).toHaveAttribute('href', '#/videos/42')
  await page.getByRole('button', { name: '시청한 영상 시청 기록 삭제' }).click()
  await expect(page.getByRole('status')).toHaveText('삭제 실패')
  fail = false
  await page.getByRole('button', { name: '시청한 영상 시청 기록 삭제' }).click()
  await expect(page.getByRole('heading', { name: '첫 페이지 영상' })).toBeVisible()
  expect(deleted).toBe(true)
})

test('history error retry and empty state', async ({ page }) => {
  await setup(page)
  let fail = true
  await page.route('**/api/v1/users/me/watch-history?*', route => fail ? route.fulfill({ status: 503, json: { message: '조회 실패' } }) : route.fulfill({ json: { content: [], totalPages: 0, totalElements: 0 } }))
  await page.goto('/#/me/watch-history')
  await expect(page.getByRole('alert')).toContainText('조회 실패')
  fail = false
  await page.getByRole('button', { name: '다시 시도' }).click()
  await expect(page.getByRole('heading', { name: '아직 시청 기록이 없습니다.' })).toBeVisible()
})

for (const authenticated of [true, false]) {
  test(`progress periodic, pause and navigation writes: authenticated=${authenticated}`, async ({ page }) => {
    await setup(page, authenticated)
    await page.route('**/api/v1/videos/42', route => route.fulfill({ json: { ...item, viewCount: 1, categoryName: '요리', author: { userId: 7, nickname: '작가' }, createdAt: item.lastWatchedAt, likeCount: 0, liked: false, bookmarked: false } }))
    await page.route('**/api/v1/videos/42/playback', route => route.fulfill({ json: { playbackUrl: '/test.mp4', mediaType: 'video/mp4', duration: 120 } }))
    await page.route('**/test.mp4', route => route.fulfill({ status: 204 }))
    const positions: number[] = []
    await page.route('**/api/v1/videos/42/progress', route => {
      expect(route.request().method()).toBe('PUT')
      expect(route.request().headers().authorization).toBe('Bearer history-token')
      positions.push(route.request().postDataJSON().positionSeconds)
      return route.fulfill({ status: 204 })
    })
    await page.goto('/#/videos/42')
    await expect(page.locator('video')).toBeVisible()
    await page.clock.install()
    await page.locator('video').evaluate(video => {
      Object.defineProperty(video, 'currentTime', { configurable: true, value: 20.9 })
      video.dispatchEvent(new Event('playing'))
    })
    await page.clock.fastForward(11000)
    await page.locator('video').dispatchEvent('timeupdate')
    if (authenticated) await expect.poll(() => positions).toEqual([20])
    await page.locator('video').evaluate(video => {
      Object.defineProperty(video, 'currentTime', { configurable: true, value: 35.8 })
      video.dispatchEvent(new Event('pause'))
    })
    if (authenticated) await expect.poll(() => positions).toEqual([20, 35])
    await page.locator('video').evaluate(video => Object.defineProperty(video, 'currentTime', { configurable: true, value: 45 }))
    await page.getByRole('link', { name: '← 영상 목록' }).click()
    await expect(page).toHaveURL(/#\/$/)
    if (authenticated) await expect.poll(() => positions).toEqual([20, 35, 45])
    else expect(positions).toEqual([])
  })
}

test('guest history does not request private data', async ({ page }) => {
  await setup(page, false)
  let calls = 0
  await page.route('**/api/v1/users/me/watch-history**', route => { calls++; return route.fulfill({ status: 401 }) })
  await page.goto('/#/me/watch-history')
  await expect(page.getByText('로그인 후 시청 기록을 확인할 수 있습니다.')).toBeVisible()
  expect(calls).toBe(0)
})
