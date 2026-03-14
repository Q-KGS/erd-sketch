package com.erdsketch.dbml;

import jakarta.validation.constraints.NotBlank;

public record DbmlParseRequest(
        @NotBlank String dbml
) {}
