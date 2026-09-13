package com.ubidict.backend.workspace.presentation.dto;

import com.ubidict.backend.workspace.service.model.CreateWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank @Size(max = 50) String name) {

    public CreateWorkspaceCommand toCommand(Long memberId) {
        return new CreateWorkspaceCommand(name, memberId);
    }
}
