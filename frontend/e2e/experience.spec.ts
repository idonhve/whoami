import { expect, test, type Page } from '@playwright/test'

/**
 * /experience 经历页 E2E（Spec 09 验收）。
 * 前置：全栈已起且 experience 表有 ≥2 条经历（docs/content 种子）。
 * 桌面：滚动驱动主线逐段点亮 + 当前卡片高亮；移动端：单列卡片、主线细化、无横向滚动。
 * 同一用例在 desktop/mobile 两个 project 各跑一次，按视口分支断言。
 */

const TIMELINE = '[data-testid="experience-timeline"]'

async function gotoExperience(page: Page) {
  await page.goto('/experience')
  await expect(page.locator(TIMELINE)).toBeVisible({ timeout: 10000 })
  await expect(page.locator('.card-slot').first()).toBeVisible()
}

test('桌面：滚动驱动主线点亮 + 当前卡片高亮', async ({ page }) => {
  const vp = page.viewportSize()
  test.skip(vp === null || vp.width < 721, '由移动端用例覆盖')

  await gotoExperience(page)
  const count = await page.locator('.card-slot').count()
  expect(count).toBeGreaterThanOrEqual(2)

  const lastSlot = page.locator('.card-slot').nth(count - 1)
  await lastSlot.scrollIntoViewIfNeeded()

  await expect(lastSlot.locator('.exp-card')).toHaveClass(/--active/, { timeout: 6000 })
  await expect(page.locator('.segment').first()).toHaveClass(/--lit/, { timeout: 6000 })
  await expect(page.locator(TIMELINE)).toHaveAttribute('data-active-index', String(count - 1))
})

test('移动端：单列卡片、主线细化、无横向滚动', async ({ page }) => {
  const vp = page.viewportSize()
  test.skip(vp !== null && vp.width >= 721, '由桌面用例覆盖')

  await gotoExperience(page)

  // 单列：卡片容器纵向排布
  const cardsFlex = await page
    .locator('.cards')
    .evaluate((el) => getComputedStyle(el).flexDirection)
  expect(cardsFlex).toBe('column')

  // 移动主线细化为细线（≤2px）
  const railWidth = await page.locator('.rail').evaluate((el) => getComputedStyle(el).width)
  expect(Number.parseInt(railWidth, 10)).toBeLessThanOrEqual(2)

  // 无横向溢出
  const noHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth <= window.innerWidth + 1,
  )
  expect(noHorizontalOverflow).toBe(true)
})
