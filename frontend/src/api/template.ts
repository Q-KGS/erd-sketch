import { apiClient } from './client'

export interface TemplateInfo {
  type: string
  name: string
  description: string
}

export interface TemplateSchema {
  tables: Record<string, unknown>[]
  relationships: Record<string, unknown>[]
}

export const templateApi = {
  list: async (): Promise<TemplateInfo[]> => {
    const res = await apiClient.get('/templates')
    return res.data
  },

  apply: async (type: string): Promise<TemplateSchema> => {
    const res = await apiClient.post(`/templates/${type}/apply`)
    return res.data
  },

  merge: async (type: string, existingSchemaJson: string): Promise<TemplateSchema> => {
    const res = await apiClient.post(`/templates/${type}/merge`, { existingSchemaJson })
    return res.data
  },
}
