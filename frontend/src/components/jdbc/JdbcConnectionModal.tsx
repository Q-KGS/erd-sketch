import { useState } from 'react'
import { jdbcApi, type JdbcConnectionRequest } from '@/api/jdbc'
import type { DbType } from '@/models'
import toast from 'react-hot-toast'

interface Props {
  onClose: () => void
  onImport: (tables: Record<string, unknown>[]) => void
}

const DB_TYPES: DbType[] = ['POSTGRESQL', 'MYSQL', 'ORACLE', 'MSSQL']

export default function JdbcConnectionModal({ onClose, onImport }: Props) {
  const [form, setForm] = useState<JdbcConnectionRequest>({
    host: 'localhost',
    port: 5432,
    database: '',
    username: '',
    password: '',
    dbType: 'POSTGRESQL',
  })
  const [testing, setTesting] = useState(false)
  const [extracting, setExtracting] = useState(false)

  const update = (key: keyof JdbcConnectionRequest, value: string | number) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const handleTest = async () => {
    setTesting(true)
    try {
      await jdbcApi.testConnection(form)
      toast.success('연결 성공!')
    } catch {
      toast.error('연결 실패. 접속 정보를 확인해주세요.')
    } finally {
      setTesting(false)
    }
  }

  const handleExtract = async () => {
    setExtracting(true)
    try {
      const tables = await jdbcApi.extract(form)
      onImport(tables)
      toast.success(`${tables.length}개 테이블을 가져왔습니다.`)
      onClose()
    } catch {
      toast.error('스키마 가져오기에 실패했습니다.')
    } finally {
      setExtracting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">JDBC 연결</h2>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">호스트</label>
              <input
                type="text"
                className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                value={form.host}
                onChange={(e) => update('host', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">포트</label>
              <input
                type="number"
                className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                value={form.port}
                onChange={(e) => update('port', parseInt(e.target.value) || 5432)}
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">데이터베이스</label>
            <input
              type="text"
              className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={form.database}
              onChange={(e) => update('database', e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">사용자</label>
              <input
                type="text"
                className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                value={form.username}
                onChange={(e) => update('username', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">비밀번호</label>
              <input
                type="password"
                className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                value={form.password}
                onChange={(e) => update('password', e.target.value)}
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">DB 타입</label>
            <select
              className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={form.dbType}
              onChange={(e) => update('dbType', e.target.value)}
            >
              {DB_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
        </div>
        <div className="flex justify-between mt-5">
          <button
            onClick={handleTest}
            disabled={testing}
            className="px-4 py-2 text-sm border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
          >
            {testing ? '테스트 중...' : '연결 테스트'}
          </button>
          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
            >
              취소
            </button>
            <button
              onClick={handleExtract}
              disabled={extracting}
              className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
            >
              {extracting ? '가져오는 중...' : '스키마 가져오기'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
