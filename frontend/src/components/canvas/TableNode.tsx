import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { TableDef } from '@/models'
import { useEditorStore } from '@/store/editorStore'

interface TableNodeData extends TableDef {
  isSelected?: boolean
}

const TableNode = memo(function TableNode({ data, selected }: NodeProps) {
  const tableData = data as TableNodeData
  const { setSelection } = useEditorStore()

  const handleClick = () => {
    setSelection(tableData.id, 'table')
  }

  const borderColor = tableData.color ?? '#3b82f6'

  return (
    <div
      onClick={handleClick}
      className={`bg-white rounded-lg shadow-md border-2 min-w-[560px] cursor-pointer transition-shadow ${
        selected ? 'shadow-lg ring-2 ring-primary-400' : 'hover:shadow-lg'
      }`}
      style={{ borderColor }}
    >
      {/* Table Header */}
      <div
        className="px-3 py-2 rounded-t-md"
        style={{ backgroundColor: borderColor + '20' }}
      >
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-gray-500 uppercase">TABLE</span>
          <span className="font-semibold text-gray-900 flex-1 truncate">{tableData.name}</span>
          {tableData.logicalName && (
            <span className="text-xs text-gray-500 truncate">({tableData.logicalName})</span>
          )}
        </div>
        {tableData.comment && (
          <p className="text-xs text-gray-400 mt-0.5 truncate">{tableData.comment}</p>
        )}
      </div>

      {/* Column Header Row */}
      <div className="px-3 py-1 bg-gray-50 border-b border-gray-200 grid text-xs text-gray-400 font-medium"
        style={{ gridTemplateColumns: '2fr 2fr 1.5fr 60px 2fr' }}>
        <span>논리명</span>
        <span>물리명</span>
        <span>유형</span>
        <span className="text-center">NULL</span>
        <span>코멘트</span>
      </div>

      {/* Columns */}
      <div className="divide-y divide-gray-100">
        {tableData.columns.map((col) => (
          <div key={col.id} className="relative group">
            <Handle
              type="source"
              position={Position.Right}
              id={`col-${col.id}-source`}
              className="!w-2 !h-2 !bg-gray-300 !border-0 opacity-0 group-hover:opacity-100 !right-0"
            />
            <Handle
              type="target"
              position={Position.Left}
              id={`col-${col.id}-target`}
              className="!w-2 !h-2 !bg-gray-300 !border-0 opacity-0 group-hover:opacity-100 !left-0"
            />
            <div
              className="px-3 py-1.5 grid text-xs items-center gap-x-2"
              style={{ gridTemplateColumns: '2fr 2fr 1.5fr 60px 2fr' }}
            >
              {/* 논리명 */}
              <span className="text-gray-500 truncate">{col.logicalName ?? ''}</span>

              {/* 물리명 + PK/UQ badge */}
              <div className="flex items-center gap-1 min-w-0">
                {col.isPrimaryKey && (
                  <span className="text-yellow-500 font-bold shrink-0">PK</span>
                )}
                {!col.isPrimaryKey && col.isUnique && (
                  <span className="text-blue-500 font-bold shrink-0">UQ</span>
                )}
                <span className={`truncate ${col.isPrimaryKey ? 'font-semibold text-gray-900' : 'text-gray-700'}`}>
                  {col.name}
                </span>
              </div>

              {/* 컬럼유형 */}
              <span className="text-gray-400 font-mono truncate">{col.dataType}</span>

              {/* Null여부 */}
              <span className={`text-center font-medium ${col.nullable ? 'text-gray-300' : 'text-red-400'}`}>
                {col.nullable ? 'NULL' : 'NN'}
              </span>

              {/* 코멘트 */}
              <span className="text-gray-400 truncate">{col.comment ?? ''}</span>
            </div>
          </div>
        ))}
        {tableData.columns.length === 0 && (
          <div className="px-3 py-2 text-xs text-gray-400 italic">컬럼 없음</div>
        )}
      </div>

      {/* Connection handles on sides */}
      <Handle
        type="source"
        position={Position.Right}
        id="table-source"
        className="!w-3 !h-3 !bg-primary-400 !border-2 !border-white"
      />
      <Handle
        type="target"
        position={Position.Left}
        id="table-target"
        className="!w-3 !h-3 !bg-primary-400 !border-2 !border-white"
      />
    </div>
  )
})

export default TableNode
