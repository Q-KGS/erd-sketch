import { test, expect } from '@playwright/test'
import { createEditorPage } from '../helpers/setup'
import { AUTH_FILE } from '../global-setup'

test.use({ storageState: AUTH_FILE })

test('E2E-OFFLINE-01: 오프라인 상태에서도 편집 가능', async ({ page, context }) => {
  await createEditorPage(page)

  // 온라인 상태 확인
  await expect(page.getByText('연결됨')).toBeVisible({ timeout: 10000 })

  // 네트워크 오프라인 설정
  await context.setOffline(true)

  // 오프라인 상태에서 테이블 추가 가능 여부 확인
  // (y-websocket은 IndexedDB 캐시 덕분에 로컬 편집은 계속 가능)
  await page.getByRole('button', { name: '+ 테이블' }).click()
  await page.waitForTimeout(500)
  // 캔버스에 노드가 렌더링되어 있어야 함 (로컬 Yjs 상태 작동 증명)
  await expect(page.locator('.react-flow__node')).toBeVisible({ timeout: 5000 })

  // 온라인 복구
  await context.setOffline(false)

  // 재연결 확인 (y-websocket 재연결 시도 포함, 충분한 타임아웃 부여)
  await expect(page.getByText('연결됨')).toBeVisible({ timeout: 35000 })
})
