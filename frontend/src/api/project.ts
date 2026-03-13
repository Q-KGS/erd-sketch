import { apiClient } from './client'
import type { Project, DbType } from '@/models'

export const projectApi = {
  create: (workspaceId: string, data: { name: string; description?: string; targetDbType: DbType }) =>
    apiClient.post<Project>(`/workspaces/${workspaceId}/projects`, data).then((r) => r.data),

  list: (workspaceId: string) =>
    apiClient.get<Project[]>(`/workspaces/${workspaceId}/projects`).then((r) => r.data),

  get: (projectId: string) =>
    apiClient.get<Project>(`/projects/${projectId}`).then((r) => r.data),

  update: (projectId: string, data: Partial<{ name: string; description: string; targetDbType: DbType }>) =>
    apiClient.patch<Project>(`/projects/${projectId}`, data).then((r) => r.data),

  delete: (projectId: string) =>
    apiClient.delete(`/projects/${projectId}`),
}
