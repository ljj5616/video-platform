import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

async function setup(page: Page, authenticated = true) {
  if (authenticated) await page.addInitScript(() => sessionStorage.setItem('vidshare.session', JSON.stringify({ accessToken: 'saved-token', refreshToken: 'refresh', expiresAt: Date.now() + 3600000 })))
  await page.route('**/api/v1/**', route => route.fulfill({ json: route.request().url().endsWith('/categories') ? [] : { content: [], totalPages: 0 } }))
}

for (const [kind, title] of [['likes', '좋아요한 영상'], ['bookmarks', '북마크한 영상']] as const) {
  test(`${kind}: authenticated list, pagination, and detail navigation`, async ({ page }) => {
    await setup(page)
    const pages: string[] = []
    await page.route(`**/api/v1/users/me/${kind}?*`, route => {
      expect(route.request().method()).toBe('GET')
      expect(route.request().headers().authorization).toBe('Bearer saved-token')
      const params = new URL(route.request().url()).searchParams
      expect(params.get('size')).toBe('12')
      pages.push(params.get('page')!)
      const index = Number(params.get('page'))
      return route.fulfill({ json: { content: [{ videoId: 42 + index, title: `저장한 영상 ${index + 1}`, thumbnailUrl: null, duration: 125, viewCount: 321, author: { userId: 7, nickname: '영상작가' } }], page: index, totalPages: 2, totalElements: 13 } })
    })
    await page.goto('/')
    await page.getByText('내 계정', { exact: true }).click()
    await page.locator('.account-actions').getByRole('link', { name: title }).click()
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible()
    await expect(page.getByRole('heading', { name: '저장한 영상 1' })).toBeVisible()
    await expect(page.getByText('영상작가', { exact: true })).toBeVisible()
    await expect(page.getByText('2:05', { exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: '이전 페이지' })).toBeDisabled()
    await page.getByRole('button', { name: '다음 페이지' }).click()
    await expect(page.getByRole('heading', { name: '저장한 영상 2' })).toBeVisible()
    await expect(page.getByRole('button', { name: '다음 페이지' })).toBeDisabled()
    await page.getByRole('button', { name: '이전 페이지' }).click()
    await expect(page.getByRole('heading', { name: '저장한 영상 1' })).toBeVisible()
    expect(pages.filter((value, index) => value !== pages[index - 1])).toEqual(['0', '1', '0'])
    await page.locator('.video-card a').click()
    await expect(page).toHaveURL(/#\/videos\/42$/)
  })
}

test('server error can retry; switching lists shows empty state', async ({ page }) => {
  await setup(page)
  let failing = true
  await page.route('**/api/v1/users/me/likes?*', route => failing
    ? route.fulfill({ status: 503, json: { message: '목록 조회에 실패했습니다.' } })
    : route.fulfill({ json: { content: [], totalPages: 0, totalElements: 0, page: 0 } }))
  await page.route('**/api/v1/users/me/bookmarks?*', route => route.fulfill({ json: { content: [], totalPages: 0, totalElements: 0, page: 0 } }))
  await page.goto('/#/me/likes')
  await expect(page.getByRole('alert')).toContainText('목록 조회에 실패했습니다.')
  failing = false
  await page.getByRole('button', { name: '다시 시도' }).click()
  await expect(page.getByRole('heading', { name: '아직 좋아요한 영상이 없습니다.' })).toBeVisible()
  await page.getByRole('navigation', { name: '내 영상 목록', exact: true }).getByRole('link', { name: '북마크한 영상' }).click()
  await expect(page.getByRole('heading', { name: '아직 북마크한 영상이 없습니다.' })).toBeVisible()
})

test('anonymous visitors do not request private lists', async ({ page }) => {
  await setup(page, false)
  let requests = 0
  await page.route('**/api/v1/users/me/**', route => { requests++; return route.fulfill({ status: 401 }) })
  await page.goto('/#/me/likes')
  await expect(page.getByText('로그인 후 좋아요한 영상을 확인할 수 있습니다.')).toBeVisible()
  await page.getByRole('navigation', { name: '내 영상 목록', exact: true }).getByRole('link', { name: '북마크한 영상' }).click()
  await expect(page.getByText('로그인 후 북마크한 영상을 확인할 수 있습니다.')).toBeVisible()
  expect(requests).toBe(0)
})

test('401 clears session and replaces private list with login guidance', async ({ page }) => {
  await setup(page)
  await page.route('**/api/v1/users/me/likes?*', route => route.fulfill({ status: 401 }))
  await page.goto('/#/me/likes')
  await expect(page.getByText('로그인 후 좋아요한 영상을 확인할 수 있습니다.')).toBeVisible()
  expect(await page.evaluate(() => sessionStorage.getItem('vidshare.session'))).toBeNull()
})
