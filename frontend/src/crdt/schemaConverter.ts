import * as Y from 'yjs'
import { v4 as uuidv4 } from 'uuid'
import type { TableDef, ColumnDef, IndexDef, RelationshipDef, NoteDef, ErdSchema } from '@/models'

// Convert Yjs doc to plain schema object
export function ydocToSchema(ydoc: Y.Doc): ErdSchema {
  const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
  const relMap = ydoc.getMap('relationships') as Y.Map<Y.Map<unknown>>
  const notesMap = ydoc.getMap('notes') as Y.Map<Y.Map<unknown>>

  const tables: Record<string, TableDef> = {}
  tablesMap.forEach((yTable, id) => {
    tables[id] = ymapToTable(yTable)
  })

  const relationships: Record<string, RelationshipDef> = {}
  relMap.forEach((yRel, id) => {
    relationships[id] = ymapToRelationship(yRel)
  })

  const notes: Record<string, NoteDef> = {}
  notesMap.forEach((yNote, id) => {
    notes[id] = ymapToNote(yNote)
  })

  return { tables, relationships, notes }
}

function ymapToTable(yMap: Y.Map<unknown>): TableDef {
  const columnsArray = yMap.get('columns') as Y.Array<Y.Map<unknown>>
  const indexesArray = yMap.get('indexes') as Y.Array<Y.Map<unknown>>
  const posMap = yMap.get('position') as Y.Map<unknown>

  return {
    id: yMap.get('id') as string,
    name: yMap.get('name') as string,
    logicalName: yMap.get('logicalName') as string | undefined,
    schema: yMap.get('schema') as string | undefined,
    comment: yMap.get('comment') as string | undefined,
    color: yMap.get('color') as string | undefined,
    position: {
      x: posMap?.get('x') as number ?? 0,
      y: posMap?.get('y') as number ?? 0,
    },
    columns: columnsArray?.toArray().map(ymapToColumn) ?? [],
    indexes: indexesArray?.toArray().map(ymapToIndex) ?? [],
  }
}

function ymapToColumn(yMap: Y.Map<unknown>): ColumnDef {
  return {
    id: yMap.get('id') as string,
    name: yMap.get('name') as string,
    logicalName: yMap.get('logicalName') as string | undefined,
    dataType: yMap.get('dataType') as string,
    nullable: yMap.get('nullable') as boolean ?? true,
    defaultValue: yMap.get('defaultValue') as string | undefined,
    isPrimaryKey: yMap.get('isPrimaryKey') as boolean ?? false,
    isUnique: yMap.get('isUnique') as boolean ?? false,
    isAutoIncrement: yMap.get('isAutoIncrement') as boolean ?? false,
    comment: yMap.get('comment') as string | undefined,
    order: yMap.get('order') as number ?? 0,
  }
}

function ymapToIndex(yMap: Y.Map<unknown>): IndexDef {
  return {
    id: yMap.get('id') as string,
    name: yMap.get('name') as string,
    columns: (yMap.get('columns') as Y.Array<string>)?.toArray() ?? [],
    isUnique: yMap.get('isUnique') as boolean ?? false,
    type: yMap.get('type') as IndexDef['type'] ?? 'BTREE',
  }
}

function ymapToRelationship(yMap: Y.Map<unknown>): RelationshipDef {
  return {
    id: yMap.get('id') as string,
    name: yMap.get('name') as string | undefined,
    sourceTableId: yMap.get('sourceTableId') as string,
    sourceColumnIds: (yMap.get('sourceColumnIds') as Y.Array<string>)?.toArray() ?? [],
    targetTableId: yMap.get('targetTableId') as string,
    targetColumnIds: (yMap.get('targetColumnIds') as Y.Array<string>)?.toArray() ?? [],
    cardinality: yMap.get('cardinality') as RelationshipDef['cardinality'] ?? '1:N',
    type: yMap.get('type') as RelationshipDef['type'] ?? 'NON_IDENTIFYING',
    onDelete: yMap.get('onDelete') as RelationshipDef['onDelete'] ?? 'RESTRICT',
    onUpdate: yMap.get('onUpdate') as RelationshipDef['onUpdate'] ?? 'RESTRICT',
  }
}

function ymapToNote(yMap: Y.Map<unknown>): NoteDef {
  const posMap = yMap.get('position') as Y.Map<unknown>
  return {
    id: yMap.get('id') as string,
    content: yMap.get('content') as string ?? '',
    position: {
      x: posMap?.get('x') as number ?? 0,
      y: posMap?.get('y') as number ?? 0,
    },
    color: yMap.get('color') as string | undefined,
  }
}

// Write table to Yjs doc
export function addTableToYdoc(ydoc: Y.Doc, table: Omit<TableDef, 'id'>): string {
  const id = uuidv4()
  const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>

  const yTable = new Y.Map()
  yTable.set('id', id)
  yTable.set('name', table.name)
  if (table.logicalName) yTable.set('logicalName', table.logicalName)
  if (table.schema) yTable.set('schema', table.schema)
  if (table.comment) yTable.set('comment', table.comment)
  if (table.color) yTable.set('color', table.color)

  const posMap = new Y.Map()
  posMap.set('x', table.position.x)
  posMap.set('y', table.position.y)
  yTable.set('position', posMap)

  const columnsArray = new Y.Array()
  table.columns.forEach((col) => {
    const yCol = columnToYmap(col)
    columnsArray.push([yCol])
  })
  yTable.set('columns', columnsArray)

  const indexesArray = new Y.Array()
  yTable.set('indexes', indexesArray)

  tablesMap.set(id, yTable)
  return id
}

function columnToYmap(col: ColumnDef): Y.Map<unknown> {
  const yCol = new Y.Map()
  yCol.set('id', col.id || uuidv4())
  yCol.set('name', col.name)
  if (col.logicalName) yCol.set('logicalName', col.logicalName)
  yCol.set('dataType', col.dataType)
  yCol.set('nullable', col.nullable)
  if (col.defaultValue !== undefined) yCol.set('defaultValue', col.defaultValue)
  yCol.set('isPrimaryKey', col.isPrimaryKey)
  yCol.set('isUnique', col.isUnique)
  yCol.set('isAutoIncrement', col.isAutoIncrement)
  if (col.comment) yCol.set('comment', col.comment)
  yCol.set('order', col.order)
  return yCol
}

export function addRelationshipToYdoc(ydoc: Y.Doc, rel: Omit<RelationshipDef, 'id'>): string {
  const id = uuidv4()
  const relMap = ydoc.getMap('relationships') as Y.Map<Y.Map<unknown>>

  const yRel = new Y.Map()
  yRel.set('id', id)
  if (rel.name) yRel.set('name', rel.name)
  yRel.set('sourceTableId', rel.sourceTableId)
  const srcCols = new Y.Array<string>()
  srcCols.push(rel.sourceColumnIds)
  yRel.set('sourceColumnIds', srcCols)
  yRel.set('targetTableId', rel.targetTableId)
  const tgtCols = new Y.Array<string>()
  tgtCols.push(rel.targetColumnIds)
  yRel.set('targetColumnIds', tgtCols)
  yRel.set('cardinality', rel.cardinality)
  if (rel.type) yRel.set('type', rel.type)
  yRel.set('onDelete', rel.onDelete)
  yRel.set('onUpdate', rel.onUpdate)

  relMap.set(id, yRel)
  return id
}
