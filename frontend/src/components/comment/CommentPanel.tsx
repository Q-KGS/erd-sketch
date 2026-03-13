import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import { commentApi } from '@/api/comment'
import type { Comment } from '@/models'

interface Props {
  documentId: string
}

export default function CommentPanel({ documentId }: Props) {
  const [comments, setComments] = useState<Comment[]>([])
  const [content, setContent] = useState('')
  const [replyParentId, setReplyParentId] = useState<string | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [onlyUnresolved, setOnlyUnresolved] = useState(false)

  const loadComments = (resolved?: boolean) => {
    commentApi.list(documentId, resolved)
      .then(setComments)
      .catch(() => toast.error('댓글을 불러오는 데 실패했습니다.'))
  }

  useEffect(() => {
    loadComments()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [documentId])

  const handleSubmit = async () => {
    if (!content.trim()) return
    try {
      await commentApi.create(documentId, { targetType: 'CANVAS', content })
      setContent('')
      loadComments(onlyUnresolved ? false : undefined)
    } catch {
      toast.error('댓글 전송에 실패했습니다.')
    }
  }

  const handleReplySubmit = async (parentId: string) => {
    if (!replyContent.trim()) return
    try {
      await commentApi.create(documentId, { targetType: 'CANVAS', content: replyContent, parentId })
      setReplyContent('')
      setReplyParentId(null)
      loadComments(onlyUnresolved ? false : undefined)
    } catch {
      toast.error('답글 전송에 실패했습니다.')
    }
  }

  const handleResolve = async (commentId: string) => {
    try {
      await commentApi.resolve(commentId)
      loadComments(onlyUnresolved ? false : undefined)
    } catch {
      toast.error('댓글 해결 처리에 실패했습니다.')
    }
  }

  const handleFilterChange = (checked: boolean) => {
    setOnlyUnresolved(checked)
    loadComments(checked ? false : undefined)
  }

  return (
    <div className="p-4 flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-sm">댓글</h3>
        <label htmlFor="unresolved-filter" className="flex items-center gap-1 text-xs text-gray-600 cursor-pointer">
          <input
            id="unresolved-filter"
            type="checkbox"
            checked={onlyUnresolved}
            onChange={e => handleFilterChange(e.target.checked)}
          />
          미해결만 보기
        </label>
      </div>

      <div className="flex flex-col gap-2">
        <textarea
          value={content}
          onChange={e => setContent(e.target.value)}
          placeholder="댓글을 입력하세요"
          className="border text-xs px-2 py-1 rounded resize-none"
          rows={2}
        />
        <button onClick={handleSubmit} className="self-end text-xs bg-blue-500 text-white px-2 py-1 rounded">
          전송
        </button>
      </div>

      <ul className="space-y-2">
        {comments.map(comment => (
          <li key={comment.id} className={`text-xs border rounded p-2 ${comment.resolved ? 'opacity-50' : ''}`}>
            <div className="flex items-center justify-between">
              <span className="font-semibold">{comment.author.displayName}</span>
              <span className="text-gray-400">{new Date(comment.createdAt).toLocaleString('ko-KR')}</span>
            </div>
            <p className={`mt-1 ${comment.resolved ? 'line-through' : ''}`}>{comment.content}</p>
            <div className="flex gap-2 mt-1">
              {!comment.resolved && (
                <button
                  onClick={() => handleResolve(comment.id)}
                  className="text-xs text-green-600 hover:underline"
                >
                  해결
                </button>
              )}
              <button
                onClick={() => {
                  setReplyParentId(comment.id)
                  setReplyContent('')
                }}
                className="text-xs text-blue-600 hover:underline"
              >
                답글
              </button>
            </div>
            {replyParentId === comment.id && (
              <div className="ml-4 mt-2 flex flex-col gap-1">
                <textarea
                  value={replyContent}
                  onChange={e => setReplyContent(e.target.value)}
                  placeholder="답글을 입력하세요"
                  className="border text-xs px-2 py-1 rounded resize-none"
                  rows={2}
                />
                <div className="flex gap-1">
                  <button
                    onClick={() => handleReplySubmit(comment.id)}
                    className="text-xs bg-blue-500 text-white px-2 py-1 rounded"
                  >
                    전송
                  </button>
                  <button
                    onClick={() => setReplyParentId(null)}
                    className="text-xs bg-gray-300 px-2 py-1 rounded"
                  >
                    취소
                  </button>
                </div>
              </div>
            )}
            {comment.replies && comment.replies.length > 0 && (
              <ul className="ml-4 mt-2 space-y-1">
                {comment.replies.map(reply => (
                  <li key={reply.id} className={`text-xs border rounded p-2 ${reply.resolved ? 'opacity-50' : ''}`}>
                    <div className="flex items-center justify-between">
                      <span className="font-semibold">{reply.author.displayName}</span>
                      <span className="text-gray-400">{new Date(reply.createdAt).toLocaleString('ko-KR')}</span>
                    </div>
                    <p className={reply.resolved ? 'line-through' : ''}>{reply.content}</p>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
