package com.erdsketch.document;

import com.erdsketch.project.DbType;
import java.util.List;

public record DdlGenerateRequest(DbType dialect, List<String> tableIds, boolean includeDrops) {}
