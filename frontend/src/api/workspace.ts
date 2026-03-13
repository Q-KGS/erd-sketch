import { apiClient } from './client'
import type { Workspace, WorkspaceMember, WorkspaceRole } from '@/models'

export const workspaceApi = {
  create: (data: { name: string; slug: string }) =>
    apiClient.post<Workspace>('/workspaces', data).then((r) => r.data),

  list: () =>
    apiClient.get<Workspace[]>('/workspaces').then((r) => r.data),

  get: (workspaceId: string) =>
    apiClient.get<Workspace>(`/workspaces/${workspaceId}`).then((r) => r.data),

  update: (workspaceId: string, data: Partial<{ name: string }>) =>
    apiClient.patch<Workspace>(`/workspaces/${workspaceId}`, data).then((r) => r.data),

  delete: (workspaceId: string) =>
    apiClient.delete(`/workspaces/${workspaceId}`),

  getMembers: (workspaceId: string) =>
    apiClient.get<WorkspaceMember[]>(`/workspaces/${workspaceId}/members`).then((r) => r.data),

  inviteMember: (workspaceId: string, data: { email: string; role: WorkspaceRole }) =>
    apiClient.post<WorkspaceMember>(`/workspaces/${workspaceId}/members/invite`, data).then((r) => r.data),

  updateMemberRole: (workspaceId: string, userId: string, role: WorkspaceRole) =>
    apiClient.patch<WorkspaceMember>(`/workspaces/${workspaceId}/members/${userId}`, { role }).then((r) => r.data),

  removeMember: (workspaceId: string, userId: string) =>
    apiClient.delete(`/workspaces/${workspaceId}/members/${userId}`),
}
