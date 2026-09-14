package com.ubidict.backend.reviewrequest.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import java.time.OffsetDateTime;

/**
 * 승인된 개정안이 반영돼 새 버전이 발행됐다.
 */
public record ReviewRequestRevisedEvent(
        Long reviewRequestId,
        ReviewRequestType type,
        Long targetDraftId,
        int resultVersionNo,
        OffsetDateTime occurredAt)
        implements DomainEvent {}
