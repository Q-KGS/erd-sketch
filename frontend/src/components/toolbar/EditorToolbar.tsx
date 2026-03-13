import type { Awareness } from 'y-protocols/awareness'
import { useEditorStore } from '@/store/editorStore'
import type { DbType } from '@/models'

interface Props {
  projectName: string
  targetDbType: DbType
  isConnected: boolean
  onAutoLayout: () => void
  onAddTable: () => void
  awareness: Awareness | null
}

export default function EditorToolbar({ projectName, targetDbType, isConnected, onAutoLayout, onAddTable }: Props) {
  const { toggleDdlPanel, isDdlPanelOpen } = useEditorStore()

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

      {/* Tools */}
      <button
        onClick={onAddTable}
        className="px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 rounded-lg"
        title="테이블 추가"
      >
        + 테이블
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
    </header>
  )
}
