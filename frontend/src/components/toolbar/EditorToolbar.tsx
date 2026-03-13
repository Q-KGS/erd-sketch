import { useState, useRef, useEffect } from 'react'
import type { Awareness } from 'y-protocols/awareness'
import { useEditorStore } from '@/store/editorStore'
import type { DbType } from '@/models'

interface Props {
  projectName: string
  targetDbType: DbType
  isConnected: boolean
  isReadOnly: boolean
  isVersionPanelOpen: boolean
  isCommentPanelOpen: boolean
  onAutoLayout: () => void
  onAddTable: () => void
  onExportJson: () => void
  onExportPng: () => void
  onExportPdf: () => void
  onToggleVersion: () => void
  onToggleComment: () => void
  awareness: Awareness | null
  documentId: string
}

export default function EditorToolbar({
  projectName, targetDbType, isConnected, isReadOnly,
  isVersionPanelOpen, isCommentPanelOpen,
  onAutoLayout, onAddTable, onExportJson, onExportPng, onExportPdf,
  onToggleVersion, onToggleComment,
}: Props) {
  const { toggleDdlPanel, isDdlPanelOpen } = useEditorStore()
  const [exportMenuOpen, setExportMenuOpen] = useState(false)
  const exportMenuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (exportMenuRef.current && !exportMenuRef.current.contains(e.target as Node)) {
        setExportMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <header className="h-12 bg-white border-b border-gray-200 flex items-center px-4 gap-3 shadow-sm">
      <a href="/" className="font-bold text-primary-600 text-lg shrink-0">ErdSketch</a>
      <span className="text-gray-300">/</span>
      <span className="font-medium text-gray-700 truncate">{projectName}</span>

      <div className="flex-1" />

      {/* DB Type Badge */}
      <span className="px-2 py-1 bg-blue-50 text-blue-700 text-xs rounded font-mono">{targetDbType}</span>

      {/* Connection Status */}
      <div className="flex items-center gap-1">
        <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-400' : 'bg-gray-300'}`} />
        <span className="text-xs text-gray-500">{isConnected ? '연결됨' : '오프라인'}</span>
      </div>

      <div className="w-px h-5 bg-gray-200" />

      {/* Read-only badge */}
      {isReadOnly && (
        <span className="px-2 py-1 bg-yellow-50 text-yellow-700 text-xs rounded font-medium">읽기 전용</span>
      )}

      {/* Tools */}
      {!isReadOnly && (
        <button
          onClick={onAddTable}
          className="px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 rounded-lg"
          title="테이블 추가"
        >
          + 테이블
        </button>
      )}

      <button
        onClick={onToggleVersion}
        className={`px-3 py-1.5 text-sm rounded-lg ${isVersionPanelOpen ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:bg-gray-100'}`}
      >
        버전
      </button>

      <button
        onClick={onToggleComment}
        className={`px-3 py-1.5 text-sm rounded-lg ${isCommentPanelOpen ? 'bg-green-100 text-green-700' : 'text-gray-600 hover:bg-gray-100'}`}
      >
        댓글
      </button>

      <button
        onClick={onAutoLayout}
        className="px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 rounded-lg"
        title="자동 레이아웃"
      >
        자동 정렬
      </button>

      <button
        onClick={toggleDdlPanel}
        className={`px-3 py-1.5 text-sm rounded-lg ${isDdlPanelOpen ? 'bg-primary-100 text-primary-700' : 'text-gray-600 hover:bg-gray-100'}`}
      >
        DDL 보기
      </button>

      <div className="w-px h-5 bg-gray-200" />

      {/* Export Dropdown */}
      <div className="relative" ref={exportMenuRef}>
        <button
          onClick={() => setExportMenuOpen(!exportMenuOpen)}
          className="px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 rounded-lg flex items-center gap-1"
        >
          내보내기
          <svg className={`w-3.5 h-3.5 transition-transform ${exportMenuOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        {exportMenuOpen && (
          <div className="absolute right-0 mt-1 w-36 bg-white border border-gray-200 rounded-lg shadow-lg z-50 py-1">
            <button
              onClick={() => { onExportJson(); setExportMenuOpen(false) }}
              className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              JSON
            </button>
            <button
              onClick={() => { onExportPng(); setExportMenuOpen(false) }}
              className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              PNG
            </button>
            <button
              onClick={() => { onExportPdf(); setExportMenuOpen(false) }}
              className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            >
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              PDF
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
