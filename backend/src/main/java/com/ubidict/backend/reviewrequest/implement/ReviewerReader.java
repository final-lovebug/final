package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewerReader {
    private final ReviewerRepository repository;

    public List<Reviewer> readAll(Long id) {
        return repository.findByReviewRequestId(id);
    }

    public Reviewer read(Long id) {
        return repository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVIEWER_NOT_FOUND));
    }
}
