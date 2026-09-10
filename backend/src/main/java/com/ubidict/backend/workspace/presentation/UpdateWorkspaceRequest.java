package com.ubidict.backend.workspace.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank @Size(max = 50) String name) {}
