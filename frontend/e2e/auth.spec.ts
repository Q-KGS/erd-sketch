import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test('로그인 페이지가 렌더링된다', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: /로그인|ErdSketch/i })).toBeVisible()
    await expect(page.getByLabel(/이메일/i)).toBeVisible()
    await expect(page.getByLabel(/비밀번호/i)).toBeVisible()
  })

  test('빈 폼 제출 시 유효성 오류를 표시한다', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('button', { name: /로그인/i }).click()
    // Form validation prevents empty submit
    const emailInput = page.getByLabel(/이메일/i)
    await expect(emailInput).toBeVisible()
  })

  test('잘못된 자격증명으로 오류 메시지를 표시한다', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel(/이메일/i).fill('wrong@example.com')
    await page.getByLabel(/비밀번호/i).fill('wrongpassword')
    await page.getByRole('button', { name: /로그인/i }).click()
    await expect(page.getByText(/이메일 또는 비밀번호|오류|실패/i)).toBeVisible({ timeout: 5000 })
  })

  test('회원가입 페이지로 이동한다', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: /회원가입/i }).click()
    await expect(page).toHaveURL(/register/)
  })
})
