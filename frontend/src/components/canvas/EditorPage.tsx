import { useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import {
  ReactFlow, ReactFlowProvider, Background, Controls, MiniMap,
  useNodesState, useEdgesState, useReactFlow,
  type Node, type Edge, type EdgeMouseHandler, type Connection, type OnConnect,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useQuery } from '@tanstack/react-query'
import { documentApi } from '@/api/document'
import { projectApi } from '@/api/project'
import { useEditorStore } from '@/store/editorStore'
import { useYjs } from '@/hooks/useYjs'
import { useSchema } from '@/hooks/useSchema'
import TableNode from './TableNode'
import RelationshipEdge from './RelationshipEdge'
import EditorToolbar from '../toolbar/EditorToolbar'
import TableEditorPanel from '../editor/TableEditorPanel'
import RelationshipEditorPanel from '../editor/RelationshipEditorPanel'
import DdlPreviewPanel from '../ddl/DdlPreviewPanel'
import CollaborationPresence from '../collaboration/CollaborationPresence'
import { applyAutoLayout } from '@/utils/autoLayout'
import type { TableDef, RelationshipDef } from '@/models'

const nodeTypes = { table: TableNode }
const edgeTypes = { relationship: RelationshipEdge }

export default function EditorPage() {
  return (
    <ReactFlowProvider>
      <EditorCanvas />
    </ReactFlowProvider>
  )
}

function EditorCanvas() {
  const { documentId, projectId } = useParams<{ documentId: string; projectId: string; workspaceId: string }>()
  const { setDocument, setSelection, selectedId, selectionType, isDdlPanelOpen } = useEditorStore()
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const { screenToFlowPosition } = useReactFlow()

  const { ydoc, awareness, isConnected } = useYjs(documentId ?? null)
  const { schema, addTable, addRelationship, updateRelationship, deleteTable, deleteRelationship, updateTable } = useSchema(ydoc)

  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId!),
    enabled: !!projectId,
  })

  useEffect(() => {
    if (documentId && projectId) {
      setDocument(documentId, projectId)
    }
  }, [documentId, projectId, setDocument])

  // Sync schema to React Flow nodes/edges
  useEffect(() => {
    const newNodes: Node[] = Object.values(schema.tables).map((table: TableDef) => ({
      id: table.id,
      type: 'table',
      position: table.position,
      data: table,
    }))

    const newEdges: Edge[] = Object.values(schema.relationships).map((rel: RelationshipDef) => ({
      id: rel.id,
      type: 'relationship',
      source: rel.sourceTableId,
      target: rel.targetTableId,
      sourceHandle: 'table-source',
      targetHandle: 'table-target',
      data: rel,
    }))

    setNodes(newNodes)
    setEdges(newEdges)
  }, [schema, setNodes, setEdges])

  const onConnect: OnConnect = useCallback(
    (connection: Connection) => {
      if (!connection.source || !connection.target) return
      addRelationship({
        sourceTableId: connection.source,
        sourceColumnIds: [],
        targetTableId: connection.target,
        targetColumnIds: [],
        cardinality: '1:N',
        onDelete: 'RESTRICT',
        onUpdate: 'RESTRICT',
      })
    },
    [addRelationship]
  )

  const handleAddTable = useCallback(() => {
    const centerX = window.innerWidth / 2
    const centerY = window.innerHeight / 2
    const position = screenToFlowPosition({ x: centerX, y: centerY })
    addTable(position)
  }, [addTable, screenToFlowPosition])

  const onNodesDelete = useCallback(
    (deletedNodes: Node[]) => {
      deletedNodes.forEach((n) => deleteTable(n.id))
    },
    [deleteTable]
  )

  const onEdgesDelete = useCallback(
    (deletedEdges: Edge[]) => {
      deletedEdges.forEach((e) => deleteRelationship(e.id))
    },
    [deleteRelationship]
  )

  const handleAutoLayout = useCallback(() => {
    setNodes((nds) => applyAutoLayout(nds, edges))
  }, [edges, setNodes])

  const handleNodeDragStop = useCallback(
    (_: React.MouseEvent, node: Node) => {
      updateTable(node.id, { position: node.position })
    },
    [updateTable]
  )

  const onEdgeClick: EdgeMouseHandler = useCallback(
    (_, edge) => { setSelection(edge.id, 'relationship') },
    [setSelection]
  )

  const selectedTable = selectionType === 'table' && selectedId ? schema.tables[selectedId] : null
  const selectedRelationship = selectionType === 'relationship' && selectedId ? schema.relationships[selectedId] : null

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      <EditorToolbar
        projectName={project?.name ?? ''}
        targetDbType={project?.targetDbType ?? 'POSTGRESQL'}
        isConnected={isConnected}
        onAutoLayout={handleAutoLayout}
        onAddTable={handleAddTable}
        awareness={awareness}
      />
      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onEdgeClick={onEdgeClick}
            onNodesDelete={onNodesDelete}
            onEdgesDelete={onEdgesDelete}
            onNodeDragStop={handleNodeDragStop}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            fitView
            deleteKeyCode="Delete"
            multiSelectionKeyCode="Shift"
          >
            <Background />
            <Controls />
            <MiniMap />
            {awareness && <CollaborationPresence awareness={awareness} />}
          </ReactFlow>
        </div>

        {selectedTable && (
          <TableEditorPanel key={selectedTable.id} table={selectedTable} ydoc={ydoc} />
        )}

        {selectedRelationship && (
          <RelationshipEditorPanel relationship={selectedRelationship} onUpdate={updateRelationship} />
        )}

        {isDdlPanelOpen && (
          <DdlPreviewPanel schema={schema} documentId={documentId!} />
        )}
      </div>
    </div>
  )
}
