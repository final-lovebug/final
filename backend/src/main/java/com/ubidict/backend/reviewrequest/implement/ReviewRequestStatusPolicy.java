package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.LatestReviewAggregator.ReviewAggregate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRequestStatusPolicy {

    private final ReviseEligibilityCalculator reviseEligibilityCalculator;

    public void validateReviewable(ReviewRequest reviewRequest) {
        if (!reviewRequest.isReviewable()) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REVIEWABLE_STATUS);
        }
    }

    public void validateTargetRound(int targetRound, int currentRound) {
        if (targetRound != currentRound) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_STALE_TARGET_ROUND);
        }
    }

    public void update(ReviewRequest reviewRequest, int requiredReviewerCount, ReviewAggregate aggregate) {
        if (reviewRequest.getStatus() == ReviewRequestStatus.PENDING_REVIEW) {
            reviewRequest.startReview();
        }

        if (aggregate.changesRequestedCount() > 0) {
            if (reviewRequest.getStatus() == ReviewRequestStatus.IN_REVIEW
                    || reviewRequest.getStatus() == ReviewRequestStatus.APPROVED) {
                reviewRequest.requestChanges();
            }
            return;
        }

        if (reviewRequest.getStatus() == ReviewRequestStatus.CHANGES_REQUESTED) {
            reviewRequest.resumeReview();
        }

        if (requiredReviewerCount > 0
                && reviseEligibilityCalculator.isEligible(requiredReviewerCount, aggregate)
                && reviewRequest.getStatus() == ReviewRequestStatus.IN_REVIEW) {
            reviewRequest.approve(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));
        }
    }
}
