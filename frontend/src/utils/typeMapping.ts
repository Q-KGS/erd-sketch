import type { DbType } from '@/models'

export interface TypeOption {
  value: string
  label: string
}

const POSTGRESQL_TYPES: TypeOption[] = [
  { value: 'BIGINT', label: 'BIGINT' },
  { value: 'INTEGER', label: 'INTEGER' },
  { value: 'SMALLINT', label: 'SMALLINT' },
  { value: 'SERIAL', label: 'SERIAL' },
  { value: 'BIGSERIAL', label: 'BIGSERIAL' },
  { value: 'NUMERIC', label: 'NUMERIC' },
  { value: 'DECIMAL', label: 'DECIMAL' },
  { value: 'REAL', label: 'REAL' },
  { value: 'DOUBLE PRECISION', label: 'DOUBLE PRECISION' },
  { value: 'VARCHAR', label: 'VARCHAR' },
  { value: 'CHAR', label: 'CHAR' },
  { value: 'TEXT', label: 'TEXT' },
  { value: 'BOOLEAN', label: 'BOOLEAN' },
  { value: 'DATE', label: 'DATE' },
  { value: 'TIMESTAMP', label: 'TIMESTAMP' },
  { value: 'TIMESTAMPTZ', label: 'TIMESTAMPTZ' },
  { value: 'UUID', label: 'UUID' },
  { value: 'JSONB', label: 'JSONB' },
  { value: 'JSON', label: 'JSON' },
  { value: 'BYTEA', label: 'BYTEA' },
]

const MYSQL_TYPES: TypeOption[] = [
  { value: 'BIGINT', label: 'BIGINT' },
  { value: 'INT', label: 'INT' },
  { value: 'SMALLINT', label: 'SMALLINT' },
  { value: 'TINYINT', label: 'TINYINT' },
  { value: 'DECIMAL', label: 'DECIMAL' },
  { value: 'FLOAT', label: 'FLOAT' },
  { value: 'DOUBLE', label: 'DOUBLE' },
  { value: 'VARCHAR', label: 'VARCHAR' },
  { value: 'CHAR', label: 'CHAR' },
  { value: 'TEXT', label: 'TEXT' },
  { value: 'LONGTEXT', label: 'LONGTEXT' },
  { value: 'BOOLEAN', label: 'BOOLEAN' },
  { value: 'DATE', label: 'DATE' },
  { value: 'DATETIME', label: 'DATETIME' },
  { value: 'TIMESTAMP', label: 'TIMESTAMP' },
  { value: 'JSON', label: 'JSON' },
  { value: 'BLOB', label: 'BLOB' },
]

const ORACLE_TYPES: TypeOption[] = [
  { value: 'NUMBER', label: 'NUMBER' },
  { value: 'INTEGER', label: 'INTEGER' },
  { value: 'FLOAT', label: 'FLOAT' },
  { value: 'VARCHAR2', label: 'VARCHAR2' },
  { value: 'CHAR', label: 'CHAR' },
  { value: 'CLOB', label: 'CLOB' },
  { value: 'NVARCHAR2', label: 'NVARCHAR2' },
  { value: 'DATE', label: 'DATE' },
  { value: 'TIMESTAMP', label: 'TIMESTAMP' },
  { value: 'BLOB', label: 'BLOB' },
  { value: 'RAW', label: 'RAW' },
]

const MSSQL_TYPES: TypeOption[] = [
  { value: 'BIGINT', label: 'BIGINT' },
  { value: 'INT', label: 'INT' },
  { value: 'SMALLINT', label: 'SMALLINT' },
  { value: 'TINYINT', label: 'TINYINT' },
  { value: 'DECIMAL', label: 'DECIMAL' },
  { value: 'FLOAT', label: 'FLOAT' },
  { value: 'REAL', label: 'REAL' },
  { value: 'NVARCHAR', label: 'NVARCHAR' },
  { value: 'VARCHAR', label: 'VARCHAR' },
  { value: 'CHAR', label: 'CHAR' },
  { value: 'NTEXT', label: 'NTEXT' },
  { value: 'TEXT', label: 'TEXT' },
  { value: 'BIT', label: 'BIT' },
  { value: 'DATE', label: 'DATE' },
  { value: 'DATETIME', label: 'DATETIME' },
  { value: 'DATETIME2', label: 'DATETIME2' },
  { value: 'UNIQUEIDENTIFIER', label: 'UNIQUEIDENTIFIER' },
  { value: 'VARBINARY', label: 'VARBINARY' },
]

export function getTypesForDb(dbType: DbType): TypeOption[] {
  switch (dbType) {
    case 'POSTGRESQL': return POSTGRESQL_TYPES
    case 'MYSQL': return MYSQL_TYPES
    case 'ORACLE': return ORACLE_TYPES
    case 'MSSQL': return MSSQL_TYPES
  }
}
