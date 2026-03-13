import { type Page, request } from '@playwright/test'
import { AUTH_FILE } from '../global-setup'
import fs from 'fs'

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'

function getToken(): string {
  const state = JSON.parse(fs.readFileSync(AUTH_FILE, 'utf-8'))
  // Find JWT in localStorage from storageState
  const authStorage = state.origins?.[0]?.localStorage?.find(
    (item: { name: string }) => item.name === 'erdsketch-auth'
  )
  if (!authStorage) throw new Error('auth-storage not found in storageState')
  const parsed = JSON.parse(authStorage.value)
  return parsed.state.tokens.accessToken
}

export async function createEditorPage(page: Page): Promise<string> {
  const token = getToken()
  const slug = `e2e-ws-${Date.now()}`

  const api = await request.newContext({
    baseURL: BASE_URL,
    extraHTTPHeaders: { Authorization: `Bearer ${token}` },
  })

  const wsRes = await api.post('/api/v1/workspaces', {
    data: { name: 'E2E Workspace', slug },
  })
  const ws = await wsRes.json()

  const prjRes = await api.post(`/api/v1/workspaces/${ws.id}/projects`, {
    data: { name: 'E2E Project', targetDbType: 'POSTGRESQL' },
  })
  const prj = await prjRes.json()

  const docRes = await api.post(`/api/v1/projects/${prj.id}/documents`, {
    data: { name: 'Main ERD' },
  })
  const doc = await docRes.json()

  await api.dispose()

  const editorUrl = `/workspaces/${ws.id}/projects/${prj.id}/documents/${doc.id}`
  await page.goto(editorUrl)
  // Wait for React Flow canvas to appear
  await page.waitForSelector('.react-flow', { timeout: 15000 })
  return editorUrl
}
