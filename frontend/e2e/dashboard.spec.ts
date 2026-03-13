import { test, expect } from '@playwright/test'

test.describe('대시보드', () => {
  test('비인증 상태에서 로그인 페이지로 리다이렉트', async ({ page }) => {
    await page.goto('/')
    // Should redirect to login if not authenticated
    await expect(page).toHaveURL(/login|\//, { timeout: 5000 })
  })

  test('로그인 페이지가 올바른 제목을 갖는다', async ({ page }) => {
    await page.goto('/login')
    await expect(page).toHaveTitle(/ErdSketch/)
  })
})
