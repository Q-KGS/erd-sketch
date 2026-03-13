import { useEffect, useState, useCallback } from 'react'
import * as Y from 'yjs'
import { ydocToSchema, addTableToYdoc, addRelationshipToYdoc } from '@/crdt/schemaConverter'
import type { ErdSchema, TableDef, RelationshipDef } from '@/models'
import { v4 as uuidv4 } from 'uuid'

export function useSchema(ydoc: Y.Doc | null) {
  const [schema, setSchema] = useState<ErdSchema>({ tables: {}, relationships: {}, notes: {} })

  useEffect(() => {
    if (!ydoc) return

    const update = () => setSchema(ydocToSchema(ydoc))
    update()

    ydoc.on('update', update)
    return () => ydoc.off('update', update)
  }, [ydoc])

  const addTable = useCallback((position: { x: number; y: number }) => {
    if (!ydoc) return null
    return addTableToYdoc(ydoc, {
      name: 'new_table',
      position,
      columns: [
        {
          id: uuidv4(),
          name: 'id',
          dataType: 'BIGINT',
          nullable: false,
          isPrimaryKey: true,
          isUnique: true,
          isAutoIncrement: true,
          order: 0,
        },
      ],
      indexes: [],
    })
  }, [ydoc])

  const updateTable = useCallback((tableId: string, updates: Partial<Omit<TableDef, 'id' | 'columns' | 'indexes'>>) => {
    if (!ydoc) return
    const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
    const yTable = tablesMap.get(tableId)
    if (!yTable) return

    ydoc.transact(() => {
      if (updates.name !== undefined) yTable.set('name', updates.name)
      if (updates.comment !== undefined) yTable.set('comment', updates.comment)
      if (updates.color !== undefined) yTable.set('color', updates.color)
      if (updates.position !== undefined) {
        const posMap = yTable.get('position') as Y.Map<unknown>
        posMap.set('x', updates.position.x)
        posMap.set('y', updates.position.y)
      }
    })
  }, [ydoc])

  const deleteTable = useCallback((tableId: string) => {
    if (!ydoc) return
    const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
    tablesMap.delete(tableId)
  }, [ydoc])

  const addRelationship = useCallback((rel: Omit<RelationshipDef, 'id'>) => {
    if (!ydoc) return null
    return addRelationshipToYdoc(ydoc, rel)
  }, [ydoc])

  const updateRelationship = useCallback((relId: string, updates: Partial<Omit<RelationshipDef, 'id'>>) => {
    if (!ydoc) return
    const relMap = ydoc.getMap('relationships') as Y.Map<Y.Map<unknown>>
    const yRel = relMap.get(relId)
    if (!yRel) return

    ydoc.transact(() => {
      if (updates.name !== undefined) yRel.set('name', updates.name)
      if (updates.cardinality !== undefined) yRel.set('cardinality', updates.cardinality)
      if (updates.type !== undefined) yRel.set('type', updates.type)
      if (updates.onDelete !== undefined) yRel.set('onDelete', updates.onDelete)
      if (updates.onUpdate !== undefined) yRel.set('onUpdate', updates.onUpdate)
    })
  }, [ydoc])

  const deleteRelationship = useCallback((relId: string) => {
    if (!ydoc) return
    const relMap = ydoc.getMap('relationships') as Y.Map<Y.Map<unknown>>
    relMap.delete(relId)
  }, [ydoc])

  return {
    schema,
    addTable,
    updateTable,
    deleteTable,
    addRelationship,
    updateRelationship,
    deleteRelationship,
  }
}
