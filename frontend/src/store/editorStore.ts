import { create } from 'zustand'
import type { DbType } from '@/models'

type SelectionType = 'table' | 'relationship' | 'note' | null

interface EditorState {
  // Document
  documentId: string | null
  projectId: string | null
  targetDbType: DbType

  // Selection
  selectedId: string | null
  selectionType: SelectionType

  // UI State
  isDdlPanelOpen: boolean
  isCommentPanelOpen: boolean
  isVersionPanelOpen: boolean
  searchQuery: string

  // Actions
  setDocument: (documentId: string, projectId: string) => void
  setTargetDbType: (dbType: DbType) => void
  setSelection: (id: string | null, type: SelectionType) => void
  toggleDdlPanel: () => void
  toggleCommentPanel: () => void
  toggleVersionPanel: () => void
  setSearchQuery: (query: string) => void
}

export const useEditorStore = create<EditorState>((set) => ({
  documentId: null,
  projectId: null,
  targetDbType: 'POSTGRESQL',
  selectedId: null,
  selectionType: null,
  isDdlPanelOpen: false,
  isCommentPanelOpen: false,
  isVersionPanelOpen: false,
  searchQuery: '',

  setDocument: (documentId, projectId) => set({ documentId, projectId }),
  setTargetDbType: (targetDbType) => set({ targetDbType }),
  setSelection: (selectedId, selectionType) => set({ selectedId, selectionType }),
  toggleDdlPanel: () => set((s) => ({ isDdlPanelOpen: !s.isDdlPanelOpen })),
  toggleCommentPanel: () => set((s) => ({ isCommentPanelOpen: !s.isCommentPanelOpen })),
  toggleVersionPanel: () => set((s) => ({ isVersionPanelOpen: !s.isVersionPanelOpen })),
  setSearchQuery: (searchQuery) => set({ searchQuery }),
}))
