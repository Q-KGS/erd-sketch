import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import type { Comment } from '@/models'

vi.mock('@/api/comment', () => ({
  commentApi: {
    list: vi.fn(async () => []),
    create: vi.fn(async (docId, data) => ({
      id: 'cmt-new',
      documentId: docId,
      authorId: 'u1',
      author: { id: 'u1', email: 'a@a.com', displayName: 'Alice', createdAt: '' },
      targetType: data.targetType,
      content: data.content,
      resolved: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    })),
    resolve: vi.fn(async () => ({ id: 'cmt-1', resolved: true })),
    reopen: vi.fn(async () => ({})),
    delete: vi.fn(async () => {}),
  },
}))

import CommentPanel from './CommentPanel'
import { commentApi } from '@/api/comment'

const mockComments: Comment[] = [
  {
    id: 'cmt-1', documentId: 'doc-1', authorId: 'u1',
    author: { id: 'u1', email: 'a@a.com', displayName: 'Alice', createdAt: '' },
    targetType: 'CANVAS', content: '첫 번째 댓글', resolved: false,
    createdAt: '2024-01-01T00:00:00Z', updatedAt: '2024-01-01T00:00:00Z',
    replies: [],
  },
  {
    id: 'cmt-2', documentId: 'doc-1', authorId: 'u2',
    author: { id: 'u2', email: 'b@b.com', displayName: 'Bob', createdAt: '' },
    targetType: 'CANVAS', content: '두 번째 댓글', resolved: true,
    createdAt: '2024-01-02T00:00:00Z', updatedAt: '2024-01-02T00:00:00Z',
    replies: [],
  },
]

describe('CommentPanel', () => {
  beforeEach(() => { vi.clearAllMocks() })

  // F-CMT-01: 댓글 목록 렌더링
  test('F-CMT-01: 댓글 목록이 작성자 이름과 내용과 함께 렌더링됨', async () => {
    vi.mocked(commentApi.list).mockResolvedValueOnce(mockComments)
    render(<CommentPanel documentId="doc-1" />)
    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeDefined()
      expect(screen.getByText('첫 번째 댓글')).toBeDefined()
    })
  })

  // F-CMT-02: 댓글 작성
  test('F-CMT-02: 댓글 작성 후 목록 갱신', async () => {
    vi.mocked(commentApi.list).mockResolvedValue([])
    render(<CommentPanel documentId="doc-1" />)
    const textarea = screen.getByPlaceholderText(/댓글/)
    fireEvent.change(textarea, { target: { value: '새 댓글입니다' } })
    fireEvent.click(screen.getByText('전송'))
    await waitFor(() => expect(commentApi.create).toHaveBeenCalledWith('doc-1', expect.objectContaining({ content: '새 댓글입니다' })))
  })

  // F-CMT-03: 대댓글 작성
  test('F-CMT-03: 답글 클릭 시 들여쓰기 입력창 표시', async () => {
    vi.mocked(commentApi.list).mockResolvedValueOnce([mockComments[0]])
    render(<CommentPanel documentId="doc-1" />)
    await waitFor(() => expect(screen.getByText('첫 번째 댓글')).toBeDefined())
    fireEvent.click(screen.getByText('답글'))
    expect(screen.getByPlaceholderText(/답글/)).toBeDefined()
  })

  // F-CMT-04: 해결 버튼
  test('F-CMT-04: 해결 버튼 클릭 시 resolve 호출', async () => {
    vi.mocked(commentApi.list).mockResolvedValueOnce([mockComments[0]])
    render(<CommentPanel documentId="doc-1" />)
    await waitFor(() => expect(screen.getByText('해결')).toBeDefined())
    fireEvent.click(screen.getByText('해결'))
    expect(commentApi.resolve).toHaveBeenCalledWith('cmt-1')
  })

  // F-CMT-05: 미해결 필터
  test('F-CMT-05: 미해결 필터 체크 시 list 재호출', async () => {
    vi.mocked(commentApi.list).mockResolvedValue(mockComments)
    render(<CommentPanel documentId="doc-1" />)
    await waitFor(() => expect(commentApi.list).toHaveBeenCalledTimes(1))
    fireEvent.click(screen.getByLabelText(/미해결만/))
    await waitFor(() => expect(commentApi.list).toHaveBeenCalledTimes(2))
    expect(vi.mocked(commentApi.list).mock.calls[1][1]).toBe(false)
  })
})
