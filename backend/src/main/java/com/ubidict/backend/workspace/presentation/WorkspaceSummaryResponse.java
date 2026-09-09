package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.WorkspaceSummaryResult;

public record WorkspaceSummaryResponse(Long workspaceId, String name, Permission myPermission) {

    public static WorkspaceSummaryResponse from(WorkspaceSummaryResult result) {
        return new WorkspaceSummaryResponse(result.workspaceId(), result.name(), result.myPermission());
    }
}
