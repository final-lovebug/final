package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalAuthorityValidator {

    private final WorkspaceAccessValidator workspaceAccessValidator;

    public void validateRevise(ReviewRequest reviewRequest, Long actorId) {
        Permission permission = workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), actorId);
        if (!permission.isAtLeast(Permission.ADMIN)) {
            log.warn(
                    "[ApprovalAuthorityValidator.validateRevise] Review revision access denied. reviewRequestId={}, workspaceId={}, actorId={}",
                    reviewRequest.getId(),
                    reviewRequest.getWorkspaceId(),
                    actorId);
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_ACCESS_DENIED);
        }
    }

    public void validateReexamine(ReviewRequest reviewRequest, Long actorId) {
        validateRequesterOrAdmin(reviewRequest, actorId);
    }

    public void validateRequesterOrAdmin(ReviewRequest reviewRequest, Long actorId) {
        Permission permission = workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), actorId);
        if (!reviewRequest.getRequesterId().equals(actorId) && !permission.isAtLeast(Permission.ADMIN)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REQUESTER);
        }
    }
}
