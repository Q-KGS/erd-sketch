package com.erdsketch.document;

import java.util.List;

public record DdlGenerateResponse(String ddl, List<String> warnings) {}
