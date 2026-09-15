package com.ubidict.backend.reviewrequest.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import java.time.OffsetDateTime;

/**
 * 리뷰 요청이 취소됐다.
 */
public record ReviewRequestCanceledEvent(
        Long reviewRequestId, ReviewRequestType type, Long targetDraftId, OffsetDateTime occurredAt)
        implements DomainEvent {}
