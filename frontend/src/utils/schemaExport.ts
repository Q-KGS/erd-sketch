import type { ErdSchema } from '@/models'

const SCHEMA_VERSION = '1'

export interface SchemaExportData {
  version: string
  exportedAt: string
  schema: ErdSchema
}

export function exportSchemaToJson(schema: ErdSchema): string {
  const data: SchemaExportData = {
    version: SCHEMA_VERSION,
    exportedAt: new Date().toISOString(),
    schema,
  }
  return JSON.stringify(data, null, 2)
}

export function downloadSchemaJson(schema: ErdSchema, filename?: string): void {
  const json = exportSchemaToJson(schema)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename ?? `erdsketch_export_${formatDate(new Date())}.json`
  a.click()
  URL.revokeObjectURL(url)
}

export function importSchemaFromJson(jsonText: string): ErdSchema {
  let parsed: unknown
  try {
    parsed = JSON.parse(jsonText)
  } catch {
    throw new Error('유효하지 않은 JSON 형식입니다.')
  }

  // Support both wrapped format ({ version, schema }) and raw ErdSchema
  if (parsed && typeof parsed === 'object' && 'schema' in parsed) {
    const data = parsed as SchemaExportData
    return validateSchema(data.schema)
  }

  // Assume raw ErdSchema
  return validateSchema(parsed as ErdSchema)
}

function validateSchema(schema: unknown): ErdSchema {
  if (!schema || typeof schema !== 'object') {
    throw new Error('유효하지 않은 스키마 형식입니다.')
  }
  const s = schema as Record<string, unknown>
  if (typeof s.tables !== 'object' || typeof s.relationships !== 'object') {
    throw new Error('스키마에 tables 또는 relationships 필드가 없습니다.')
  }
  return {
    tables: (s.tables ?? {}) as ErdSchema['tables'],
    relationships: (s.relationships ?? {}) as ErdSchema['relationships'],
    notes: (s.notes ?? {}) as ErdSchema['notes'],
  }
}

function formatDate(date: Date): string {
  return date.toISOString().slice(0, 10).replace(/-/g, '')
}
