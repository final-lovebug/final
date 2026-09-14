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
        OffsetDateTime updatedAt,
        /** 대상 문서/사전집 id. type에 따라 documentId 또는 dictionaryId(D-63, T-INT-12). 아직
         * 개정안이 없거나(생성 직후) 사전집이 처음이라 dictionaryId가 없으면 null. */
        Long targetId,
        /** 지정된 리뷰어 수(T-INT-12). 정족수 판정 자체는 룰셋 기준이라 이 값과 다를 수 있다(G-4). */
        int reviewerCount) {

    /** 대상 id·리뷰어 수를 채우지 않는 경로(생성 직후 등)에서 쓴다. */
    public static ReviewRequestResult from(ReviewRequest reviewRequest) {
        return from(reviewRequest, null, 0);
    }

    public static ReviewRequestResult from(ReviewRequest reviewRequest, Long targetId, int reviewerCount) {
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
                reviewRequest.getUpdatedAt(),
                targetId,
                reviewerCount);
    }
}
