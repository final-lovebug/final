package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.service.WorkspaceIdResult;

public record WorkspaceIdResponse(Long workspaceId) {

    public static WorkspaceIdResponse from(WorkspaceIdResult result) {
        return new WorkspaceIdResponse(result.workspaceId());
    }
}
