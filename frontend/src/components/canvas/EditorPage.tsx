import { useEffect, useCallback, useRef } from 'react'
import { useParams } from 'react-router-dom'
import {
  ReactFlow, ReactFlowProvider, Background, Controls, MiniMap,
  useNodesState, useEdgesState, useReactFlow,
  type Node, type Edge, type EdgeMouseHandler, type Connection, type OnConnect,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useQuery } from '@tanstack/react-query'
import { projectApi } from '@/api/project'
import { workspaceApi } from '@/api/workspace'
import { useEditorStore } from '@/store/editorStore'
import { useAuthStore } from '@/store/authStore'
import { downloadSchemaJson } from '@/utils/schemaExport'
import toast from 'react-hot-toast'
import { toPng } from 'html-to-image'
import { useYjs } from '@/hooks/useYjs'
import { useSchema } from '@/hooks/useSchema'
import TableNode from './TableNode'
import RelationshipEdge from './RelationshipEdge'
import EditorToolbar from '../toolbar/EditorToolbar'
import TableEditorPanel from '../editor/TableEditorPanel'
import RelationshipEditorPanel from '../editor/RelationshipEditorPanel'
import DdlPreviewPanel from '../ddl/DdlPreviewPanel'
import CollaborationPresence from '../collaboration/CollaborationPresence'
import VersionHistoryPanel from '../version/VersionHistoryPanel'
import CommentPanel from '../comment/CommentPanel'
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
  const { documentId, projectId, workspaceId } = useParams<{ documentId: string; projectId: string; workspaceId: string }>()
  const { setDocument, setSelection, selectedId, selectionType, isDdlPanelOpen, isCommentPanelOpen, isVersionPanelOpen, toggleCommentPanel, toggleVersionPanel } = useEditorStore()
  const { user } = useAuthStore()
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const canvasRef = useRef<HTMLDivElement>(null)
  const { screenToFlowPosition } = useReactFlow()

  const { ydoc, awareness, isConnected } = useYjs(documentId ?? null)
  const { schema, addTable, addRelationship, updateRelationship, deleteTable, deleteRelationship, updateTable } = useSchema(ydoc)

  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId!),
    enabled: !!projectId,
  })

  const { data: members = [] } = useQuery({
    queryKey: ['workspace-members', workspaceId],
    queryFn: () => workspaceApi.getMembers(workspaceId!),
    enabled: !!workspaceId,
  })

  const myRole = members.find((m) => m.userId === user?.id)?.role
  const isReadOnly = myRole === 'VIEWER'

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
      data: table as unknown as Record<string, unknown>,
    }))

    const newEdges: Edge[] = Object.values(schema.relationships).map((rel: RelationshipDef) => ({
      id: rel.id,
      type: 'relationship',
      source: rel.sourceTableId,
      target: rel.targetTableId,
      sourceHandle: 'table-source',
      targetHandle: 'table-target',
      data: rel as unknown as Record<string, unknown>,
    }))

    setNodes(newNodes)
    setEdges(newEdges)
  }, [schema, setNodes, setEdges])

  const onConnect: OnConnect = useCallback(
    (connection: Connection) => {
      if (isReadOnly || !connection.source || !connection.target) return
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
    [addRelationship, isReadOnly]
  )

  const handleAddTable = useCallback(() => {
    const centerX = window.innerWidth / 2
    const centerY = window.innerHeight / 2
    const position = screenToFlowPosition({ x: centerX, y: centerY })
    addTable(position)
  }, [addTable, screenToFlowPosition])

  const onNodesDelete = useCallback(
    (deletedNodes: Node[]) => {
      if (isReadOnly) return
      deletedNodes.forEach((n) => deleteTable(n.id))
    },
    [deleteTable, isReadOnly]
  )

  const onEdgesDelete = useCallback(
    (deletedEdges: Edge[]) => {
      if (isReadOnly) return
      deletedEdges.forEach((e) => deleteRelationship(e.id))
    },
    [deleteRelationship, isReadOnly]
  )

  const handleAutoLayout = useCallback(() => {
    const laid = applyAutoLayout(nodes, edges)
    setNodes(laid)
    // Yjs에 새 position 동기화
    laid.forEach((node) => {
      updateTable(node.id, { position: node.position })
    })
  }, [nodes, edges, setNodes, updateTable])

  const handleExportJson = useCallback(() => {
    const filename = `${project?.name ?? 'erdsketch'}_${new Date().toISOString().slice(0, 10)}.json`
    downloadSchemaJson(schema, filename)
    toast.success('JSON으로 내보냈습니다.')
  }, [schema, project?.name])

  const handleExportPng = useCallback(async () => {
    const element = canvasRef.current?.querySelector('.react-flow') as HTMLElement | null
    if (!element) return
    try {
      const dataUrl = await toPng(element, {
        pixelRatio: 2,
        cacheBust: true,
        filter: (node) => {
          if (node instanceof Element) {
            const cls = node.className
            if (typeof cls === 'string') {
              return !cls.includes('react-flow__controls') &&
                     !cls.includes('react-flow__minimap') &&
                     !cls.includes('react-flow__panel') &&
                     !cls.includes('export-ignore')
            }
          }
          return true
        },
      })
      const a = document.createElement('a')
      a.href = dataUrl
      a.download = `${project?.name ?? 'erdsketch'}_${new Date().toISOString().slice(0, 10)}.png`
      a.click()
      toast.success('PNG로 내보냈습니다.')
    } catch {
      toast.error('PNG 내보내기에 실패했습니다. 캔버스에 테이블이 있는지 확인해주세요.')
    }
  }, [project?.name])

  const handleExportPdf = useCallback(async () => {
    if (!documentId) return
    try {
      const token = useAuthStore.getState().tokens?.accessToken
      const res = await fetch(`/api/v1/documents/${documentId}/export/pdf`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error('PDF 생성 실패')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${project?.name ?? 'erdsketch'}_${new Date().toISOString().slice(0, 10)}.pdf`
      a.click()
      URL.revokeObjectURL(url)
      toast.success('PDF로 내보냈습니다.')
    } catch {
      toast.error('PDF 내보내기에 실패했습니다.')
    }
  }, [documentId, project?.name])

  const handleNodeDragStop = useCallback(
    (_: React.MouseEvent, node: Node) => {
      if (isReadOnly) return
      updateTable(node.id, { position: node.position })
    },
    [updateTable, isReadOnly]
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
        isReadOnly={isReadOnly}
        isVersionPanelOpen={isVersionPanelOpen}
        isCommentPanelOpen={isCommentPanelOpen}
        onAutoLayout={handleAutoLayout}
        onAddTable={handleAddTable}
        onExportJson={handleExportJson}
        onExportPng={handleExportPng}
        onExportPdf={handleExportPdf}
        onToggleVersion={toggleVersionPanel}
        onToggleComment={toggleCommentPanel}
        awareness={awareness}
        documentId={documentId ?? ''}
      />
      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 relative" ref={canvasRef}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={isReadOnly ? undefined : onConnect}
            onEdgeClick={onEdgeClick}
            onNodesDelete={isReadOnly ? undefined : onNodesDelete}
            onEdgesDelete={isReadOnly ? undefined : onEdgesDelete}
            onNodeDragStop={isReadOnly ? undefined : handleNodeDragStop}
            nodesDraggable={!isReadOnly}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            fitView
            deleteKeyCode={isReadOnly ? null : 'Delete'}
            multiSelectionKeyCode="Shift"
          >
            <Background />
            <Controls />
            <MiniMap />
            {awareness && <CollaborationPresence awareness={awareness} />}
          </ReactFlow>
        </div>

        {selectedTable && !isReadOnly && (
          <TableEditorPanel key={selectedTable.id} table={selectedTable} ydoc={ydoc} />
        )}

        {selectedRelationship && !isReadOnly && (
          <RelationshipEditorPanel relationship={selectedRelationship} onUpdate={updateRelationship} />
        )}

        {isDdlPanelOpen && (
          <DdlPreviewPanel schema={schema} documentId={documentId!} />
        )}

        {isVersionPanelOpen && documentId && (
          <div className="w-72 border-l border-gray-200 bg-white overflow-y-auto">
            <VersionHistoryPanel documentId={documentId} />
          </div>
        )}

        {isCommentPanelOpen && documentId && (
          <div className="w-72 border-l border-gray-200 bg-white overflow-y-auto">
            <CommentPanel documentId={documentId} />
          </div>
        )}
      </div>
    </div>
  )
}
