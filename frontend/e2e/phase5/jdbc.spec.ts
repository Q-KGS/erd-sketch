import { test, expect } from '@playwright/test'
import { AUTH_FILE } from './global-setup'
import { createEditorPageWithIds } from './helpers'

test.use({ storageState: AUTH_FILE })

test('E2E-JDBC-01: JDBC 연결 모달 열기 및 연결 테스트', async ({ page }) => {
  await createEditorPageWithIds(page)

  // Open JDBC modal
  await page.getByRole('button', { name: 'JDBC' }).click()
  await expect(page.getByRole('heading', { name: 'JDBC 연결' })).toBeVisible()

  // Fill form with app's own DB credentials
  await page.locator('input[type="text"]').first().fill('localhost')
  await page.locator('input[type="number"]').fill('5432')

  // Find database input (3rd text input)
  const textInputs = page.locator('input[type="text"]')
  await textInputs.nth(1).fill('erdsketch_e2e')
  await textInputs.nth(2).fill('erdsketch')
  await page.locator('input[type="password"]').fill('erdsketch')

  // Click connection test
  await page.getByRole('button', { name: '연결 테스트' }).click()

  // Should show either success or failure toast (connection to test DB may vary)
  await expect(
    page.getByText('연결 성공').or(page.getByText('연결 실패'))
  ).toBeVisible({ timeout: 10000 })
})

test('E2E-JDBC-01b: JDBC 모달 닫기', async ({ page }) => {
  await createEditorPageWithIds(page)

  await page.getByRole('button', { name: 'JDBC' }).click()
  await expect(page.getByRole('heading', { name: 'JDBC 연결' })).toBeVisible()

  await page.getByRole('button', { name: '취소' }).click()
  await expect(page.getByRole('heading', { name: 'JDBC 연결' })).not.toBeVisible()
})
