import { apiClient } from './client'
import type { Comment } from '@/models'

export interface CreateCommentData {
  targetType: string
  targetId?: string
  content: string
  parentId?: string
}

export const commentApi = {
  list: (documentId: string, resolved?: boolean) => {
    const params = resolved !== undefined ? { resolved } : {}
    return apiClient.get<Comment[]>(`/documents/${documentId}/comments`, { params }).then(r => r.data)
  },
  create: (documentId: string, data: CreateCommentData) =>
    apiClient.post<Comment>(`/documents/${documentId}/comments`, data).then(r => r.data),
  update: (commentId: string, content: string) =>
    apiClient.patch<Comment>(`/comments/${commentId}`, { content }).then(r => r.data),
  delete: (commentId: string) =>
    apiClient.delete(`/comments/${commentId}`),
  resolve: (commentId: string) =>
    apiClient.post<Comment>(`/comments/${commentId}/resolve`).then(r => r.data),
  reopen: (commentId: string) =>
    apiClient.post<Comment>(`/comments/${commentId}/reopen`).then(r => r.data),
}
