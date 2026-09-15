package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewerCount;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import java.util.*;
import java.util.stream.Collectors;
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

    /** 리뷰 요청 목록/상세 응답에 {@code reviewerCount}를 실어 보내기 위한 배치 집계(T-INT-12). */
    public Map<Long, Integer> countsByReviewRequestIds(Collection<Long> reviewRequestIds) {
        if (reviewRequestIds.isEmpty()) {
            return Map.of();
        }
        return repository.countByReviewRequestIdIn(reviewRequestIds).stream()
                .collect(Collectors.toMap(ReviewerCount::reviewRequestId, count -> (int) count.count()));
    }
}
