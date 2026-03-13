package com.erdsketch.workspace;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteMemberRequest(@Email @NotBlank String email, WorkspaceRole role) {}
