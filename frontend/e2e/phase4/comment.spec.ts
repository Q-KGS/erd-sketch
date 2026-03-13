import { test, expect } from '@playwright/test'
import { createEditorPage } from '../helpers/setup'
import { AUTH_FILE } from '../global-setup'

test.use({ storageState: AUTH_FILE })

test('E2E-CMT-01: 댓글 작성, 답글, 해결', async ({ page }) => {
  await createEditorPage(page)

  // 1. 댓글 패널 열기
  await page.getByRole('button', { name: '댓글' }).click()
  await expect(page.getByRole('heading', { name: '댓글' })).toBeVisible()

  // 2. 댓글 작성
  await page.getByPlaceholder('댓글을 입력하세요').fill('email 컬럼 인덱스 추가 필요')
  await page.getByRole('button', { name: '전송' }).first().click()

  // 댓글이 목록에 나타날 때까지 대기
  const commentItem = page.locator('li').filter({ hasText: 'email 컬럼 인덱스 추가 필요' }).first()
  await expect(commentItem).toBeVisible({ timeout: 5000 })

  // 3. 답글 작성 (댓글 항목 내 답글 버튼 클릭)
  await commentItem.getByRole('button', { name: '답글' }).click()
  await page.getByPlaceholder('답글을 입력하세요').fill('확인했습니다')
  // Click the 전송 button inside the reply form
  await page.locator('.ml-4 button', { hasText: '전송' }).click()
  await expect(page.getByText('확인했습니다')).toBeVisible({ timeout: 5000 })

  // 4. 댓글 해결
  await commentItem.getByRole('button', { name: '해결' }).click()

  // 해결된 댓글은 opacity-50으로 표시
  await expect(commentItem).toHaveClass(/opacity-50/, { timeout: 5000 })
})

test('E2E-CMT-02: 미해결만 보기 필터', async ({ page }) => {
  await createEditorPage(page)

  await page.getByRole('button', { name: '댓글' }).click()

  // 댓글 두 개 작성
  await page.getByPlaceholder('댓글을 입력하세요').fill('미해결 댓글')
  await page.getByRole('button', { name: '전송' }).first().click()
  const unresolvedItem = page.locator('li').filter({ hasText: '미해결 댓글' }).first()
  await expect(unresolvedItem).toBeVisible({ timeout: 5000 })

  await page.getByPlaceholder('댓글을 입력하세요').fill('해결할 댓글')
  await page.getByRole('button', { name: '전송' }).first().click()
  const toResolveItem = page.locator('li').filter({ hasText: '해결할 댓글' }).first()
  await expect(toResolveItem).toBeVisible({ timeout: 5000 })

  // "해결할 댓글" 항목의 해결 버튼만 클릭 (스코프 한정으로 오동작 방지)
  await toResolveItem.getByRole('button', { name: '해결' }).click()
  await page.waitForTimeout(500)

  // 미해결만 보기 체크박스
  await page.getByLabel('미해결만 보기').check()
  await page.waitForTimeout(500)

  await expect(unresolvedItem).toBeVisible({ timeout: 5000 })
  await expect(page.getByText('해결할 댓글')).not.toBeVisible()
})
