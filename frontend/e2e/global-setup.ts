import { chromium, request } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'
export const E2E_EMAIL = 'e2e-phase4@erdsketch.test'
export const E2E_PASSWORD = 'E2EPhase4!'
export const E2E_NAME = 'E2E Tester'
export const AUTH_FILE = path.join(__dirname, '.auth/user.json')

export default async function globalSetup() {
  const authDir = path.dirname(AUTH_FILE)
  if (!fs.existsSync(authDir)) fs.mkdirSync(authDir, { recursive: true })

  // Register test user (ignore error if already exists)
  const apiContext = await request.newContext({ baseURL: BASE_URL })
  await apiContext.post('/api/v1/auth/register', {
    data: { email: E2E_EMAIL, password: E2E_PASSWORD, displayName: E2E_NAME },
    failOnStatusCode: false,
  })
  await apiContext.dispose()

  // Login via browser and save storage state
  const browser = await chromium.launch()
  const page = await browser.newPage()
  await page.goto(`${BASE_URL}/login`)
  await page.getByLabel(/이메일/i).fill(E2E_EMAIL)
  await page.getByLabel(/비밀번호/i).fill(E2E_PASSWORD)
  await page.getByRole('button', { name: /로그인/i }).click()
  // Wait until we leave the login page (navigated to dashboard)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 })
  await page.context().storageState({ path: AUTH_FILE })
  await browser.close()
}
