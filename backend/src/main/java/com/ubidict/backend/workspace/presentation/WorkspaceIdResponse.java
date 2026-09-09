package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.service.WorkspaceResult;

public record WorkspaceIdResponse(Long workspaceId) {

    public static WorkspaceIdResponse from(WorkspaceResult result) {
        return new WorkspaceIdResponse(result.workspaceId());
    }
}
