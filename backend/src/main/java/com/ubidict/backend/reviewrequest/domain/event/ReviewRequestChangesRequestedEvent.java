package com.ubidict.backend.reviewrequest.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import java.time.OffsetDateTime;

/**
 * 재교정이 요청됐다. 초안이 다시 교정 상태로 돌아간다.
 */
public record ReviewRequestChangesRequestedEvent(
        Long reviewRequestId, ReviewRequestType type, Long targetDraftId, Long requesterId, OffsetDateTime occurredAt)
        implements DomainEvent {}
