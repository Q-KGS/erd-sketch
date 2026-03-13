import type { RelationshipDef } from '@/models'
import { useEditorStore } from '@/store/editorStore'

interface Props {
  relationship: RelationshipDef
  onUpdate: (relId: string, updates: Partial<Omit<RelationshipDef, 'id'>>) => void
}

export default function RelationshipEditorPanel({ relationship, onUpdate }: Props) {
  const { setSelection } = useEditorStore()
  const isIdentifying = relationship.type === 'IDENTIFYING'

  return (
    <aside className="w-72 bg-white border-l border-gray-200 flex flex-col overflow-hidden">
      <div className="p-3 border-b border-gray-100 flex items-center justify-between">
        <span className="text-xs font-semibold text-gray-500 uppercase">관계 편집</span>
        <button onClick={() => setSelection(null, null)} className="text-gray-400 hover:text-gray-700 text-lg leading-none">×</button>
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {/* Relationship Name */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">관계 이름 (선택)</label>
          <input
            className="w-full px-2 py-1.5 border border-gray-200 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
            placeholder="FK 관계명..."
            defaultValue={relationship.name ?? ''}
            onBlur={(e) => onUpdate(relationship.id, { name: e.target.value || undefined })}
          />
        </div>

        {/* Cardinality */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">카디널리티</label>
          <select
            className="w-full px-2 py-1.5 border border-gray-200 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
            value={relationship.cardinality}
            onChange={(e) => onUpdate(relationship.id, { cardinality: e.target.value as RelationshipDef['cardinality'] })}
          >
            <option value="1:1">1:1 — 일대일</option>
            <option value="1:N">1:N — 일대다</option>
            <option value="M:N">M:N — 다대다</option>
          </select>
        </div>

        {/* Relationship Type */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-2">관계 유형</label>
          <div className="flex gap-2">
            <button
              className={`flex-1 py-2 text-xs rounded border transition-colors ${isIdentifying ? 'bg-primary-50 border-primary-400 text-primary-700 font-medium' : 'border-gray-200 text-gray-600 hover:bg-gray-50'}`}
              onClick={() => onUpdate(relationship.id, { type: 'IDENTIFYING' })}
            >
              식별 (실선)
            </button>
            <button
              className={`flex-1 py-2 text-xs rounded border transition-colors ${!isIdentifying ? 'bg-primary-50 border-primary-400 text-primary-700 font-medium' : 'border-gray-200 text-gray-600 hover:bg-gray-50'}`}
              onClick={() => onUpdate(relationship.id, { type: 'NON_IDENTIFYING' })}
            >
              비식별 (점선)
            </button>
          </div>
          <p className="text-xs text-gray-400 mt-1.5">
            {isIdentifying ? '식별: 자식 PK가 부모 PK에 종속됨' : '비식별: 자식이 독립적인 PK를 보유'}
          </p>
        </div>

        <div className="border-t border-gray-100 pt-4 space-y-3">
          {/* On Delete */}
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">ON DELETE</label>
            <select
              className="w-full px-2 py-1.5 border border-gray-200 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
              value={relationship.onDelete}
              onChange={(e) => onUpdate(relationship.id, { onDelete: e.target.value as RelationshipDef['onDelete'] })}
            >
              <option value="RESTRICT">RESTRICT</option>
              <option value="CASCADE">CASCADE</option>
              <option value="SET_NULL">SET NULL</option>
              <option value="NO_ACTION">NO ACTION</option>
            </select>
          </div>

          {/* On Update */}
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">ON UPDATE</label>
            <select
              className="w-full px-2 py-1.5 border border-gray-200 rounded text-sm focus:outline-none focus:ring-1 focus:ring-primary-400"
              value={relationship.onUpdate}
              onChange={(e) => onUpdate(relationship.id, { onUpdate: e.target.value as RelationshipDef['onUpdate'] })}
            >
              <option value="RESTRICT">RESTRICT</option>
              <option value="CASCADE">CASCADE</option>
              <option value="SET_NULL">SET NULL</option>
              <option value="NO_ACTION">NO ACTION</option>
            </select>
          </div>
        </div>
      </div>
    </aside>
  )
}
