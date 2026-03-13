import { test, expect } from '@playwright/test'
import { createEditorPage } from '../helpers/setup'
import { AUTH_FILE } from '../global-setup'

test.use({ storageState: AUTH_FILE })

test('E2E-PDF-01: PDF 내보내기 다운로드', async ({ page }) => {
  await createEditorPage(page)

  // 테이블 추가
  await page.getByRole('button', { name: '+ 테이블' }).click()
  await page.waitForTimeout(800)

  // 내보내기 드롭다운 열기
  await page.getByRole('button', { name: /내보내기/i }).click()

  // PDF 다운로드 이벤트 대기
  const downloadPromise = page.waitForEvent('download', { timeout: 15000 })
  await page.getByRole('button', { name: 'PDF' }).click()
  const download = await downloadPromise

  // 파일명과 크기 검증
  expect(download.suggestedFilename()).toMatch(/\.pdf$/)
  const path = await download.path()
  expect(path).toBeTruthy()
})
