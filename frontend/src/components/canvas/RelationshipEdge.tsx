import { memo } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import type { RelationshipDef } from '@/models'

interface RelationshipEdgeData extends RelationshipDef {
  isSelected?: boolean
}

const RelationshipEdge = memo(function RelationshipEdge({
  id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected,
}: EdgeProps) {
  const edgeData = data as unknown as RelationshipEdgeData
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition })

  return (
    <>
      <BaseEdge
        id={id}
        path={edgePath}
        style={{
          stroke: selected ? '#3b82f6' : '#94a3b8',
          strokeWidth: selected ? 2 : 1.5,
          strokeDasharray: edgeData?.type === 'NON_IDENTIFYING' ? '6 3' : undefined,
        }}
        markerEnd={`url(#arrow-${id})`}
      />
      {edgeData?.cardinality && (
        <EdgeLabelRenderer>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
              pointerEvents: 'all',
            }}
            className="px-1.5 py-0.5 bg-white border border-gray-200 rounded text-xs text-gray-600 font-mono shadow-sm"
          >
            {edgeData.cardinality}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
})

export default RelationshipEdge
