package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewerDuplicationValidator {
    private final ReviewerRepository repository;

    public void validate(Long requestId, Long memberId) {
        if (repository.existsByReviewRequestIdAndMemberId(requestId, memberId))
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_DUPLICATE_REVIEWER);
    }
}
