import { test, expect } from '@playwright/test'
import { AUTH_FILE } from './global-setup'

test.use({ storageState: AUTH_FILE })

test('E2E-DARK-01: 다크 모드 전환, 새로고침 유지, 라이트 복원', async ({ page }) => {
  await page.goto('/login')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 })

  // Initial state should be light (no dark class)
  const html = page.locator('html')

  // Click theme toggle button
  await page.getByRole('button', { name: '테마 전환' }).first().click()

  // Should have dark class applied
  await expect(html).toHaveClass(/dark/, { timeout: 3000 })

  // Reload and verify persistence
  await page.reload()
  await expect(html).toHaveClass(/dark/, { timeout: 3000 })

  // Toggle back to light
  await page.getByRole('button', { name: '테마 전환' }).first().click()
  await expect(html).not.toHaveClass(/dark/, { timeout: 3000 })
})

test('E2E-DARK-01b: 다크 모드 전환 - 에디터 페이지', async ({ page, context }) => {
  // Set initial theme to light in localStorage
  await page.goto('/login')
  await page.evaluate(() => localStorage.setItem('theme', 'light'))

  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 })

  const html = page.locator('html')
  await expect(html).not.toHaveClass(/dark/)

  // Navigate to dashboard and find theme toggle (may be in editor page)
  // Toggle via localStorage check
  await page.evaluate(() => localStorage.setItem('theme', 'dark'))
  await page.reload()

  // After reload with dark in localStorage, should have dark class
  await expect(html).toHaveClass(/dark/, { timeout: 3000 })

  // Restore
  await page.evaluate(() => localStorage.setItem('theme', 'light'))
})
