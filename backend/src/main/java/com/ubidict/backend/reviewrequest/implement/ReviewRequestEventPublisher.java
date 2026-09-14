package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRequestEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishCreated(ReviewRequest request, Long targetDraftId) {
        eventPublisher.publish(new ReviewRequestCreatedEvent(
                request.getId(),
                request.getWorkspaceId(),
                request.getType(),
                targetDraftId,
                request.getRequesterId(),
                now()));
    }

    public void publishSubmitted(Review review) {
        eventPublisher.publish(new ReviewSubmittedEvent(
                review.getReviewRequestId(),
                review.getId(),
                review.getMemberId(),
                review.getVerdict(),
                review.getTargetRound(),
                review.getSubmittedAt()));
    }

    public void publishChangesRequested(ReviewRequest request, Long targetDraftId) {
        eventPublisher.publish(new ReviewRequestChangesRequestedEvent(
                request.getId(), request.getType(), targetDraftId, request.getRequesterId(), now()));
    }

    public void publishRevised(ReviewRequest request, Long targetDraftId, int resultVersionNo) {
        eventPublisher.publish(new ReviewRequestRevisedEvent(
                request.getId(), request.getType(), targetDraftId, resultVersionNo, request.getRevisedAt()));
    }

    public void publishCanceled(ReviewRequest request, Long targetDraftId) {
        eventPublisher.publish(
                new ReviewRequestCanceledEvent(request.getId(), request.getType(), targetDraftId, now()));
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
