import dagre from '@dagrejs/dagre'
import type { Node, Edge } from '@xyflow/react'

const NODE_WIDTH = 280
const NODE_HEIGHT_BASE = 120
const COLUMN_HEIGHT = 30

export function applyAutoLayout(
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR' = 'LR'
): Node[] {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({ rankdir: direction, ranksep: 80, nodesep: 60 })

  nodes.forEach((node) => {
    const columnCount = (node.data as { columns?: unknown[] })?.columns?.length ?? 0
    const height = NODE_HEIGHT_BASE + columnCount * COLUMN_HEIGHT
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height })
  })

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  dagre.layout(dagreGraph)

  return nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id)
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - NODE_WIDTH / 2,
        y: nodeWithPosition.y - NODE_HEIGHT_BASE / 2,
      },
    }
  })
}
