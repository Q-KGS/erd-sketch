import dagre from '@dagrejs/dagre'
import type { Node, Edge } from '@xyflow/react'

// TableNode 실제 렌더 높이에 맞춰 계산
// 헤더: ~56px, 컬럼 헤더 행: ~28px, 각 컬럼: ~34px, 최소 여백: 20px
const NODE_WIDTH = 580
const HEADER_HEIGHT = 56
const COL_HEADER_HEIGHT = 28
const COLUMN_HEIGHT = 34
const MIN_NODE_HEIGHT = 120

function calcNodeHeight(node: Node): number {
  // React Flow가 측정한 실제 높이가 있으면 우선 사용
  if (node.measured?.height) return node.measured.height
  const columnCount = (node.data as { columns?: unknown[] })?.columns?.length ?? 0
  return Math.max(MIN_NODE_HEIGHT, HEADER_HEIGHT + COL_HEADER_HEIGHT + columnCount * COLUMN_HEIGHT)
}

export function applyAutoLayout(
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR' = 'LR'
): Node[] {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({ rankdir: direction, ranksep: 120, nodesep: 80 })

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height: calcNodeHeight(node) })
  })

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  dagre.layout(dagreGraph)

  return nodes.map((node) => {
    const pos = dagreGraph.node(node.id)
    const h = calcNodeHeight(node)
    return {
      ...node,
      position: {
        x: pos.x - NODE_WIDTH / 2,
        y: pos.y - h / 2,
      },
    }
  })
}
