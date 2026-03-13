import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import type { DocumentVersion } from '@/models'

vi.mock('@/api/document', () => ({
  documentApi: {
    listVersions: vi.fn(async () => []),
    createVersion: vi.fn(async () => ({ id: 'v1', documentId: 'doc-1', versionNumber: 1, createdAt: new Date().toISOString() })),
    restoreVersion: vi.fn(async () => {}),
  },
}))

import VersionHistoryPanel from './VersionHistoryPanel'
import { documentApi } from '@/api/document'

const mockVersions: DocumentVersion[] = [
  { id: 'v1', documentId: 'doc-1', versionNumber: 1, label: 'v1 - 초기', createdBy: 'u1', createdAt: '2024-01-01T00:00:00Z' },
  { id: 'v2', documentId: 'doc-1', versionNumber: 2, label: undefined, createdBy: 'u1', createdAt: '2024-01-02T00:00:00Z' },
]

describe('VersionHistoryPanel', () => {
  beforeEach(() => { vi.clearAllMocks() })

  // F-VER-01: 버전 목록 렌더링
  test('F-VER-01: 버전 목록이 렌더링됨', async () => {
    vi.mocked(documentApi.listVersions).mockResolvedValueOnce(mockVersions)
    render(<VersionHistoryPanel documentId="doc-1" />)
    await waitFor(() => {
      expect(screen.getByText('v1 - 초기')).toBeDefined()
      expect(screen.getByText('v2')).toBeDefined()
    })
  })

  // F-VER-02: 버전 클릭 → 상세 표시
  test('F-VER-02: 버전 클릭 시 상세 정보 표시', async () => {
    vi.mocked(documentApi.listVersions).mockResolvedValueOnce(mockVersions)
    render(<VersionHistoryPanel documentId="doc-1" />)
    await waitFor(() => expect(screen.getByText('v1 - 초기')).toBeDefined())
    fireEvent.click(screen.getByText('v1 - 초기').closest('li')!)
    expect(screen.getByText('복원')).toBeDefined()
  })

  // F-VER-03: 복원 버튼 클릭 → confirm 후 restoreVersion 호출
  test('F-VER-03: 복원 버튼 클릭 시 restoreVersion 호출', async () => {
    vi.mocked(documentApi.listVersions).mockResolvedValueOnce(mockVersions)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<VersionHistoryPanel documentId="doc-1" />)
    await waitFor(() => expect(screen.getByText('v1 - 초기')).toBeDefined())
    fireEvent.click(screen.getByText('v1 - 초기').closest('li')!)
    await waitFor(() => expect(screen.getByText('복원')).toBeDefined())
    fireEvent.click(screen.getByText('복원'))
    expect(documentApi.restoreVersion).toHaveBeenCalledWith('doc-1', 'v1')
    vi.restoreAllMocks()
  })

  // F-VER-04: 새 버전 저장 클릭 → createVersion 호출
  test('F-VER-04: 새 버전 저장 시 createVersion 호출', async () => {
    vi.mocked(documentApi.listVersions).mockResolvedValue([])
    render(<VersionHistoryPanel documentId="doc-1" />)
    fireEvent.click(screen.getByText('새 버전 저장'))
    const input = screen.getByPlaceholderText(/버전 레이블/)
    fireEvent.change(input, { target: { value: 'v1 - 초기 설계' } })
    fireEvent.click(screen.getByText('저장'))
    await waitFor(() => expect(documentApi.createVersion).toHaveBeenCalledWith('doc-1', { label: 'v1 - 초기 설계' }))
  })

  // F-VER-05: 버전 없을 때 메시지 표시
  test('F-VER-05: 버전 없을 때 안내 메시지 표시', async () => {
    vi.mocked(documentApi.listVersions).mockResolvedValueOnce([])
    render(<VersionHistoryPanel documentId="doc-1" />)
    await waitFor(() => expect(screen.getByText(/저장된 버전이 없습니다/)).toBeDefined())
  })
})
