package com.ubidict.backend.reviewrequest.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import java.time.OffsetDateTime;

/**
 * 리뷰어가 판정을 제출했다.
 */
public record ReviewSubmittedEvent(
        Long reviewRequestId,
        Long reviewId,
        Long memberId,
        ReviewVerdict verdict,
        int targetRound,
        OffsetDateTime occurredAt)
        implements DomainEvent {}
