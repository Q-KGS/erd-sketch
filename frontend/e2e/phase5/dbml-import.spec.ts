import { test, expect } from '@playwright/test'
import { AUTH_FILE } from './global-setup'
import { createEditorPageWithIds } from './helpers'

test.use({ storageState: AUTH_FILE })

const SAMPLE_DBML = `
Table users {
  id uuid [pk]
  email varchar [unique, not null]
  display_name varchar
  created_at timestamptz [default: 'NOW()']
}

Table orders {
  id uuid [pk]
  user_id uuid [not null]
  status varchar
  total_amount numeric
}

Ref: orders.user_id > users.id
`

test('E2E-DBML-01: DBML 가져오기 → 2개 테이블 + 1개 관계 생성', async ({ page }) => {
  await createEditorPageWithIds(page)

  // Open import menu
  await page.getByRole('button', { name: /가져오기/i }).click()
  await page.getByRole('button', { name: 'DBML' }).click()

  // Verify modal is open
  await expect(page.getByRole('heading', { name: 'DBML 가져오기' })).toBeVisible()

  // Paste DBML
  await page.locator('textarea').fill(SAMPLE_DBML)

  // Click import (modal submit button — use last to avoid toolbar button)
  await page.getByRole('button', { name: '가져오기' }).last().click()

  // Wait for toast success
  await expect(page.getByText(/테이블.*가져왔습니다/)).toBeVisible({ timeout: 8000 })

  // Wait for nodes to appear on canvas
  await expect(page.locator('.react-flow__node')).toHaveCount(2, { timeout: 8000 })

  // Verify at least 1 edge (relationship)
  await expect(page.locator('.react-flow__edge')).toHaveCount(1, { timeout: 5000 })
})
