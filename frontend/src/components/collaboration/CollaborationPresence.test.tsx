import { describe, test, expect, vi } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import CollaborationPresence from './CollaborationPresence'

function makeAwareness(
  states: Array<{ clientId: number; name: string; color: string }>,
  localClientId = 999,
) {
  const stateMap = new Map<number, { user: { name: string; color: string } }>()
  for (const s of states) {
    stateMap.set(s.clientId, { user: { name: s.name, color: s.color } })
  }
  // 로컬 클라이언트 state 추가 (필터링 대상)
  stateMap.set(localClientId, { user: { name: 'Me', color: '#000000' } })

  const listeners: Record<string, Array<() => void>> = { change: [] }

  return {
    clientID: localClientId,
    getStates: vi.fn(() => stateMap),
    on: vi.fn((event: string, cb: () => void) => {
      if (!listeners[event]) listeners[event] = []
      listeners[event].push(cb)
    }),
    off: vi.fn((event: string, cb: () => void) => {
      if (listeners[event]) {
        listeners[event] = listeners[event].filter((fn) => fn !== cb)
      }
    }),
    // 테스트에서 이벤트 수동 트리거용
    emit: (event: string) => {
      for (const cb of listeners[event] ?? []) cb()
    },
  }
}

describe('CollaborationPresence', () => {
  // F-PRES-01: presence 없음 → null 렌더
  test('F-PRES-01: presence 0개이면 null 렌더', () => {
    const awareness = makeAwareness([])
    const { container } = render(<CollaborationPresence awareness={awareness as never} />)
    expect(container.firstChild).toBeNull()
  })

  // F-PRES-02: 2명의 다른 사용자 → 아바타 2개
  test('F-PRES-02: 2명의 presence → 아바타 2개 렌더', () => {
    const awareness = makeAwareness([
      { clientId: 1, name: 'Alice', color: '#ff0000' },
      { clientId: 2, name: 'Bob', color: '#00ff00' },
    ])
    render(<CollaborationPresence awareness={awareness as never} />)

    expect(screen.getByTitle('Alice')).toBeDefined()
    expect(screen.getByTitle('Bob')).toBeDefined()
  })

  // F-PRES-03: 6명 이상 → 최대 5개 아바타 + "+N" 배지
  test('F-PRES-03: 6명 presence → 아바타 5개 + "+1" 배지', () => {
    const users = Array.from({ length: 6 }, (_, i) => ({
      clientId: i + 1,
      name: `User${i + 1}`,
      color: '#aaaaaa',
    }))
    const awareness = makeAwareness(users)
    render(<CollaborationPresence awareness={awareness as never} />)

    // 5개 아바타 렌더 확인
    expect(screen.getByTitle('User1')).toBeDefined()
    expect(screen.getByTitle('User5')).toBeDefined()
    // User6는 아바타로 표시 안 됨
    expect(screen.queryByTitle('User6')).toBeNull()
    // "+1" 배지
    expect(screen.getByText('+1')).toBeDefined()
  })

  // F-PRES-04: awareness change 이벤트 → presence 업데이트 (리렌더)
  test('F-PRES-04: awareness change 이벤트 → presence 리렌더', () => {
    const awareness = makeAwareness([])
    render(<CollaborationPresence awareness={awareness as never} />)

    // 초기 상태: 아무도 없음 → null 렌더
    expect(screen.queryByTitle(/User/)).toBeNull()

    // awareness에 새 사용자 추가 후 change 이벤트 발생
    awareness.getStates.mockReturnValueOnce(
      new Map([
        [1, { user: { name: 'Charlie', color: '#0000ff' } }],
        [awareness.clientID, { user: { name: 'Me', color: '#000' } }],
      ]),
    )

    act(() => {
      awareness.emit('change')
    })

    expect(screen.getByTitle('Charlie')).toBeDefined()
  })
})
