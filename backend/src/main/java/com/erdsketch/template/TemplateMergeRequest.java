package com.erdsketch.template;

import jakarta.validation.constraints.NotBlank;

public record TemplateMergeRequest(
        @NotBlank String existingSchemaJson
) {}
