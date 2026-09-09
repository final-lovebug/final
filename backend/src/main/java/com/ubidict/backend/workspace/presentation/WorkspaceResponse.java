package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.WorkspaceResult;
import java.time.OffsetDateTime;

public record WorkspaceResponse(
        Long workspaceId,
        String name,
        int requiredDocumentReviewerCount,
        int requiredDictionaryReviewerCount,
        Permission myPermission,
        OffsetDateTime createdAt) {

    public static WorkspaceResponse from(WorkspaceResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.requiredDocumentReviewerCount(),
                result.requiredDictionaryReviewerCount(),
                result.myPermission(),
                result.createdAt());
    }
}
