package com.erdsketch.document;

import com.erdsketch.project.DbType;

public record DdlParseRequest(String ddl, DbType dialect) {}
