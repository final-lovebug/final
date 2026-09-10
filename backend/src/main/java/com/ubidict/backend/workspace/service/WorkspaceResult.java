package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import java.time.OffsetDateTime;

/**
 * 도메인 모델이 JPA 엔티티를 겸하므로 result에 담지 않고 필요한 값만 옮긴다(API.md 공통 규칙).
 */
public record WorkspaceResult(
        Long workspaceId,
        String name,
        int requiredDocumentReviewerCount,
        int requiredDictionaryReviewerCount,
        Permission myPermission,
        OffsetDateTime createdAt) {

    public static WorkspaceResult of(Workspace workspace, Permission myPermission) {
        RuleSet ruleSet = workspace.getRuleSet();

        return new WorkspaceResult(
                workspace.getId(),
                workspace.getName(),
                ruleSet.requiredDocumentReviewerCount(),
                ruleSet.requiredDictionaryReviewerCount(),
                myPermission,
                workspace.getCreatedAt());
    }
}
