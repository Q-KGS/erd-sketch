import { apiClient } from './client'
import type { ErdDocument, ErdSchema, CanvasSettings, DocumentVersion, DdlGenerateRequest, DdlGenerateResponse, DdlParseRequest, DdlParseResponse } from '@/models'

export const documentApi = {
  create: (projectId: string, data: { name: string }) =>
    apiClient.post<ErdDocument>(`/projects/${projectId}/documents`, data).then((r) => r.data),

  list: (projectId: string) =>
    apiClient.get<ErdDocument[]>(`/projects/${projectId}/documents`).then((r) => r.data),

  get: (documentId: string) =>
    apiClient.get<ErdDocument>(`/documents/${documentId}`).then((r) => r.data),

  delete: (documentId: string) =>
    apiClient.delete(`/documents/${documentId}`),

  updateSettings: (documentId: string, settings: CanvasSettings) =>
    apiClient.patch<ErdDocument>(`/documents/${documentId}/settings`, settings).then((r) => r.data),

  getSchema: (documentId: string) =>
    apiClient.get<ErdSchema>(`/documents/${documentId}/schema`).then((r) => r.data),

  // Versions
  listVersions: (documentId: string) =>
    apiClient.get<DocumentVersion[]>(`/documents/${documentId}/versions`).then((r) => r.data),

  createVersion: (documentId: string, data: { label?: string }) =>
    apiClient.post<DocumentVersion>(`/documents/${documentId}/versions`, data).then((r) => r.data),

  getVersion: (documentId: string, versionId: string) =>
    apiClient.get<DocumentVersion>(`/documents/${documentId}/versions/${versionId}`).then((r) => r.data),

  restoreVersion: (documentId: string, versionId: string) =>
    apiClient.post(`/documents/${documentId}/versions/${versionId}/restore`),

  // DDL
  generateDdl: (documentId: string, request: DdlGenerateRequest) =>
    apiClient.post<DdlGenerateResponse>(`/documents/${documentId}/ddl/generate`, request).then((r) => r.data),

  parseDdl: (documentId: string, request: DdlParseRequest) =>
    apiClient.post<DdlParseResponse>(`/documents/${documentId}/ddl/parse`, request).then((r) => r.data),
}
