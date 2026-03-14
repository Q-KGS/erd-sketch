import { apiClient } from './client'
import type { DbType } from '@/models'

export interface JdbcConnectionRequest {
  host: string
  port: number
  database: string
  username: string
  password: string
  dbType: DbType
}

export const jdbcApi = {
  testConnection: async (request: JdbcConnectionRequest): Promise<{ status: string }> => {
    const res = await apiClient.post('/jdbc/test', request)
    return res.data
  },

  extract: async (request: JdbcConnectionRequest): Promise<Record<string, unknown>[]> => {
    const res = await apiClient.post('/jdbc/extract', request)
    return res.data
  },
}
