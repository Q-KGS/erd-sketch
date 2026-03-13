import type { ErdSchema, TableDef, ColumnDef, DbType } from '@/models'

export function generateDdlLocal(schema: ErdSchema, dialect: DbType): { ddl: string; warnings: string[] } {
  const tables = Object.values(schema.tables)
  const warnings: string[] = []

  if (tables.length === 0) return { ddl: '', warnings: [] }

  const parts = tables.map((table) => generateCreateTable(table, dialect, warnings))
  return { ddl: parts.join('\n\n'), warnings }
}

function q(name: string, dialect: DbType): string {
  switch (dialect) {
    case 'MYSQL': return `\`${name}\``
    case 'MSSQL': return `[${name}]`
    default: return `"${name}"`
  }
}

function mapType(col: ColumnDef, dialect: DbType): string {
  const dt = col.dataType.toUpperCase().trim()

  if (dialect === 'POSTGRESQL') {
    if ((dt === 'BIGINT' || dt === 'BIGSERIAL') && col.isAutoIncrement) return 'BIGSERIAL'
    if ((dt === 'INT' || dt === 'INTEGER' || dt === 'SERIAL') && col.isAutoIncrement) return 'SERIAL'
    if (dt === 'BOOLEAN' || dt === 'BOOL') return 'BOOLEAN'
    return col.dataType
  }

  if (dialect === 'MYSQL') {
    if (dt === 'BOOLEAN' || dt === 'BOOL') return 'TINYINT(1)'
    if (dt === 'BIGSERIAL') return 'BIGINT'
    if (dt === 'SERIAL') return 'INT'
    if (dt === 'TEXT') return 'TEXT'
    return col.dataType
  }

  if (dialect === 'ORACLE') {
    if (dt === 'BIGINT' || dt === 'BIGSERIAL' || dt === 'INT' || dt === 'INTEGER' || dt === 'SERIAL') return 'NUMBER'
    if (dt === 'BOOLEAN' || dt === 'BOOL') return 'NUMBER(1)'
    if (dt === 'VARCHAR' || dt === 'VARCHAR2') return 'VARCHAR2(255)'
    if (dt === 'TEXT') return 'CLOB'
    if (dt === 'TIMESTAMP') return 'TIMESTAMP'
    return col.dataType
  }

  if (dialect === 'MSSQL') {
    if (dt === 'BIGINT' || dt === 'BIGSERIAL') return 'BIGINT'
    if (dt === 'INT' || dt === 'INTEGER' || dt === 'SERIAL') return 'INT'
    if (dt === 'VARCHAR') return 'NVARCHAR(255)'
    if (dt === 'TEXT') return 'NVARCHAR(MAX)'
    if (dt === 'BOOLEAN' || dt === 'BOOL') return 'BIT'
    if (dt === 'TIMESTAMP') return 'DATETIME2'
    return col.dataType
  }

  return col.dataType
}

function generateColumnDef(col: ColumnDef, dialect: DbType): string {
  const parts: string[] = [q(col.name, dialect), mapType(col, dialect)]

  if (!col.nullable) parts.push('NOT NULL')

  if (col.isAutoIncrement) {
    if (dialect === 'MYSQL') parts.push('AUTO_INCREMENT')
    if (dialect === 'MSSQL') parts.push('IDENTITY(1,1)')
    // PostgreSQL uses SERIAL/BIGSERIAL type itself; Oracle uses GENERATED ALWAYS AS IDENTITY
    if (dialect === 'ORACLE') parts.push('GENERATED ALWAYS AS IDENTITY')
  }

  if (col.defaultValue !== undefined && col.defaultValue !== '') {
    parts.push(`DEFAULT ${col.defaultValue}`)
  }

  if (col.comment && dialect === 'MYSQL') {
    parts.push(`COMMENT '${col.comment.replace(/'/g, "\\'")}'`)
  }

  return '  ' + parts.join(' ')
}

function generateCreateTable(table: TableDef, dialect: DbType, warnings: string[]): string {
  if (table.columns.length === 0) {
    warnings.push(`테이블 "${table.name}"에 컬럼이 없습니다.`)
  }

  const tableName = q(table.name, dialect)
  const lines: string[] = []

  for (const col of table.columns) {
    lines.push(generateColumnDef(col, dialect))
  }

  const pkCols = table.columns.filter((c) => c.isPrimaryKey)
  if (pkCols.length > 0) {
    const pkNames = pkCols.map((c) => q(c.name, dialect)).join(', ')
    lines.push(`  PRIMARY KEY (${pkNames})`)
  }

  const uqCols = table.columns.filter((c) => c.isUnique && !c.isPrimaryKey)
  for (const col of uqCols) {
    lines.push(`  UNIQUE (${q(col.name, dialect)})`)
  }

  let ddl = `CREATE TABLE ${tableName} (\n${lines.join(',\n')}\n)`

  if (dialect === 'MYSQL') {
    ddl += ' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    if (table.comment) ddl += ` COMMENT='${table.comment.replace(/'/g, "''")}'`
  }

  ddl += ';'

  // PostgreSQL/Oracle COMMENT ON TABLE
  if (table.comment && (dialect === 'POSTGRESQL' || dialect === 'ORACLE')) {
    ddl += `\nCOMMENT ON TABLE ${tableName} IS '${table.comment.replace(/'/g, "''")}';`
  }

  // Column comments for PostgreSQL/Oracle
  if (dialect === 'POSTGRESQL' || dialect === 'ORACLE') {
    for (const col of table.columns) {
      if (col.comment) {
        ddl += `\nCOMMENT ON COLUMN ${tableName}.${q(col.name, dialect)} IS '${col.comment.replace(/'/g, "''")}';`
      }
    }
  }

  // MSSQL: table and column comments via sp_addextendedproperty
  if (dialect === 'MSSQL') {
    if (table.comment) {
      ddl += `\nEXEC sp_addextendedproperty N'MS_Description', N'${table.comment.replace(/'/g, "''")}', N'SCHEMA', N'dbo', N'TABLE', N'${table.name.replace(/'/g, "''")}';`
    }
    for (const col of table.columns) {
      if (col.comment) {
        ddl += `\nEXEC sp_addextendedproperty N'MS_Description', N'${col.comment.replace(/'/g, "''")}', N'SCHEMA', N'dbo', N'TABLE', N'${table.name.replace(/'/g, "''")}', N'COLUMN', N'${col.name.replace(/'/g, "''")}';`
      }
    }
  }

  return ddl
}
