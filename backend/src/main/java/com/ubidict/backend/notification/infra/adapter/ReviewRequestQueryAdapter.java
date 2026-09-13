package com.ubidict.backend.notification.infra.adapter;

import com.ubidict.backend.notification.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.notification.infra.port.ReviewRequestSnapshot;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 조회 어댑터는 소비 도메인에 두고 제공 도메인의 Repository만 참조한다(D-33).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.review-request.mode", havingValue = "real")
public class ReviewRequestQueryAdapter implements ReviewRequestQueryPort {

    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewerRepository reviewerRepository;

    @Override
    public Optional<ReviewRequestSnapshot> findSnapshot(Long reviewRequestId) {
        return reviewRequestRepository
                .findByIdAndDeletedAtIsNull(reviewRequestId)
                .map(reviewRequest -> new ReviewRequestSnapshot(
                        reviewRequest.getWorkspaceId(),
                        reviewRequest.getType(),
                        reviewRequest.getTitle(),
                        reviewRequest.getRequesterId()));
    }

    @Override
    public List<Long> reviewerMemberIds(Long reviewRequestId) {
        return reviewerRepository.findByReviewRequestId(reviewRequestId).stream()
                .filter(reviewer -> !reviewer.isDeleted())
                .map(Reviewer::getMemberId)
                .toList();
    }
}
