import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import { documentApi } from '@/api/document'
import type { DocumentVersion } from '@/models'

interface Props {
  documentId: string
}

export default function VersionHistoryPanel({ documentId }: Props) {
  const [versions, setVersions] = useState<DocumentVersion[]>([])
  const [selected, setSelected] = useState<DocumentVersion | null>(null)
  const [showSaveForm, setShowSaveForm] = useState(false)
  const [label, setLabel] = useState('')

  useEffect(() => {
    documentApi.listVersions(documentId)
      .then(setVersions)
      .catch(() => toast.error('버전 히스토리를 불러오는 데 실패했습니다.'))
  }, [documentId])

  const handleSave = async () => {
    try {
      await documentApi.createVersion(documentId, { label: label || undefined })
      const updated = await documentApi.listVersions(documentId)
      setVersions(updated)
      setLabel('')
      setShowSaveForm(false)
      toast.success('버전이 저장되었습니다.')
    } catch {
      toast.error('버전 저장에 실패했습니다.')
    }
  }

  const handleRestore = async (versionId: string) => {
    if (!window.confirm('이 버전으로 복원하시겠습니까? 현재 작업 내용이 대체됩니다.')) return
    try {
      await documentApi.restoreVersion(documentId, versionId)
      toast.success('버전이 복원되었습니다. 페이지를 새로고침하면 반영됩니다.')
    } catch {
      toast.error('버전 복원에 실패했습니다.')
    }
  }

  return (
    <div className="p-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-sm">버전 히스토리</h3>
        <button onClick={() => setShowSaveForm(true)} className="text-xs bg-blue-500 text-white px-2 py-1 rounded">
          새 버전 저장
        </button>
      </div>
      {showSaveForm && (
        <div className="mb-3 flex gap-2">
          <input
            value={label}
            onChange={e => setLabel(e.target.value)}
            placeholder="버전 레이블 (선택)"
            className="flex-1 border text-xs px-2 py-1 rounded"
          />
          <button onClick={handleSave} className="text-xs bg-green-500 text-white px-2 py-1 rounded">저장</button>
          <button onClick={() => setShowSaveForm(false)} className="text-xs bg-gray-300 px-2 py-1 rounded">취소</button>
        </div>
      )}
      {versions.length === 0 ? (
        <p className="text-xs text-gray-500">저장된 버전이 없습니다</p>
      ) : (
        <ul className="space-y-1">
          {versions.map(v => (
            <li
              key={v.id}
              onClick={() => setSelected(v)}
              className={`cursor-pointer text-xs p-2 rounded border ${selected?.id === v.id ? 'border-blue-500 bg-blue-50' : 'border-transparent hover:bg-gray-50'}`}
            >
              <div className="font-medium">{v.label ?? `v${v.versionNumber}`}</div>
              <div className="text-gray-500">{new Date(v.createdAt).toLocaleString('ko-KR')}</div>
              {selected?.id === v.id && (
                <button
                  onClick={e => { e.stopPropagation(); handleRestore(v.id) }}
                  className="mt-1 text-xs bg-orange-500 text-white px-2 py-1 rounded"
                >
                  복원
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
