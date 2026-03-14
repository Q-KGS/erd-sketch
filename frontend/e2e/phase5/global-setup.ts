import { request } from '@playwright/test'
import path from 'path'
import fs from 'fs'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'
export const E2E_EMAIL = 'e2e-phase5@erdsketch.test'
export const E2E_PASSWORD = 'E2EPhase5!'
export const E2E_NAME = 'E2E Phase5 Tester'
export const AUTH_FILE = path.join(__dirname, '.auth/user.json')

export default async function globalSetup() {
  const authDir = path.dirname(AUTH_FILE)
  fs.mkdirSync(authDir, { recursive: true })

  const apiContext = await request.newContext({ baseURL: BASE_URL })

  // Register (ignore 409 if user already exists)
  await apiContext.post('/api/v1/auth/register', {
    data: { email: E2E_EMAIL, password: E2E_PASSWORD, displayName: E2E_NAME },
    failOnStatusCode: false,
  })

  // Login via API and get tokens
  const loginRes = await apiContext.post('/api/v1/auth/login', {
    data: { email: E2E_EMAIL, password: E2E_PASSWORD },
  })
  if (!loginRes.ok()) {
    throw new Error(`Login failed: ${loginRes.status()} ${await loginRes.text()}`)
  }
  const { user, tokens } = await loginRes.json()
  await apiContext.dispose()

  // Write Playwright storage state with Zustand auth persisted in localStorage
  const storageState = {
    cookies: [],
    origins: [
      {
        origin: BASE_URL,
        localStorage: [
          {
            name: 'erdsketch-auth',
            value: JSON.stringify({
              state: { user, tokens, isAuthenticated: true },
              version: 0,
            }),
          },
        ],
      },
    ],
  }
  fs.writeFileSync(AUTH_FILE, JSON.stringify(storageState, null, 2))
  console.log(`[global-setup] Auth state saved to ${AUTH_FILE}`)
}
