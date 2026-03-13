package com.erdsketch.document;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(@NotBlank String name) {}
