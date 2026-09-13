package com.ubidict.backend.workspace.presentation.dto;

import com.ubidict.backend.workspace.service.model.RenameWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank @Size(max = 50) String name) {

    public RenameWorkspaceCommand toCommand(Long workspaceId, Long memberId) {
        return new RenameWorkspaceCommand(workspaceId, name, memberId);
    }
}
