import { describe, test, expect, vi, beforeEach } from 'vitest'
import { exportSchemaToJson, importSchemaFromJson, downloadSchemaJson } from './schemaExport'
import type { ErdSchema } from '@/models'

const mockSchema: ErdSchema = {
  tables: {
    'tbl-1': {
      id: 'tbl-1',
      name: 'users',
      position: { x: 100, y: 200 },
      columns: [
        {
          id: 'col-1', name: 'id', dataType: 'BIGINT', nullable: false,
          isPrimaryKey: true, isUnique: false, isAutoIncrement: true, order: 0,
        },
      ],
      indexes: [],
    },
  },
  relationships: {
    'rel-1': {
      id: 'rel-1',
      sourceTableId: 'tbl-1',
      sourceColumnIds: ['col-1'],
      targetTableId: 'tbl-2',
      targetColumnIds: ['col-2'],
      cardinality: '1:N',
      onDelete: 'RESTRICT',
      onUpdate: 'RESTRICT',
    },
  },
  notes: {},
}

describe('schemaExport', () => {
  beforeEach(() => {
    // mock URL APIs for download tests
    global.URL.createObjectURL = vi.fn(() => 'blob:mock')
    global.URL.revokeObjectURL = vi.fn()
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    vi.spyOn(document, 'createElement').mockReturnValue(mockAnchor as unknown as HTMLElement)
  })

  // F-JSON-01: 스키마 JSON 직렬화 → { tables, relationships, notes } 구조
  test('F-JSON-01: 스키마가 올바른 JSON 구조로 직렬화됨', () => {
    const json = exportSchemaToJson(mockSchema)
    const parsed = JSON.parse(json)

    expect(parsed).toHaveProperty('schema')
    expect(parsed.schema).toHaveProperty('tables')
    expect(parsed.schema).toHaveProperty('relationships')
    expect(parsed.schema).toHaveProperty('notes')
    expect(parsed.schema.tables['tbl-1'].name).toBe('users')
    expect(parsed.schema.relationships['rel-1'].cardinality).toBe('1:N')
  })

  // F-JSON-02: JSON 역직렬화 후 스키마 복원
  test('F-JSON-02: JSON 역직렬화 후 원본 스키마 복원됨', () => {
    const json = exportSchemaToJson(mockSchema)
    const restored = importSchemaFromJson(json)

    expect(restored.tables['tbl-1'].name).toBe('users')
    expect(restored.relationships['rel-1'].cardinality).toBe('1:N')
    expect(restored.notes).toEqual({})
  })

  // F-JSON-03: 손상된 JSON 가져오기 → 에러 throw
  test('F-JSON-03: 손상된 JSON은 에러를 throw함', () => {
    expect(() => importSchemaFromJson('{ invalid json')).toThrow()
    expect(() => importSchemaFromJson('{"noTablesField": true}')).toThrow()
    expect(() => importSchemaFromJson('"just a string"')).toThrow()
  })
})
