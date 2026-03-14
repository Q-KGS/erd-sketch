import { test, expect } from '@playwright/test'
import { AUTH_FILE } from './global-setup'
import { createEditorPageWithIds } from './helpers'

test.use({ storageState: AUTH_FILE })

test('E2E-TPL-01: 이커머스 템플릿 적용 → 5개 테이블 + 4개 관계', async ({ page }) => {
  await createEditorPageWithIds(page)

  // Open template picker
  await page.getByRole('button', { name: '템플릿' }).click()
  await expect(page.getByRole('heading', { name: '프로젝트 템플릿' })).toBeVisible()

  // Wait for templates to load
  await expect(page.getByText('이커머스')).toBeVisible({ timeout: 5000 })

  // Select e-commerce template
  await page.getByText('이커머스').click()

  // Apply template
  await page.getByRole('button', { name: '적용' }).click()

  // Wait for toast
  await expect(page.getByText('템플릿이 적용되었습니다')).toBeVisible({ timeout: 8000 })

  // Verify 5 tables (nodes) added
  await expect(page.locator('.react-flow__node')).toHaveCount(5, { timeout: 8000 })

  // Verify 4 relationships (edges)
  await expect(page.locator('.react-flow__edge')).toHaveCount(4, { timeout: 5000 })
})

test('E2E-TPL-02: 블로그 템플릿 적용 → 5개 테이블', async ({ page }) => {
  await createEditorPageWithIds(page)

  await page.getByRole('button', { name: '템플릿' }).click()
  await expect(page.getByText('블로그')).toBeVisible({ timeout: 5000 })
  await page.getByText('블로그').click()
  await page.getByRole('button', { name: '적용' }).click()

  await expect(page.getByText('템플릿이 적용되었습니다')).toBeVisible({ timeout: 8000 })
  await expect(page.locator('.react-flow__node')).toHaveCount(5, { timeout: 8000 })
})
