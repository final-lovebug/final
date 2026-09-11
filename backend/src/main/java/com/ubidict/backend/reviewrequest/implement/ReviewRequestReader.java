package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRequestReader {

    private final ReviewRequestRepository reviewRequestRepository;

    public ReviewRequest read(Long reviewRequestId) {
        return reviewRequestRepository
                .findByIdAndDeletedAtIsNull(reviewRequestId)
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_FOUND));
    }

    public PageResult<ReviewRequest> search(ReviewRequestSearchQuery query) {
        Page<ReviewRequest> page = reviewRequestRepository.search(
                query.workspaceId(),
                query.type(),
                query.status(),
                query.requesterId(),
                query.reviewerMemberId(),
                PageRequest.of(query.page(), query.size(), Sort.by(query.sortDirection(), query.sortField())));
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
