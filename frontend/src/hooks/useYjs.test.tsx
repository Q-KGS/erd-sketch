import { describe, test, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'

// Stable mock user reference — must not be recreated on every call,
// otherwise useEffect([documentId, user]) will loop infinitely.
const mockUser = { id: 'user-1', displayName: 'Alice', email: 'alice@example.com' }

const mockDestroy = vi.fn()
const mockWsProvider = {
  on: vi.fn(),
  off: vi.fn(),
  destroy: vi.fn(),
}

vi.mock('@/crdt/yjsProvider', () => ({
  createYjsProvider: vi.fn(() => ({
    ydoc: { id: 'mock-ydoc' },
    wsProvider: mockWsProvider,
    awareness: {},
    idbProvider: { destroy: vi.fn() },
    destroy: mockDestroy,
  })),
}))

vi.mock('@/store/authStore', () => ({
  // useAuthStore must return a stable user reference to avoid re-triggering
  // useEffect every render cycle (which would cause an infinite loop)
  useAuthStore: () => ({ user: mockUser }),
}))

import { useYjs } from './useYjs'
import { createYjsProvider } from '@/crdt/yjsProvider'

describe('useYjs', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // F-HOOK-01: documentId가 null이면 ydoc: null, isConnected: false
  test('F-HOOK-01: documentId null이면 ydoc null, isConnected false', () => {
    const { result } = renderHook(() => useYjs(null))

    expect(result.current.ydoc).toBeNull()
    expect(result.current.isConnected).toBe(false)
    expect(createYjsProvider).not.toHaveBeenCalled()
  })

  // F-HOOK-02: documentId 변경 시 이전 컨텍스트 destroy 후 새 컨텍스트 생성
  test('F-HOOK-02: documentId 변경 시 이전 destroy 호출 후 재생성', () => {
    const { rerender } = renderHook(({ docId }: { docId: string }) => useYjs(docId), {
      initialProps: { docId: 'doc-a' },
    })

    expect(createYjsProvider).toHaveBeenCalledTimes(1)
    expect(vi.mocked(createYjsProvider).mock.calls[0][0].documentId).toBe('doc-a')

    act(() => {
      rerender({ docId: 'doc-b' })
    })

    expect(mockDestroy).toHaveBeenCalledTimes(1)
    expect(createYjsProvider).toHaveBeenCalledTimes(2)
    expect(vi.mocked(createYjsProvider).mock.calls[1][0].documentId).toBe('doc-b')
  })

  // F-HOOK-03: 컴포넌트 unmount 시 destroy 호출
  test('F-HOOK-03: unmount 시 destroy 호출됨', () => {
    const { unmount } = renderHook(() => useYjs('doc-test'))

    expect(createYjsProvider).toHaveBeenCalledOnce()

    unmount()

    expect(mockDestroy).toHaveBeenCalledOnce()
  })
})
