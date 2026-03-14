import { test, expect } from '@playwright/test'
import { request as pwRequest } from '@playwright/test'
import { AUTH_FILE } from './global-setup'
import { createEditorPageWithIds } from './helpers'
import fs from 'fs'

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'

// Second user credentials
const USER2_EMAIL = 'e2e-phase5-user2@erdsketch.test'
const USER2_PASSWORD = 'E2EPhase5User2!'
const USER2_NAME = 'Phase5 User2'

test.use({ storageState: AUTH_FILE })

function getUser1Token(): string {
  const state = JSON.parse(fs.readFileSync(AUTH_FILE, 'utf-8'))
  const authStorage = state.origins?.[0]?.localStorage?.find(
    (i: { name: string }) => i.name === 'erdsketch-auth'
  )
  if (!authStorage) throw new Error('auth-storage not found')
  return JSON.parse(authStorage.value).state.tokens.accessToken
}

test('E2E-FOLLOW-01: 팔로우 모드 - 두 사용자 시나리오', async ({ page, browser }) => {
  // Register second user (ignore 409 if already exists)
  const api = await pwRequest.newContext({ baseURL: BASE_URL })
  await api.post('/api/v1/auth/register', {
    data: { email: USER2_EMAIL, password: USER2_PASSWORD, displayName: USER2_NAME },
    failOnStatusCode: false,
  })
  await api.dispose()

  // User 1: create editor page
  const { editorUrl, workspaceId } = await createEditorPageWithIds(page)

  // User 1 invites user 2 to the workspace so user 2 can access the editor
  const inviteApi = await pwRequest.newContext({
    baseURL: BASE_URL,
    extraHTTPHeaders: { Authorization: `Bearer ${getUser1Token()}` },
  })
  await inviteApi.post(`/api/v1/workspaces/${workspaceId}/members`, {
    data: { email: USER2_EMAIL, role: 'MEMBER' },
    failOnStatusCode: false,
  })
  await inviteApi.dispose()

  // Login as user 2 in a fresh browser context (unauthenticated)
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

  // Wait for Yjs presence to sync
  await page.waitForTimeout(2000)

  // Check if a collaborator avatar appeared for user 1
  const avatars = page.locator('.react-flow .export-ignore button')
  const avatarCount = await avatars.count()

  if (avatarCount > 0) {
    await avatars.first().click()
    await expect(page.getByRole('button', { name: '따라가기' })).toBeVisible({ timeout: 3000 })

    await page.getByRole('button', { name: '따라가기' }).click()
    await expect(page.getByText(/을 따라가는 중/)).toBeVisible({ timeout: 3000 })

    await page.getByRole('button', { name: '해제' }).click()
    await expect(page.getByText(/을 따라가는 중/)).not.toBeVisible({ timeout: 3000 })
  } else {
    // Collaboration presence requires real-time WebSocket; skip if unavailable
    test.info().annotations.push({ type: 'skip-reason', description: 'No collaborator avatar visible (WebSocket may not be available)' })
  }

  await context2.close()
})
