package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.infra.port.WorkspacePolicyPort;
import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.workspace.mode", havingValue = "real")
public class WorkspacePolicyAdapter implements WorkspacePolicyPort {
    private final WorkspaceRepository workspaceRepository;

    @Override
    public int requiredReviewerCount(Long workspaceId, ReviewRequestType type) {
        Workspace workspace = workspaceRepository
                .findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        RuleSet ruleSet = workspace.getRuleSet();
        return type == ReviewRequestType.DOCUMENT
                ? ruleSet.requiredDocumentReviewerCount()
                : ruleSet.requiredDictionaryReviewerCount();
    }
}
