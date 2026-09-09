package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Workspace;

public record WorkspaceIdResult(Long workspaceId) {

    public static WorkspaceIdResult from(Workspace workspace) {
        return new WorkspaceIdResult(workspace.getId());
    }
}
