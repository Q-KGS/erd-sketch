import { test, expect } from '@playwright/test'
import { AUTH_FILE } from './global-setup'
import { createEditorPageWithIds } from './helpers'

test.use({ storageState: AUTH_FILE })

test('E2E-DARK-01: 다크 모드 전환, 새로고침 유지, 라이트 복원', async ({ page }) => {
  await createEditorPageWithIds(page)

  const html = page.locator('html')

  // Ensure light mode initially (clear any persisted theme)
  await page.evaluate(() => localStorage.removeItem('theme'))
  await page.reload()
  await page.waitForSelector('.react-flow', { timeout: 15000 })

  // Click theme toggle button
  await page.getByRole('button', { name: '테마 전환' }).first().click()

  // Should have dark class applied
  await expect(html).toHaveClass(/dark/, { timeout: 3000 })

  // Reload and verify persistence
  await page.reload()
  await page.waitForSelector('.react-flow', { timeout: 15000 })
  await expect(html).toHaveClass(/dark/, { timeout: 3000 })

  // Toggle back to light
  await page.getByRole('button', { name: '테마 전환' }).first().click()
  await expect(html).not.toHaveClass(/dark/, { timeout: 3000 })
})

test('E2E-DARK-01b: localStorage로 다크 모드 직접 설정 및 복원', async ({ page }) => {
  await createEditorPageWithIds(page)

  const html = page.locator('html')

  // Set dark mode via localStorage
  await page.evaluate(() => localStorage.setItem('theme', 'dark'))
  await page.reload()
  await page.waitForSelector('.react-flow', { timeout: 15000 })

  // After reload with dark in localStorage, should have dark class
  await expect(html).toHaveClass(/dark/, { timeout: 3000 })

  // Restore to light
  await page.evaluate(() => localStorage.setItem('theme', 'light'))
  await page.reload()
  await page.waitForSelector('.react-flow', { timeout: 15000 })
  await expect(html).not.toHaveClass(/dark/, { timeout: 3000 })
})
