import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

async function fixture(page: Page, authenticated = true) {
  const author = { userId: 7, nickname: '요리왕김셰프', profileImageUrl: null }
  const title = '나만의 파스타 레시피 공개! 초보자도 10분 완성 황금 조리 비율법'
  let liked = false
  let bookmarked = false
  const comments = [{ commentId: 1, content: '맛있게 만들어 봤습니다.', author, createdAt: '2026-09-15T09:00:00' }]
  const requests: string[] = []
  if (authenticated) await page.addInitScript(() => {
    sessionStorage.setItem('vidshare.session', JSON.stringify({ accessToken: `e30.${btoa('{"sub":"7"}')}.test`, refreshToken: 'test', expiresAt: Date.now() + 3600000 }))
  })
  await page.route('**/api/v1/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname.replace('/api/v1', '')
    requests.push(`${request.method()} ${path}`)
    if (request.method() !== 'GET') {
      expect(request.headers().authorization).toContain('Bearer ')
      if (path.endsWith('/likes')) liked = request.method() === 'POST'
      if (path.endsWith('/bookmarks')) bookmarked = request.method() === 'POST'
      if (path === '/videos/1/comments') comments.unshift({ commentId: 2, content: request.postDataJSON().content, author, createdAt: '2026-09-15T10:00:00' })
      if (path === '/comments/2' && request.method() === 'PATCH') comments[0].content = request.postDataJSON().content
      if (path === '/comments/2' && request.method() === 'DELETE') comments.shift()
      return route.fulfill({ status: request.method() === 'DELETE' ? 204 : 201 })
    }
    if (path === '/videos/1') return route.fulfill({ json: { videoId: 1, title, description: '집에서 레스토랑 수준의 정통 토마토 소스 파스타를 만들 수 있는 황금 레시피입니다.\n영상 속 재료와 조리 순서를 확인해 주세요.', thumbnailUrl: null, viewCount: 153000, categoryName: '요리', author, createdAt: '2026-03-15T12:00:00', likeCount: liked ? 1235 : 1234, liked, bookmarked } })
    if (path.endsWith('/playback')) return route.fulfill({ status: 409, json: { message: '영상 처리 중입니다.' } })
    if (path.endsWith('/comments')) return route.fulfill({ json: { content: url.searchParams.get('page') === '1' ? [] : comments, totalElements: comments.length, totalPages: 2 } })
    if (path === '/categories') return route.fulfill({ json: [{ id: 1, name: '요리' }] })
    if (path === '/videos/recommendations') return route.fulfill({ json: { content: [1, 2, 3].map(id => ({ id, title: id === 1 ? title : '초보자도 쉽게 만드는 집밥 레시피', uploaderNickname: author.nickname, viewCount: 12500, duration: 624, thumbnailUrl: null })), totalPages: 1 } })
    return route.fulfill({ json: { content: [{ videoId: 1, title, channelName: author.nickname, viewCount: 153000, thumbnailUrl: null }], totalPages: 1 } })
  })
  return requests
}

test('home navigation, reaction state, empty 201 and comment CRUD', async ({ page }) => {
  const requests = await fixture(page)
  await page.goto('/')
  await page.locator('.video-card a').first().click()
  await expect(page).toHaveURL(/#\/videos\/1$/)
  await expect(page.locator('h1')).toContainText('파스타')
  await expect(page.getByText('영상 처리 중입니다.')).toBeVisible()
  const like = page.getByRole('button', { name: /좋아요/ })
  await like.click()
  await expect(like).toHaveAttribute('aria-pressed', 'true')
  await expect(like).toContainText('1,235')
  await like.click()
  await expect(like).toHaveAttribute('aria-pressed', 'false')
  await page.getByRole('button', { name: /북마크/ }).click()
  await expect(page.getByRole('button', { name: /북마크/ })).toHaveAttribute('aria-pressed', 'true')
  await page.getByRole('textbox', { name: '댓글', exact: true }).fill('새 댓글입니다')
  await page.getByRole('button', { name: '댓글 등록' }).click()
  const row = page.locator('.comment-row').filter({ hasText: '새 댓글입니다' })
  await expect(row).toBeVisible()
  await row.locator('summary').click()
  await row.getByRole('button', { name: '수정', exact: true }).click()
  await page.getByLabel('댓글 수정').fill('수정된 댓글입니다')
  await page.getByRole('button', { name: '저장', exact: true }).click()
  const edited = page.locator('.comment-row').filter({ hasText: '수정된 댓글입니다' })
  await expect(edited).toBeVisible()
  await edited.locator('summary').click()
  await edited.getByRole('button', { name: '삭제', exact: true }).click()
  await edited.locator('.delete-confirm').getByRole('button', { name: '삭제', exact: true }).click()
  await expect(edited).toHaveCount(0)
  await page.getByRole('button', { name: '⚑ 신고', exact: true }).click()
  await page.getByLabel('신고 사유').selectOption('SPAM')
  await page.getByRole('button', { name: '신고 제출' }).click()
  await expect(page.getByText('신고가 접수되었습니다.')).toBeVisible()
  expect(requests).toContain('POST /videos/1/reports')
  await page.getByRole('button', { name: '다음', exact: true }).click()
  await expect(page.locator('.pagination')).toContainText('2 / 2')
})

test('guest actions do not send writes; mobile layout fits viewport', async ({ page }) => {
  const requests = await fixture(page, false)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/#/videos/1')
  await page.getByRole('button', { name: /좋아요/ }).click()
  await expect(page.getByText('로그인이 필요한 기능입니다.')).toBeVisible()
  await expect(page.getByRole('button', { name: '댓글 등록' })).toBeDisabled()
  expect(requests.every(request => request.startsWith('GET'))).toBe(true)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/detail-mobile.png', fullPage: true })
})

test('detail failure can be retried; desktop layout', async ({ page }) => {
  await fixture(page)
  let fail = true
  await page.route('**/api/v1/videos/1', async route => {
    if (fail) return route.fulfill({ status: 404, json: { message: '영상을 찾을 수 없습니다.' } })
    return route.fallback()
  })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('/#/videos/1')
  await expect(page.getByText('영상을 찾을 수 없습니다.')).toBeVisible()
  fail = false
  await page.getByRole('button', { name: '다시 시도' }).click()
  await expect(page.locator('h1')).toContainText('파스타')
  await page.screenshot({ path: 'test-results/detail-desktop.png', fullPage: true })
})

test('playback receives source, records a view once and survives a reaction refresh', async ({ page }) => {
  const requests = await fixture(page)
  await page.route('**/api/v1/videos/1/playback', route => route.fulfill({ json: {
    playbackUrl: '/test-video.webm', mediaType: 'video/webm', duration: 60,
  } }))
  await page.route('**/test-video.webm', route => route.fulfill({ status: 200, contentType: 'video/webm', body: '' }))
  await page.goto('/#/videos/1')
  const player = page.locator('video')
  await expect(player).toHaveAttribute('src', '/test-video.webm')
  await player.evaluate(video => { video.dataset.testIdentity = 'original'; video.dispatchEvent(new Event('playing')) })
  await expect.poll(() => requests.filter(request => request === 'POST /videos/1/views').length).toBe(1)
  await page.getByRole('button', { name: /좋아요/ }).click()
  await expect(page.getByRole('button', { name: /좋아요/ })).toHaveAttribute('aria-pressed', 'true')
  await expect(player).toHaveAttribute('data-test-identity', 'original')
  await player.dispatchEvent('playing')
  expect(requests.filter(request => request === 'POST /videos/1/views')).toHaveLength(1)
})
