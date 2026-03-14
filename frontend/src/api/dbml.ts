import { apiClient } from './client'

export interface DbmlParseResult {
  tables: Record<string, unknown>[]
  relationships: Record<string, unknown>[]
  warnings: string[]
}

export const dbmlApi = {
  parse: async (dbml: string): Promise<DbmlParseResult> => {
    const res = await apiClient.post('/dbml/parse', { dbml })
    return res.data
  },

  generate: async (
    tables: Record<string, unknown>[],
    relationships: Record<string, unknown>[]
  ): Promise<string> => {
    const res = await apiClient.post('/dbml/generate', { tables, relationships })
    return res.data.dbml
  },
}
