import { test, expect } from '@playwright/test'
import { AUTH_FILE } from './global-setup'
import { createEditorPageWithIds } from './helpers'
import path from 'path'
import fs from 'fs'

test.use({ storageState: AUTH_FILE })

test('E2E-DBML-02: 테이블 생성 후 DBML 내보내기', async ({ page }) => {
  await createEditorPageWithIds(page)

  // Add a table first
  await page.getByRole('button', { name: '+ 테이블' }).click()
  await expect(page.locator('.react-flow__node')).toHaveCount(1, { timeout: 5000 })

  // Set up download listener
  const downloadPromise = page.waitForEvent('download', { timeout: 10000 })

  // Open export menu and click DBML
  await page.getByRole('button', { name: /내보내기/i }).click()
  await page.getByRole('button', { name: 'DBML' }).click()

  // Wait for download
  const download = await downloadPromise
  expect(download.suggestedFilename()).toMatch(/\.dbml$/)

  // Save and verify content
  const tmpPath = path.join('/tmp', download.suggestedFilename())
  await download.saveAs(tmpPath)

  const content = fs.readFileSync(tmpPath, 'utf-8')
  expect(content).toMatch(/[Tt]able/)

  // Cleanup
  fs.unlinkSync(tmpPath)
})
