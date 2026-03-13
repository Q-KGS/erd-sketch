import { describe, test, expect, vi, beforeEach } from 'vitest'

// vi.hoisted()로 모킹 변수를 먼저 초기화 (vi.mock 팩토리보다 앞서 실행됨)
const { mockToPng } = vi.hoisted(() => ({
  mockToPng: vi.fn(async () => 'data:image/png;base64,abc123'),
}))

vi.mock('html-to-image', () => ({
  toPng: mockToPng,
}))

import { exportCanvasToPng } from './pngExport'

describe('pngExport', () => {
  let mockElement: HTMLElement

  beforeEach(() => {
    vi.clearAllMocks()
    mockToPng.mockResolvedValue('data:image/png;base64,abc123')
    mockElement = document.createElement('div')
  })

  // F-PNG-01: 캔버스 캡처 → html-to-image의 toPng 호출
  test('F-PNG-01: exportCanvasToPng 호출 시 toPng가 실행됨', async () => {
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    const original = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string, ...args: unknown[]) => {
      if (tag === 'a') return mockAnchor as unknown as HTMLElement
      return original(tag as keyof HTMLElementTagNameMap, ...(args as []))
    })

    await exportCanvasToPng(mockElement)

    expect(mockToPng).toHaveBeenCalledOnce()
    expect(mockToPng).toHaveBeenCalledWith(mockElement, expect.objectContaining({
      backgroundColor: '#ffffff',
    }))

    vi.restoreAllMocks()
  })

  // F-PNG-02: 다운로드 파일명 형식 → erdsketch_export_YYYYMMDD.png
  test('F-PNG-02: 다운로드 파일명이 erdsketch_export_YYYYMMDD.png 형식임', async () => {
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    const original = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string, ...args: unknown[]) => {
      if (tag === 'a') return mockAnchor as unknown as HTMLElement
      return original(tag as keyof HTMLElementTagNameMap, ...(args as []))
    })

    await exportCanvasToPng(mockElement)

    expect(mockAnchor.download).toMatch(/^erdsketch_export_\d{8}\.png$/)

    vi.restoreAllMocks()
  })

  // F-PNG-03: 빈 캔버스 내보내기 → 에러 없음
  test('F-PNG-03: 빈 캔버스 내보내기 시 에러 없이 완료됨', async () => {
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    const original = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string, ...args: unknown[]) => {
      if (tag === 'a') return mockAnchor as unknown as HTMLElement
      return original(tag as keyof HTMLElementTagNameMap, ...(args as []))
    })

    const emptyElement = document.createElement('div')
    await expect(exportCanvasToPng(emptyElement)).resolves.toBeUndefined()

    vi.restoreAllMocks()
  })
})
