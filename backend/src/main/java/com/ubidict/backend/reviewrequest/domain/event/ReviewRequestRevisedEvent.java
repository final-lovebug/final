package com.ubidict.backend.reviewrequest.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 승인된 개정안이 반영돼 새 버전이 발행됐다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 RR-4d가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record ReviewRequestRevisedEvent(
        Long reviewRequestId, Long targetDraftId, int resultVersionNo, OffsetDateTime occurredAt)
        implements DomainEvent {}
