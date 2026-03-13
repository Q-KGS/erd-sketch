import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useTheme } from './useTheme'

describe('useTheme', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  afterEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  // F-DARK-01: 토글 전환 → <html>에 class="dark" 추가/제거
  test('F-DARK-01: toggleTheme 시 html에 dark 클래스 추가/제거됨', () => {
    const { result } = renderHook(() => useTheme())

    // 라이트 → 다크
    act(() => { result.current.toggleTheme() })
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    // 다크 → 라이트
    act(() => { result.current.toggleTheme() })
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  // F-DARK-02: 설정 저장 → localStorage에 theme: dark/light 저장
  test('F-DARK-02: 테마 변경 시 localStorage에 저장됨', () => {
    const { result } = renderHook(() => useTheme())

    act(() => { result.current.setDark() })
    expect(localStorage.getItem('theme')).toBe('dark')

    act(() => { result.current.setLight() })
    expect(localStorage.getItem('theme')).toBe('light')
  })

  // F-DARK-03: 페이지 새로고침 후 유지 → 저장된 테마 자동 적용
  test('F-DARK-03: 저장된 테마가 마운트 시 자동 적용됨', () => {
    localStorage.setItem('theme', 'dark')

    const { result } = renderHook(() => useTheme())
    expect(result.current.theme).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  // F-DARK-04: 시스템 다크모드 감지 → prefers-color-scheme: dark 초기 적용
  test('F-DARK-04: 시스템 다크모드 감지 시 dark 테마 초기 적용됨', () => {
    // matchMedia mock
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn((query: string) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })

    const { result } = renderHook(() => useTheme())
    expect(result.current.theme).toBe('dark')
  })

  // F-DARK-05: theme 값 정확성 확인
  test('F-DARK-05: setDark/setLight로 테마 직접 설정 가능', () => {
    const { result } = renderHook(() => useTheme())

    act(() => { result.current.setDark() })
    expect(result.current.theme).toBe('dark')

    act(() => { result.current.setLight() })
    expect(result.current.theme).toBe('light')
  })
})
