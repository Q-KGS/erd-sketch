import { test, expect } from '@playwright/test'
import { createEditorPage } from '../helpers/setup'
import { AUTH_FILE } from '../global-setup'

test.use({ storageState: AUTH_FILE })

test('E2E-VER-01: 버전 저장 후 복원', async ({ page }) => {
  await createEditorPage(page)

  // 1. 테이블 추가
  await page.getByRole('button', { name: '+ 테이블' }).click()
  await page.waitForTimeout(800)

  // 2. 버전 패널 열기
  await page.getByRole('button', { name: '버전' }).click()
  await expect(page.getByText('버전 히스토리')).toBeVisible()

  // 3. 새 버전 저장
  await page.getByRole('button', { name: '새 버전 저장' }).click()
  await page.getByPlaceholder('버전 레이블 (선택)').fill('v1 - 초기 설계')
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await expect(page.getByText('v1 - 초기 설계')).toBeVisible({ timeout: 5000 })
  await expect(page.getByText('버전이 저장되었습니다')).toBeVisible()

  // 4. 테이블 하나 더 추가
  await page.getByRole('button', { name: '+ 테이블' }).click()
  await page.waitForTimeout(800)

  // 5. 버전 복원 (window.confirm 수락)
  page.on('dialog', (dialog) => dialog.accept())
  await page.getByText('v1 - 초기 설계').click()
  await page.getByRole('button', { name: '복원' }).click()

  // 6. 복원 성공 토스트 확인
  await expect(page.getByText('버전이 복원되었습니다')).toBeVisible({ timeout: 5000 })
})

test('E2E-VER-02: 버전 없을 때 빈 상태 표시', async ({ page }) => {
  await createEditorPage(page)

  await page.getByRole('button', { name: '버전' }).click()
  await expect(page.getByText('버전 히스토리')).toBeVisible()
  await expect(page.getByText('저장된 버전이 없습니다')).toBeVisible()
})
