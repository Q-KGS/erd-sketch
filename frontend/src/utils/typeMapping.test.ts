import { describe, test, expect } from 'vitest'
import { getTypesForDb } from './typeMapping'

describe('getTypesForDb', () => {
  // F-TYPE-01: PostgreSQL 타입 목록
  test('F-TYPE-01: PostgreSQL 타입에 BIGINT, UUID, JSONB 포함', () => {
    const types = getTypesForDb('POSTGRESQL')
    const values = types.map((t) => t.value)

    expect(values).toContain('BIGINT')
    expect(values).toContain('UUID')
    expect(values).toContain('JSONB')
  })

  // F-TYPE-02: MySQL 타입 목록
  test('F-TYPE-02: MySQL 타입에 BIGINT, DATETIME, JSON 포함 / JSONB 미포함', () => {
    const types = getTypesForDb('MYSQL')
    const values = types.map((t) => t.value)

    expect(values).toContain('BIGINT')
    expect(values).toContain('DATETIME')
    expect(values).toContain('JSON')
    expect(values).not.toContain('JSONB')
  })

  // F-TYPE-03: Oracle 타입 목록
  test('F-TYPE-03: Oracle 타입에 NUMBER, VARCHAR2 포함', () => {
    const types = getTypesForDb('ORACLE')
    const values = types.map((t) => t.value)

    expect(values).toContain('NUMBER')
    expect(values).toContain('VARCHAR2')
  })

  // F-TYPE-04: MSSQL 타입 목록
  test('F-TYPE-04: MSSQL 타입에 NVARCHAR, UNIQUEIDENTIFIER 포함', () => {
    const types = getTypesForDb('MSSQL')
    const values = types.map((t) => t.value)

    expect(values).toContain('NVARCHAR')
    expect(values).toContain('UNIQUEIDENTIFIER')
  })

  // 추가: 각 DB 타입별 결과가 배열
  test.each(['POSTGRESQL', 'MYSQL', 'ORACLE', 'MSSQL'] as const)(
    '%s 타입 목록은 배열이고 비어있지 않음',
    (dbType) => {
      const types = getTypesForDb(dbType)
      expect(Array.isArray(types)).toBe(true)
      expect(types.length).toBeGreaterThan(0)
    },
  )

  // 추가: 각 타입 항목이 value와 label 필드를 가짐
  test('PostgreSQL 타입 항목은 value와 label을 가짐', () => {
    const types = getTypesForDb('POSTGRESQL')
    types.forEach((t) => {
      expect(t).toHaveProperty('value')
      expect(t).toHaveProperty('label')
      expect(typeof t.value).toBe('string')
      expect(typeof t.label).toBe('string')
    })
  })
})
