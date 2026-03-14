import { useEffect, useState } from 'react'
import { templateApi, type TemplateInfo } from '@/api/template'
import toast from 'react-hot-toast'

interface Props {
  onClose: () => void
  onApply: (tables: Record<string, unknown>[], relationships: Record<string, unknown>[]) => void
}

export default function TemplatePicker({ onClose, onApply }: Props) {
  const [templates, setTemplates] = useState<TemplateInfo[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    templateApi.list().then(setTemplates).catch(() => toast.error('템플릿 목록을 불러오지 못했습니다.'))
  }, [])

  const handleApply = async () => {
    if (!selected) {
      toast.error('템플릿을 선택해주세요.')
      return
    }
    setLoading(true)
    try {
      const schema = await templateApi.apply(selected)
      onApply(schema.tables, schema.relationships)
      toast.success('템플릿이 적용되었습니다.')
      onClose()
    } catch {
      toast.error('템플릿 적용에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">프로젝트 템플릿</h2>
        <div className="space-y-3">
          {templates.map((tpl) => (
            <button
              key={tpl.type}
              onClick={() => setSelected(tpl.type)}
              className={`w-full text-left p-4 rounded-lg border-2 transition-colors ${
                selected === tpl.type
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                  : 'border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500'
              }`}
            >
              <div className="font-medium text-gray-900 dark:text-gray-100">{tpl.name}</div>
              <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">{tpl.description}</div>
            </button>
          ))}
          {templates.length === 0 && (
            <div className="text-center text-sm text-gray-400 py-4">템플릿을 불러오는 중...</div>
          )}
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
          >
            취소
          </button>
          <button
            onClick={handleApply}
            disabled={loading || !selected}
            className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {loading ? '적용 중...' : '적용'}
          </button>
        </div>
      </div>
    </div>
  )
}
