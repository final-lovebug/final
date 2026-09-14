package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewReader {

    private final ReviewRepository reviewRepository;

    public Review read(Long reviewId) {
        return reviewRepository
                .findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVIEW_NOT_FOUND));
    }

    public List<Review> readAll(Long reviewRequestId, Integer targetRound) {
        if (targetRound == null) {
            return reviewRepository.findByReviewRequestIdAndDeletedAtIsNullOrderBySubmittedAtAscIdAsc(reviewRequestId);
        }
        return reviewRepository.findByReviewRequestIdAndTargetRoundAndDeletedAtIsNullOrderBySubmittedAtAscIdAsc(
                reviewRequestId, targetRound);
    }

    public List<Review> readLatest(Long reviewRequestId) {
        return reviewRepository.findLatestByReviewRequestId(reviewRequestId);
    }
}
