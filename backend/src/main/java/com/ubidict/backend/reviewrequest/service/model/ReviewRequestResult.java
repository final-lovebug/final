package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import java.time.OffsetDateTime;

public record ReviewRequestResult(
        Long reviewRequestId,
        Long workspaceId,
        ReviewRequestType type,
        String title,
        String description,
        Long requesterId,
        ReviewRequestStatus status,
        OffsetDateTime approvedAt,
        OffsetDateTime revisedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ReviewRequestResult from(ReviewRequest reviewRequest) {
        return new ReviewRequestResult(
                reviewRequest.getId(),
                reviewRequest.getWorkspaceId(),
                reviewRequest.getType(),
                reviewRequest.getTitle(),
                reviewRequest.getDescription(),
                reviewRequest.getRequesterId(),
                reviewRequest.getStatus(),
                reviewRequest.getApprovedAt(),
                reviewRequest.getRevisedAt(),
                reviewRequest.getCreatedAt(),
                reviewRequest.getUpdatedAt());
    }
}
