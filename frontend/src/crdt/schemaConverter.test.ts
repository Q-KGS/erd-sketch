import { describe, test, expect } from 'vitest'
import * as Y from 'yjs'
import { ydocToSchema, addTableToYdoc, addRelationshipToYdoc } from './schemaConverter'

// F-CONV-01: 빈 Yjs 문서 → 스키마 변환
describe('ydocToSchema', () => {
  test('F-CONV-01: 빈 Yjs 문서를 변환하면 빈 스키마 반환', () => {
    const ydoc = new Y.Doc()
    const schema = ydocToSchema(ydoc)

    expect(schema.tables).toEqual({})
    expect(schema.relationships).toEqual({})
    expect(schema.notes).toEqual({})
  })

  // F-CONV-02: 테이블 추가 후 변환
  test('F-CONV-02: 테이블 추가 후 변환하면 tables에 포함', () => {
    const ydoc = new Y.Doc()
    const id = addTableToYdoc(ydoc, {
      name: 'users',
      position: { x: 100, y: 200 },
      columns: [],
      indexes: [],
    })

    const schema = ydocToSchema(ydoc)

    expect(schema.tables[id]).toBeDefined()
    expect(schema.tables[id].name).toBe('users')
    expect(schema.tables[id].columns).toBeInstanceOf(Array)
    expect(schema.tables[id].position).toEqual({ x: 100, y: 200 })
  })

  // F-CONV-03: addTableToYdoc 호출 후 반환된 id로 조회 가능
  test('F-CONV-03: addTableToYdoc이 반환한 id로 Yjs 맵 조회 가능', () => {
    const ydoc = new Y.Doc()
    const id = addTableToYdoc(ydoc, {
      name: 'posts',
      position: { x: 0, y: 0 },
      columns: [],
      indexes: [],
    })

    const tablesMap = ydoc.getMap('tables')
    expect(tablesMap.get(id)).toBeDefined()
  })

  // F-CONV-04: 관계 추가 후 변환
  test('F-CONV-04: 관계 추가 후 변환하면 relationships에 포함', () => {
    const ydoc = new Y.Doc()
    const tableId1 = addTableToYdoc(ydoc, { name: 'users', position: { x: 0, y: 0 }, columns: [], indexes: [] })
    const tableId2 = addTableToYdoc(ydoc, { name: 'posts', position: { x: 300, y: 0 }, columns: [], indexes: [] })

    const relId = addRelationshipToYdoc(ydoc, {
      sourceTableId: tableId2,
      sourceColumnIds: [],
      targetTableId: tableId1,
      targetColumnIds: [],
      cardinality: '1:N',
      onDelete: 'RESTRICT',
      onUpdate: 'RESTRICT',
    })

    const schema = ydocToSchema(ydoc)

    expect(schema.relationships[relId]).toBeDefined()
    expect(schema.relationships[relId].sourceTableId).toBe(tableId2)
    expect(schema.relationships[relId].targetTableId).toBe(tableId1)
    expect(schema.relationships[relId].cardinality).toBe('1:N')
  })

  // F-CONV-05: PK 컬럼 포함 테이블 변환
  test('F-CONV-05: PK 컬럼이 있는 테이블을 변환하면 isPrimaryKey 유지', () => {
    const ydoc = new Y.Doc()
    const id = addTableToYdoc(ydoc, {
      name: 'users',
      position: { x: 0, y: 0 },
      columns: [
        {
          id: 'col-1',
          name: 'id',
          dataType: 'BIGINT',
          nullable: false,
          isPrimaryKey: true,
          isUnique: false,
          isAutoIncrement: true,
          order: 0,
        },
        {
          id: 'col-2',
          name: 'email',
          dataType: 'VARCHAR',
          nullable: false,
          isPrimaryKey: false,
          isUnique: true,
          isAutoIncrement: false,
          order: 1,
        },
      ],
      indexes: [],
    })

    const schema = ydocToSchema(ydoc)
    const table = schema.tables[id]

    expect(table.columns[0].isPrimaryKey).toBe(true)
    expect(table.columns[1].isPrimaryKey).toBe(false)
    expect(table.columns[1].isUnique).toBe(true)
  })
})
