import { describe, test, expect, vi, beforeEach } from 'vitest'
import * as Y from 'yjs'

// y-websocket 모킹
const mockAwareness = {
  setLocalStateField: vi.fn(),
  getStates: vi.fn(() => new Map()),
  on: vi.fn(),
  off: vi.fn(),
  clientID: 1,
}
const mockWsProviderInstance = {
  awareness: mockAwareness,
  on: vi.fn(),
  off: vi.fn(),
  destroy: vi.fn(),
}
vi.mock('y-websocket', () => ({
  WebsocketProvider: vi.fn(() => mockWsProviderInstance),
}))

// y-indexeddb 모킹
const mockIdbInstance = { destroy: vi.fn() }
vi.mock('y-indexeddb', () => ({
  IndexeddbPersistence: vi.fn(() => mockIdbInstance),
}))

import { createYjsProvider } from './yjsProvider'

describe('createYjsProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockIdbInstance.destroy.mockReset()
    mockWsProviderInstance.destroy.mockReset()
  })

  // F-YJS-01: createYjsProvider 호출 시 ydoc, wsProvider, awareness 반환
  test('F-YJS-01: createYjsProvider는 ydoc, wsProvider, awareness를 반환', () => {
    const ctx = createYjsProvider({
      documentId: 'doc-test',
      userPresence: { id: 'u1', name: 'Alice', color: '#ff0000' },
    })

    expect(ctx.ydoc).toBeInstanceOf(Y.Doc)
    expect(ctx.wsProvider).toBeDefined()
    expect(ctx.awareness).toBeDefined()
    expect(ctx.idbProvider).toBeDefined()
    expect(typeof ctx.destroy).toBe('function')
  })

  // F-YJS-02: destroy() 호출 시 wsProvider, idbProvider, ydoc 모두 destroy
  test('F-YJS-02: destroy() 호출 시 모든 리소스 해제', () => {
    const ctx = createYjsProvider({
      documentId: 'doc-destroy',
      userPresence: { id: 'u1', name: 'Alice', color: '#ff0000' },
    })

    const ydocDestroySpy = vi.spyOn(ctx.ydoc, 'destroy')
    ctx.destroy()

    expect(mockWsProviderInstance.destroy).toHaveBeenCalledOnce()
    expect(mockIdbInstance.destroy).toHaveBeenCalledOnce()
    expect(ydocDestroySpy).toHaveBeenCalledOnce()
  })

  // F-YJS-03: awareness에 userPresence 설정
  test('F-YJS-03: awareness.setLocalStateField에 user 정보 설정', () => {
    const userPresence = { id: 'u2', name: 'Bob', color: '#00ff00' }
    createYjsProvider({ documentId: 'doc-awareness', userPresence })

    expect(mockAwareness.setLocalStateField).toHaveBeenCalledWith('user', userPresence)
  })

  // 추가: 다른 documentId마다 별도 Y.Doc 생성
  test('서로 다른 documentId로 생성된 컨텍스트는 별도의 Y.Doc', () => {
    const ctx1 = createYjsProvider({
      documentId: 'doc-a',
      userPresence: { id: 'u1', name: 'Alice', color: '#ff0000' },
    })
    const ctx2 = createYjsProvider({
      documentId: 'doc-b',
      userPresence: { id: 'u2', name: 'Bob', color: '#00ff00' },
    })

    expect(ctx1.ydoc).not.toBe(ctx2.ydoc)
  })
})
