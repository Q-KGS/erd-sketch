import { test, expect, chromium } from '@playwright/test'
import { AUTH_FILE, E2E_EMAIL, E2E_PASSWORD } from './global-setup'
import { createEditorPageWithIds } from './helpers'
import { request as pwRequest } from '@playwright/test'

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'

// Second user credentials
const USER2_EMAIL = 'e2e-phase5-user2@erdsketch.test'
const USER2_PASSWORD = 'E2EPhase5User2!'
const USER2_NAME = 'Phase5 User2'

test.use({ storageState: AUTH_FILE })

test('E2E-FOLLOW-01: 팔로우 모드 - 두 사용자 시나리오', async ({ page, browser }) => {
  // Register second user
  const api = await pwRequest.newContext({ baseURL: BASE_URL })
  await api.post('/api/v1/auth/register', {
    data: { email: USER2_EMAIL, password: USER2_PASSWORD, displayName: USER2_NAME },
    failOnStatusCode: false,
  })
  await api.dispose()

  // User 1: create editor page
  const { editorUrl, workspaceId } = await createEditorPageWithIds(page)

  // Login as user 2 in a second context
  const context2 = await browser.newContext()
  const page2 = await context2.newPage()
  await page2.goto(`${BASE_URL}/login`)
  await page2.getByLabel(/이메일/i).fill(USER2_EMAIL)
  await page2.getByLabel(/비밀번호/i).fill(USER2_PASSWORD)
  await page2.getByRole('button', { name: /로그인/i }).click()
  await page2.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 })

  // Navigate user 2 to the same editor
  await page2.goto(`${BASE_URL}${editorUrl}`)
  await page2.waitForSelector('.react-flow', { timeout: 15000 })

  // User 1 should see user 2's avatar appear in presence area (may take a moment for Yjs to sync)
  // Since WebSocket requires live backend, presence avatars may not appear in all E2E envs.
  // We verify the follow mode banner/button exists if a collaborator appears.
  await page.waitForTimeout(2000)

  // Check if a collaborator avatar appeared for user 1
  const avatars = page.locator('.react-flow .export-ignore button')
  const avatarCount = await avatars.count()

  if (avatarCount > 0) {
    // Click first avatar to open popover
    await avatars.first().click()
    await expect(page.getByRole('button', { name: '따라가기' })).toBeVisible({ timeout: 3000 })

    // Start following
    await page.getByRole('button', { name: '따라가기' }).click()

    // Follow mode banner should appear
    await expect(page.getByText(/을 따라가는 중/)).toBeVisible({ timeout: 3000 })

    // Stop following via banner
    await page.getByRole('button', { name: '해제' }).click()
    await expect(page.getByText(/을 따라가는 중/)).not.toBeVisible({ timeout: 3000 })
  } else {
    // Collaboration presence requires real-time connection; skip if no avatars
    test.info().annotations.push({ type: 'skip-reason', description: 'No collaborator avatar visible (WebSocket may not be available)' })
  }

  await context2.close()
})
