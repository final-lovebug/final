package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewRequest extends BaseEntity {

    public static final int TITLE_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ReviewRequestType type;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, updatable = false)
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewRequestStatus status;

    private OffsetDateTime approvedAt;

    private OffsetDateTime revisedAt;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private ReviewRequest(
            Long workspaceId,
            ReviewRequestType type,
            String title,
            String description,
            Long requesterId,
            Long createdBy) {
        this.workspaceId = workspaceId;
        this.type = requireType(type);
        this.title = normalizeTitle(title);
        this.description = description;
        this.requesterId = requesterId;
        this.status = ReviewRequestStatus.PENDING_REVIEW;
        this.createdBy = createdBy;
    }

    public static ReviewRequest create(
            Long workspaceId,
            ReviewRequestType type,
            String title,
            String description,
            Long requesterId,
            Long createdBy) {
        return new ReviewRequest(workspaceId, type, title, description, requesterId, createdBy);
    }

    public void changeContent(String title, String description) {
        if (title != null) {
            this.title = normalizeTitle(title);
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void cancel(Long actorId) {
        if (!requesterId.equals(actorId)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REQUESTER);
        }
        if (status == ReviewRequestStatus.REVISED || status == ReviewRequestStatus.CANCELED) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_STATUS_TRANSITION);
        }

        status = ReviewRequestStatus.CANCELED;
    }

    public void validateRevisionType(boolean documentRevision) {
        if ((type == ReviewRequestType.DOCUMENT) != documentRevision) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_TYPE_MISMATCHED);
        }
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_TITLE_REQUIRED);
        }

        String normalized = title.strip();
        if (normalized.isEmpty() || normalized.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_TITLE_REQUIRED);
        }

        return normalized;
    }

    private static ReviewRequestType requireType(ReviewRequestType type) {
        if (type == null) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_TYPE);
        }

        return type;
    }
}
