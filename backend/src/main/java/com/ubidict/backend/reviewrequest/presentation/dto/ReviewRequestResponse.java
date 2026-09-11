package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import java.time.OffsetDateTime;

public record ReviewRequestResponse(
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

    public static ReviewRequestResponse from(ReviewRequestResult result) {
        return new ReviewRequestResponse(
                result.reviewRequestId(),
                result.workspaceId(),
                result.type(),
                result.title(),
                result.description(),
                result.requesterId(),
                result.status(),
                result.approvedAt(),
                result.revisedAt(),
                result.createdAt(),
                result.updatedAt());
    }
}
