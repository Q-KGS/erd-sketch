import { describe, test, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import * as Y from 'yjs'
import { useSchema } from './useSchema'

function makeYdoc() {
  return new Y.Doc()
}

describe('useSchema', () => {
  // F-SCHEMA-01: ydoc 업데이트 시 schema 재계산
  test('F-SCHEMA-01: ydoc 업데이트 시 schema가 재계산됨', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    act(() => {
      const tablesMap = ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
      const yTable = new Y.Map()
      yTable.set('id', 'tbl-1')
      yTable.set('name', 'new_table')
      const posMap = new Y.Map()
      posMap.set('x', 0)
      posMap.set('y', 0)
      yTable.set('position', posMap)
      yTable.set('columns', new Y.Array())
      yTable.set('indexes', new Y.Array())
      tablesMap.set('tbl-1', yTable)
    })

    expect(result.current.schema.tables['tbl-1']).toBeDefined()
    expect(result.current.schema.tables['tbl-1'].name).toBe('new_table')
  })

  // F-SCHEMA-02: addTable 후 schema에 새 테이블 포함
  test('F-SCHEMA-02: addTable 후 schema에 새 테이블 포함', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    let tableId: string | null = null
    act(() => {
      tableId = result.current.addTable({ x: 100, y: 200 })
    })

    expect(tableId).not.toBeNull()
    expect(result.current.schema.tables[tableId!]).toBeDefined()
    expect(result.current.schema.tables[tableId!].name).toBe('new_table')
  })

  // F-SCHEMA-03: updateTable 후 schema에 변경된 name 반영
  test('F-SCHEMA-03: updateTable 후 name 변경 반영', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    let tableId: string | null = null
    act(() => {
      tableId = result.current.addTable({ x: 0, y: 0 })
    })
    act(() => {
      result.current.updateTable(tableId!, { name: 'users' })
    })

    expect(result.current.schema.tables[tableId!].name).toBe('users')
  })

  // F-SCHEMA-04: deleteTable 후 schema에서 해당 테이블 없음
  test('F-SCHEMA-04: deleteTable 후 테이블 제거됨', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    let tableId: string | null = null
    act(() => {
      tableId = result.current.addTable({ x: 0, y: 0 })
    })
    act(() => {
      result.current.deleteTable(tableId!)
    })

    expect(result.current.schema.tables[tableId!]).toBeUndefined()
  })

  // F-SCHEMA-05: addRelationship 후 schema에 새 관계 포함
  test('F-SCHEMA-05: addRelationship 후 schema에 관계 포함', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    let tableId1: string | null = null
    let tableId2: string | null = null
    let relId: string | null = null

    act(() => {
      tableId1 = result.current.addTable({ x: 0, y: 0 })
      tableId2 = result.current.addTable({ x: 300, y: 0 })
    })
    act(() => {
      relId = result.current.addRelationship({
        sourceTableId: tableId1!,
        sourceColumnIds: [],
        targetTableId: tableId2!,
        targetColumnIds: [],
        cardinality: '1:N',
        onDelete: 'RESTRICT',
        onUpdate: 'RESTRICT',
      })
    })

    expect(relId).not.toBeNull()
    expect(result.current.schema.relationships[relId!]).toBeDefined()
    expect(result.current.schema.relationships[relId!].cardinality).toBe('1:N')
  })

  // F-SCHEMA-06: deleteRelationship 후 schema에서 해당 관계 없음
  test('F-SCHEMA-06: deleteRelationship 후 관계 제거됨', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    let tableId1: string | null = null
    let tableId2: string | null = null
    let relId: string | null = null

    act(() => {
      tableId1 = result.current.addTable({ x: 0, y: 0 })
      tableId2 = result.current.addTable({ x: 300, y: 0 })
      relId = result.current.addRelationship({
        sourceTableId: tableId1!,
        sourceColumnIds: [],
        targetTableId: tableId2!,
        targetColumnIds: [],
        cardinality: 'M:N',
        onDelete: 'CASCADE',
        onUpdate: 'RESTRICT',
      })
    })
    act(() => {
      result.current.deleteRelationship(relId!)
    })

    expect(result.current.schema.relationships[relId!]).toBeUndefined()
  })

  // 추가: ydoc이 null이면 빈 schema 반환
  test('ydoc이 null이면 초기 빈 schema 반환', () => {
    const { result } = renderHook(() => useSchema(null))

    expect(result.current.schema.tables).toEqual({})
    expect(result.current.schema.relationships).toEqual({})
  })

  // 추가: updateRelationship
  test('updateRelationship 후 cardinality 변경 반영', () => {
    const ydoc = makeYdoc()
    const { result } = renderHook(() => useSchema(ydoc))

    let tableId1: string | null = null
    let tableId2: string | null = null
    let relId: string | null = null

    act(() => {
      tableId1 = result.current.addTable({ x: 0, y: 0 })
      tableId2 = result.current.addTable({ x: 300, y: 0 })
      relId = result.current.addRelationship({
        sourceTableId: tableId1!,
        sourceColumnIds: [],
        targetTableId: tableId2!,
        targetColumnIds: [],
        cardinality: '1:N',
        onDelete: 'RESTRICT',
        onUpdate: 'RESTRICT',
      })
    })
    act(() => {
      result.current.updateRelationship(relId!, { cardinality: '1:1' })
    })

    expect(result.current.schema.relationships[relId!].cardinality).toBe('1:1')
  })
})
