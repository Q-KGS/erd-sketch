import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import type { ErdSchema } from '@/models'

vi.mock('@/api/document', () => ({
  documentApi: {
    generateDdl: vi.fn(async () => ({ ddl: 'CREATE TABLE "users" ();', warnings: [] })),
  },
}))

vi.mock('@/store/editorStore', () => ({
  useEditorStore: () => ({ targetDbType: 'POSTGRESQL' }),
}))

vi.mock('react-hot-toast', () => ({
  default: Object.assign(vi.fn(), { success: vi.fn() }),
}))

vi.mock('@/utils/ddlGenerator', () => ({
  generateDdlLocal: vi.fn(() => ({ ddl: 'CREATE TABLE local_fallback ();', warnings: [] })),
}))

import DdlPreviewPanel from './DdlPreviewPanel'
import { documentApi } from '@/api/document'

const emptySchema: ErdSchema = { tables: {}, relationships: {}, notes: {} }
const schemaWithTable: ErdSchema = {
  tables: {
    't1': { id: 't1', name: 'users', position: { x: 0, y: 0 }, columns: [], indexes: [] },
  },
  relationships: {},
  notes: {},
}

describe('DdlPreviewPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(global.navigator, 'clipboard', { value: { writeText: vi.fn() }, configurable: true, writable: true })
    global.URL.createObjectURL = vi.fn(() => 'blob:mock')
    global.URL.revokeObjectURL = vi.fn()
  })

  // F-DDL-01: 빈 스키마 → "테이블을 추가하면..." 표시
  test('F-DDL-01: 빈 스키마에서 패널이 렌더링됨', async () => {
    vi.mocked(documentApi.generateDdl).mockResolvedValueOnce({ ddl: '', warnings: [] })
    render(<DdlPreviewPanel schema={emptySchema} documentId="doc-1" />)
    await waitFor(() => {
      expect(screen.getByText(/테이블을 추가하면/)).toBeDefined()
    })
  })

  // F-DDL-02: 방언 변경 → generateDdl 재호출
  test('F-DDL-02: 방언 변경 시 generateDdl 재호출', async () => {
    render(<DdlPreviewPanel schema={schemaWithTable} documentId="doc-1" />)
    await waitFor(() => expect(documentApi.generateDdl).toHaveBeenCalledTimes(1))

    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'MYSQL' } })

    await waitFor(() => expect(documentApi.generateDdl).toHaveBeenCalledTimes(2))
    expect(vi.mocked(documentApi.generateDdl).mock.calls[1][1]).toMatchObject({ dialect: 'MYSQL' })
  })

  // F-DDL-03: "복사" 클릭 → navigator.clipboard.writeText 호출
  test('F-DDL-03: 복사 버튼 클릭 시 클립보드에 복사됨', async () => {
    render(<DdlPreviewPanel schema={schemaWithTable} documentId="doc-1" />)
    await waitFor(() => expect(documentApi.generateDdl).toHaveBeenCalled())

    fireEvent.click(screen.getByText('복사'))
    expect(navigator.clipboard.writeText).toHaveBeenCalledOnce()
  })

  // F-DDL-04: "다운로드" 클릭 → <a> 요소 생성 및 click
  test('F-DDL-04: 다운로드 버튼 클릭 시 파일 다운로드 트리거됨', async () => {
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    // only intercept 'a' tag; let React create real elements for everything else
    const originalCreateElement = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string, ...args: unknown[]) => {
      if (tag === 'a') return mockAnchor as unknown as HTMLElement
      return originalCreateElement(tag as keyof HTMLElementTagNameMap, ...(args as []))
    })

    render(<DdlPreviewPanel schema={schemaWithTable} documentId="doc-1" />)
    await waitFor(() => expect(documentApi.generateDdl).toHaveBeenCalled())

    fireEvent.click(screen.getByText('다운로드'))
    expect(mockAnchor.click).toHaveBeenCalledOnce()

    vi.restoreAllMocks()
  })

  // F-DDL-05: warnings 존재 시 노란 경고 배너 표시
  test('F-DDL-05: warnings가 있으면 경고 배너 표시', async () => {
    vi.mocked(documentApi.generateDdl).mockResolvedValueOnce({
      ddl: 'CREATE TABLE [users] ();',
      warnings: ['JSONB mapped to NVARCHAR(MAX)'],
    })
    render(<DdlPreviewPanel schema={schemaWithTable} documentId="doc-1" />)
    await waitFor(() => {
      expect(screen.getByText(/JSONB mapped to NVARCHAR/)).toBeDefined()
    })
  })
})
