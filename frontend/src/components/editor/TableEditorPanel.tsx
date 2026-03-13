import { useState, useCallback } from 'react'
import * as Y from 'yjs'
import { v4 as uuidv4 } from 'uuid'
import type { TableDef, ColumnDef } from '@/models'
import { useEditorStore } from '@/store/editorStore'

interface Props {
  table: TableDef
  ydoc: Y.Doc | null
}

export default function TableEditorPanel({ table, ydoc }: Props) {
  const { setSelection } = useEditorStore()
  const [editingName, setEditingName] = useState(false)

  const updateTableField = useCallback((field: string, value: unknown) => {
    if (!ydoc) return
    const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
    const yTable = tablesMap.get(table.id)
    if (!yTable) return
    yTable.set(field, value)
  }, [ydoc, table.id])

  const addColumn = useCallback(() => {
    if (!ydoc) return
    const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
    const yTable = tablesMap.get(table.id)
    if (!yTable) return
    const columns = yTable.get('columns') as Y.Array<Y.Map<unknown>>

    const yCol = new Y.Map()
    yCol.set('id', uuidv4())
    yCol.set('name', 'new_column')
    yCol.set('dataType', 'VARCHAR')
    yCol.set('nullable', true)
    yCol.set('isPrimaryKey', false)
    yCol.set('isUnique', false)
    yCol.set('isAutoIncrement', false)
    yCol.set('order', table.columns.length)
    columns.push([yCol])
  }, [ydoc, table.id, table.columns.length])

  const deleteColumn = useCallback((colId: string) => {
    if (!ydoc) return
    const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
    const yTable = tablesMap.get(table.id)
    if (!yTable) return
    const columns = yTable.get('columns') as Y.Array<Y.Map<unknown>>
    const idx = columns.toArray().findIndex((c) => c.get('id') === colId)
    if (idx >= 0) columns.delete(idx, 1)
  }, [ydoc, table.id])

  const updateColumn = useCallback((colId: string, field: string, value: unknown) => {
    if (!ydoc) return
    const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
    const yTable = tablesMap.get(table.id)
    if (!yTable) return
    const columns = yTable.get('columns') as Y.Array<Y.Map<unknown>>
    const yCol = columns.toArray().find((c) => c.get('id') === colId)
    if (yCol) yCol.set(field, value)
  }, [ydoc, table.id])

  return (
    <aside className="w-80 bg-white border-l border-gray-200 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="p-3 border-b border-gray-100 flex items-center justify-between">
        <span className="text-xs font-semibold text-gray-500 uppercase">테이블 편집</span>
        <button onClick={() => setSelection(null, null)} className="text-gray-400 hover:text-gray-700 text-lg leading-none">×</button>
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {/* Table Name */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">테이블 이름</label>
          {editingName ? (
            <input
              autoFocus
              className="w-full px-2 py-1.5 border border-primary-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
              defaultValue={table.name}
              onBlur={(e) => { updateTableField('name', e.target.value); setEditingName(false) }}
              onKeyDown={(e) => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur() }}
            />
          ) : (
            <div
              className="px-2 py-1.5 border border-transparent hover:border-gray-200 rounded cursor-text text-sm font-medium"
              onClick={() => setEditingName(true)}
            >
              {table.name}
            </div>
          )}
        </div>

        {/* Logical Name */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">논리명</label>
          <input
            className="w-full px-2 py-1.5 border border-gray-200 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
            placeholder="예: 사용자"
            defaultValue={table.logicalName ?? ''}
            onBlur={(e) => updateTableField('logicalName', e.target.value)}
          />
        </div>

        {/* Comment */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">주석 (COMMENT)</label>
          <input
            className="w-full px-2 py-1.5 border border-gray-200 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
            placeholder="테이블 주석..."
            defaultValue={table.comment ?? ''}
            onBlur={(e) => updateTableField('comment', e.target.value)}
          />
        </div>

        {/* Columns */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-semibold text-gray-500 uppercase">컬럼</label>
            <button
              onClick={addColumn}
              className="text-xs px-2 py-1 bg-primary-50 text-primary-700 rounded hover:bg-primary-100"
            >+ 추가</button>
          </div>
          <div className="space-y-1">
            {table.columns.map((col) => (
              <ColumnRow
                key={col.id}
                column={col}
                onUpdate={(field, value) => updateColumn(col.id, field, value)}
                onDelete={() => deleteColumn(col.id)}
              />
            ))}
          </div>
        </div>
      </div>
    </aside>
  )
}

function ColumnRow({
  column, onUpdate, onDelete,
}: { column: ColumnDef; onUpdate: (field: string, value: unknown) => void; onDelete: () => void }) {
  const [expanded, setExpanded] = useState(false)
  return (
    <div className="border border-gray-100 rounded p-2 text-xs space-y-1.5 hover:border-gray-200">
      <div className="flex gap-1">
        <input
          className="flex-1 px-1.5 py-1 border border-gray-200 rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary-300"
          defaultValue={column.name}
          placeholder="컬럼명"
          onBlur={(e) => onUpdate('name', e.target.value)}
        />
        <input
          className="w-24 px-1.5 py-1 border border-gray-200 rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary-300 font-mono"
          defaultValue={column.dataType}
          placeholder="타입"
          onBlur={(e) => onUpdate('dataType', e.target.value)}
        />
        <button
          onClick={() => setExpanded((v) => !v)}
          className="text-gray-400 hover:text-gray-600 px-1 text-xs"
          title="논리명/주석 편집"
        >{expanded ? '▲' : '▼'}</button>
        <button onClick={onDelete} className="text-red-400 hover:text-red-600 px-1">×</button>
      </div>
      <div className="flex gap-3 text-gray-500">
        {(['isPrimaryKey', 'isUnique', 'nullable', 'isAutoIncrement'] as const).map((field) => (
          <label key={field} className="flex items-center gap-1 cursor-pointer select-none">
            <input
              type="checkbox"
              defaultChecked={column[field] as boolean}
              onChange={(e) => onUpdate(field, e.target.checked)}
              className="w-3 h-3"
            />
            <span>{field === 'isPrimaryKey' ? 'PK' : field === 'isUnique' ? 'UQ' : field === 'nullable' ? 'NULL' : 'AI'}</span>
          </label>
        ))}
      </div>
      {expanded && (
        <div className="pt-1 space-y-1 border-t border-gray-100">
          <input
            className="w-full px-1.5 py-1 border border-gray-200 rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary-300"
            defaultValue={column.logicalName ?? ''}
            placeholder="논리명 (예: 사용자 ID)"
            onBlur={(e) => onUpdate('logicalName', e.target.value)}
          />
          <input
            className="w-full px-1.5 py-1 border border-gray-200 rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary-300"
            defaultValue={column.comment ?? ''}
            placeholder="주석 (COMMENT)"
            onBlur={(e) => onUpdate('comment', e.target.value)}
          />
        </div>
      )}
    </div>
  )
}
