package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reviewRequestId;

    @Column(nullable = false, updatable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private int targetRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private ReviewVerdict verdict;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Review(Long reviewRequestId, Long memberId, int targetRound, ReviewVerdict verdict, Long createdBy) {
        if (reviewRequestId == null || memberId == null || createdBy == null || verdict == null) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        if (targetRound < 0) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_STALE_TARGET_ROUND);
        }

        this.reviewRequestId = reviewRequestId;
        this.memberId = memberId;
        this.targetRound = targetRound;
        this.verdict = verdict;
        this.submittedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.createdBy = createdBy;
    }

    public static Review submit(
            Long reviewRequestId, Long memberId, int targetRound, ReviewVerdict verdict, Long createdBy) {
        return new Review(reviewRequestId, memberId, targetRound, verdict, createdBy);
    }

    public boolean isApproval() {
        return verdict == ReviewVerdict.APPROVED;
    }
}
