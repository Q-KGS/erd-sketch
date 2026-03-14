import { useState } from 'react'
import { dbmlApi } from '@/api/dbml'
import toast from 'react-hot-toast'

interface Props {
  onClose: () => void
  onImport: (tables: Record<string, unknown>[], relationships: Record<string, unknown>[]) => void
}

export default function DbmlImportModal({ onClose, onImport }: Props) {
  const [dbmlText, setDbmlText] = useState('')
  const [warnings, setWarnings] = useState<string[]>([])
  const [loading, setLoading] = useState(false)

  const handleImport = async () => {
    if (!dbmlText.trim()) {
      toast.error('DBML 내용을 입력해주세요.')
      return
    }
    setLoading(true)
    try {
      const result = await dbmlApi.parse(dbmlText)
      setWarnings(result.warnings)
      onImport(result.tables, result.relationships)
      toast.success(`${result.tables.length}개 테이블, ${result.relationships.length}개 관계를 가져왔습니다.`)
      onClose()
    } catch {
      toast.error('DBML 파싱에 실패했습니다. 문법을 확인해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">DBML 가져오기</h2>
        <textarea
          className="w-full h-48 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm font-mono bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
          placeholder={`Table users {\n  id uuid [pk]\n  email varchar [unique]\n}\n\nRef: orders.user_id > users.id`}
          value={dbmlText}
          onChange={(e) => setDbmlText(e.target.value)}
        />
        {warnings.length > 0 && (
          <div className="mt-2 p-2 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded text-xs text-yellow-700 dark:text-yellow-400">
            {warnings.map((w, i) => <div key={i}>{w}</div>)}
          </div>
        )}
        <div className="flex justify-end gap-2 mt-4">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
          >
            취소
          </button>
          <button
            onClick={handleImport}
            disabled={loading}
            className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {loading ? '가져오는 중...' : '가져오기'}
          </button>
        </div>
      </div>
    </div>
  )
}
