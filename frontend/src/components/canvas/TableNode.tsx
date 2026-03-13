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
      className={`bg-white rounded-lg shadow-md border-2 min-w-[240px] cursor-pointer transition-shadow ${
        selected ? 'shadow-lg ring-2 ring-primary-400' : 'hover:shadow-lg'
      }`}
      style={{ borderColor }}
    >
      {/* Table Header */}
      <div
        className="px-3 py-2 rounded-t-md flex items-center gap-2"
        style={{ backgroundColor: borderColor + '20' }}
      >
        <span className="text-xs font-bold text-gray-500 uppercase">TABLE</span>
        <span className="font-semibold text-gray-900 flex-1 truncate">{tableData.name}</span>
      </div>

      {/* Columns */}
      <div className="divide-y divide-gray-100">
        {tableData.columns.map((col) => (
          <div key={col.id} className="px-3 py-1.5 flex items-center gap-2 text-sm group relative">
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
            <div className="flex items-center gap-1 flex-1 min-w-0">
              {col.isPrimaryKey && <span className="text-yellow-500 text-xs font-bold">PK</span>}
              {!col.isPrimaryKey && col.isUnique && <span className="text-blue-500 text-xs font-bold">UQ</span>}
              {!col.isPrimaryKey && !col.isUnique && col.nullable && (
                <span className="text-gray-300 text-xs font-bold">  </span>
              )}
              <span className={`truncate ${col.isPrimaryKey ? 'font-semibold' : ''}`}>{col.name}</span>
            </div>
            <span className="text-xs text-gray-400 font-mono shrink-0">{col.dataType}</span>
            {!col.nullable && <span className="text-red-400 text-xs shrink-0">NN</span>}
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
