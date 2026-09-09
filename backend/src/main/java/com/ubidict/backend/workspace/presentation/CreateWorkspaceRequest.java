package com.ubidict.backend.workspace.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @Schema(description = "워크스페이스 이름", example = "개발팀") @NotBlank @Size(max = 50)
        String name) {}
