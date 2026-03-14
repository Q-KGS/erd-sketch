import { describe, test, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useFollowMode } from './useFollowMode'
import type { Viewport } from './useFollowMode'

vi.mock('react-hot-toast', () => ({
  default: Object.assign(vi.fn(), { success: vi.fn(), error: vi.fn() }),
}))

// Awareness mock 팩토리
function makeAwareness() {
  const listeners: Map<string, Set<() => void>> = new Map()
  const states: Map<number, Record<string, unknown>> = new Map()

  const awareness = {
    on: vi.fn((event: string, fn: () => void) => {
      if (!listeners.has(event)) listeners.set(event, new Set())
      listeners.get(event)!.add(fn)
    }),
    off: vi.fn((event: string, fn: () => void) => {
      listeners.get(event)?.delete(fn)
    }),
    getStates: vi.fn(() => states),
    emit: (event: string) => {
      listeners.get(event)?.forEach((fn) => fn())
    },
    setState: (clientId: number, state: Record<string, unknown>) => {
      states.set(clientId, state)
    },
    removeState: (clientId: number) => {
      states.delete(clientId)
    },
  }
  return awareness
}

describe('useFollowMode', () => {
  let mockAwareness: ReturnType<typeof makeAwareness>
  let onViewportChange: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    mockAwareness = makeAwareness()
    onViewportChange = vi.fn()
  })

  // F-FOLLOW-01: 팔로우 시작 → 상대방 뷰포트로 캔버스 이동
  test('F-FOLLOW-01: startFollowing 호출 시 대상 뷰포트로 이동', () => {
    const targetViewport: Viewport = { x: 100, y: 200, zoom: 1.5 }
    mockAwareness.setState(42, { user: { id: 'user-b' }, viewport: targetViewport })

    const { result } = renderHook(() =>
      useFollowMode(mockAwareness as any, onViewportChange)
    )

    act(() => { result.current.startFollowing('user-b') })

    // awareness change 트리거
    act(() => { mockAwareness.emit('change') })

    expect(onViewportChange).toHaveBeenCalledWith(targetViewport)
    expect(result.current.followingUserId).toBe('user-b')
  })

  // F-FOLLOW-02: 팔로우 중 상대방 이동 → 내 뷰포트 자동 추적
  test('F-FOLLOW-02: 팔로우 중 상대방 이동 시 뷰포트 자동 업데이트', () => {
    mockAwareness.setState(42, { user: { id: 'user-b' }, viewport: { x: 0, y: 0, zoom: 1 } })

    const { result } = renderHook(() =>
      useFollowMode(mockAwareness as any, onViewportChange)
    )

    act(() => { result.current.startFollowing('user-b') })
    act(() => { mockAwareness.emit('change') })

    // 상대방이 뷰포트를 변경
    const newViewport: Viewport = { x: 500, y: 300, zoom: 2 }
    mockAwareness.setState(42, { user: { id: 'user-b' }, viewport: newViewport })
    act(() => { mockAwareness.emit('change') })

    expect(onViewportChange).toHaveBeenLastCalledWith(newViewport)
  })

  // F-FOLLOW-03: 팔로우 중 내 입력 → stopFollowing으로 팔로우 해제
  test('F-FOLLOW-03: stopFollowing 호출 시 팔로우 해제됨', () => {
    mockAwareness.setState(42, { user: { id: 'user-b' }, viewport: { x: 0, y: 0, zoom: 1 } })

    const { result } = renderHook(() =>
      useFollowMode(mockAwareness as any, onViewportChange)
    )

    act(() => { result.current.startFollowing('user-b') })
    expect(result.current.followingUserId).toBe('user-b')

    act(() => { result.current.stopFollowing() })
    expect(result.current.followingUserId).toBeNull()

    // 해제 후에는 awareness 변경에 반응 안 함
    onViewportChange.mockClear()
    act(() => { mockAwareness.emit('change') })
    expect(onViewportChange).not.toHaveBeenCalled()
  })

  // F-FOLLOW-04: 팔로우 상대방 오프라인 → 팔로우 자동 해제 + 알림 토스트
  test('F-FOLLOW-04: 팔로우 상대방 오프라인 시 팔로우 해제 및 토스트', async () => {
    const toast = await import('react-hot-toast')
    mockAwareness.setState(42, { user: { id: 'user-b' }, viewport: { x: 0, y: 0, zoom: 1 } })

    const { result } = renderHook(() =>
      useFollowMode(mockAwareness as any, onViewportChange)
    )

    act(() => { result.current.startFollowing('user-b') })

    // 상대방 오프라인 (states에서 제거)
    mockAwareness.removeState(42)
    act(() => { mockAwareness.emit('change') })

    expect(result.current.followingUserId).toBeNull()
    expect(toast.default).toHaveBeenCalled()
  })
})
