// ===== User & Auth =====
export interface User {
  id: string
  email: string
  displayName: string
  avatarUrl?: string
  createdAt: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

// ===== Workspace =====
export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'

export interface Workspace {
  id: string
  name: string
  slug: string
  ownerId: string
  createdAt: string
  updatedAt: string
}

export interface WorkspaceMember {
  workspaceId: string
  userId: string
  user: User
  role: WorkspaceRole
  joinedAt: string
}

// ===== Project =====
export type DbType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'MSSQL'

export interface Project {
  id: string
  workspaceId: string
  name: string
  description?: string
  targetDbType: DbType
  createdBy: string
  createdAt: string
  updatedAt: string
}

// ===== ERD Schema =====
export interface ColumnDef {
  id: string
  name: string
  logicalName?: string
  dataType: string
  nullable: boolean
  defaultValue?: string
  isPrimaryKey: boolean
  isUnique: boolean
  isAutoIncrement: boolean
  comment?: string
  order: number
}

export type IndexType = 'BTREE' | 'HASH' | 'GIN' | 'GIST'

export interface IndexDef {
  id: string
  name: string
  columns: string[]
  isUnique: boolean
  type: IndexType
}

export interface TableDef {
  id: string
  name: string
  logicalName?: string
  schema?: string
  comment?: string
  color?: string
  position: { x: number; y: number }
  columns: ColumnDef[]
  indexes: IndexDef[]
}

export type Cardinality = '1:1' | '1:N' | 'M:N'
export type ReferentialAction = 'CASCADE' | 'SET_NULL' | 'RESTRICT' | 'NO_ACTION'
export type RelationshipType = 'IDENTIFYING' | 'NON_IDENTIFYING'

export interface RelationshipDef {
  id: string
  name?: string
  sourceTableId: string
  sourceColumnIds: string[]
  targetTableId: string
  targetColumnIds: string[]
  cardinality: Cardinality
  type?: RelationshipType
  onDelete: ReferentialAction
  onUpdate: ReferentialAction
}

export interface NoteDef {
  id: string
  content: string
  position: { x: number; y: number }
  color?: string
}

export interface ErdSchema {
  tables: Record<string, TableDef>
  relationships: Record<string, RelationshipDef>
  notes: Record<string, NoteDef>
}

// ===== Document =====
export interface ErdDocument {
  id: string
  projectId: string
  name: string
  canvasSettings?: CanvasSettings
  createdAt: string
  updatedAt: string
}

export interface CanvasSettings {
  viewport?: { x: number; y: number; zoom: number }
  showMinimap?: boolean
  showGrid?: boolean
  snapToGrid?: boolean
}

// ===== Document Version =====
export interface DocumentVersion {
  id: string
  documentId: string
  versionNumber: number
  label?: string
  createdBy: string
  createdAt: string
}

// ===== Comment =====
export type CommentTargetType = 'TABLE' | 'COLUMN' | 'RELATIONSHIP' | 'CANVAS'

export interface Comment {
  id: string
  documentId: string
  authorId: string
  author: User
  targetType: CommentTargetType
  targetId?: string
  content: string
  resolved: boolean
  parentId?: string
  replies?: Comment[]
  createdAt: string
  updatedAt: string
}

// ===== DDL =====
export interface DdlGenerateRequest {
  dialect: DbType
  tableIds?: string[]
  includeDrops: boolean
  schema?: ErdSchema
}

export interface DdlGenerateResponse {
  ddl: string
  warnings: string[]
}

export interface DdlParseRequest {
  ddl: string
  dialect: DbType
}

export interface DdlParseResponse {
  tables: TableDef[]
  relationships: RelationshipDef[]
  warnings: string[]
}

// ===== Collaboration =====
export interface UserPresence {
  clientId: number
  user: {
    id: string
    name: string
    color: string
    cursor?: { x: number; y: number }
    selectedTableId?: string
    selectedColumnId?: string
  }
}
