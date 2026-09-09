package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;

public record WorkspaceSummaryResult(Long workspaceId, String name, Permission myPermission) {

    public static WorkspaceSummaryResult of(Workspace workspace, Permission myPermission) {
        return new WorkspaceSummaryResult(workspace.getId(), workspace.getName(), myPermission);
    }
}
