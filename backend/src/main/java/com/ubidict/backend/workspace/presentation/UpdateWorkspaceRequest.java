package com.ubidict.backend.workspace.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Schema(description = "바꿀 워크스페이스 이름", example = "플랫폼팀") @NotBlank @Size(max = 50)
        String name) {}
