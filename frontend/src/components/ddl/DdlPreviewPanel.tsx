import { useState, useEffect, useCallback } from 'react'
import { documentApi } from '@/api/document'
import { useEditorStore } from '@/store/editorStore'
import { generateDdlLocal } from '@/utils/ddlGenerator'
import type { ErdSchema, DbType } from '@/models'
import toast from 'react-hot-toast'

interface Props {
  schema: ErdSchema
  documentId: string
}

export default function DdlPreviewPanel({ schema, documentId }: Props) {
  const { targetDbType } = useEditorStore()
  const [ddl, setDdl] = useState('')
  const [dialect, setDialect] = useState<DbType>(targetDbType)
  const [warnings, setWarnings] = useState<string[]>([])
  const [isPending, setIsPending] = useState(false)

  const generateDdl = useCallback(async () => {
    setIsPending(true)
    try {
      const data = await documentApi.generateDdl(documentId, { dialect, includeDrops: false })
      setDdl(data.ddl)
      setWarnings(data.warnings)
    } catch {
      // 백엔드 API 실패 시 클라이언트 사이드 DDL 생성
      const result = generateDdlLocal(schema, dialect)
      setDdl(result.ddl)
      setWarnings(result.warnings.length > 0 ? result.warnings : [])
      if (Object.keys(schema.tables).length > 0) {
        toast('백엔드 연결 없이 로컬에서 DDL을 생성했습니다.', { icon: 'ℹ️' })
      }
    } finally {
      setIsPending(false)
    }
  }, [dialect, documentId, schema])

  useEffect(() => {
    generateDdl()
  }, [generateDdl])

  const copyToClipboard = () => {
    navigator.clipboard.writeText(ddl)
    toast.success('DDL이 클립보드에 복사되었습니다.')
  }

  const downloadSql = () => {
    const blob = new Blob([ddl], { type: 'text/sql' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `schema_${dialect.toLowerCase()}.sql`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <aside className="w-96 bg-white border-l border-gray-200 flex flex-col overflow-hidden">
      <div className="p-3 border-b border-gray-100 flex items-center justify-between gap-2">
        <span className="text-xs font-semibold text-gray-500 uppercase">DDL 미리보기</span>
        <div className="flex items-center gap-2">
          <select
            value={dialect}
            onChange={(e) => setDialect(e.target.value as DbType)}
            className="text-xs border border-gray-200 rounded px-2 py-1"
          >
            <option value="POSTGRESQL">PostgreSQL</option>
            <option value="MYSQL">MySQL</option>
            <option value="ORACLE">Oracle</option>
            <option value="MSSQL">MSSQL</option>
          </select>
          <button onClick={copyToClipboard} className="text-xs px-2 py-1 bg-gray-100 hover:bg-gray-200 rounded">복사</button>
          <button onClick={downloadSql} className="text-xs px-2 py-1 bg-primary-50 text-primary-700 hover:bg-primary-100 rounded">다운로드</button>
        </div>
      </div>

      {warnings.length > 0 && (
        <div className="px-3 py-2 bg-yellow-50 border-b border-yellow-100">
          {warnings.map((w, i) => (
            <p key={i} className="text-xs text-yellow-700">⚠ {w}</p>
          ))}
        </div>
      )}

      <div className="flex-1 overflow-y-auto">
        {isPending ? (
          <div className="flex items-center justify-center h-32 text-gray-400 text-sm">생성 중...</div>
        ) : (
          <pre className="p-3 text-xs font-mono text-gray-800 whitespace-pre-wrap leading-5">{ddl || '테이블을 추가하면 DDL이 표시됩니다.'}</pre>
        )}
      </div>
    </aside>
  )
}
