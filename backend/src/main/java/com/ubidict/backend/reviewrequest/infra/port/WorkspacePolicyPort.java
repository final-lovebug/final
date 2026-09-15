package com.ubidict.backend.reviewrequest.infra.port;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;

public interface WorkspacePolicyPort {
    int requiredReviewerCount(Long workspaceId, ReviewRequestType type);
}
