import { test, expect } from '@playwright/test'

// OAuth tests don't need auth state - they test the login page
test('E2E-OAUTH-01: Google 로그인 버튼 존재 및 올바른 URL', async ({ page }) => {
  test.skip(!!process.env.CI && process.env.SKIP_OAUTH === 'true', 'OAuth UI test - requires browser')

  await page.goto('/login')

  // Google OAuth button should be present
  const googleBtn = page.locator('[data-testid="oauth-google"]')
  await expect(googleBtn).toBeVisible()
  await expect(googleBtn).toHaveAttribute('href', '/oauth2/authorization/google')
})

test('E2E-OAUTH-02: GitHub 로그인 버튼 존재 및 올바른 URL', async ({ page }) => {
  test.skip(!!process.env.CI && process.env.SKIP_OAUTH === 'true', 'OAuth UI test - requires browser')

  await page.goto('/login')

  // GitHub OAuth button should be present
  const githubBtn = page.locator('[data-testid="oauth-github"]')
  await expect(githubBtn).toBeVisible()
  await expect(githubBtn).toHaveAttribute('href', '/oauth2/authorization/github')
})

test('E2E-OAUTH-01+02: OAuth 버튼이 로그인 페이지에 렌더링됨', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByText('Google로 로그인')).toBeVisible()
  await expect(page.getByText('GitHub로 로그인')).toBeVisible()
})
