package com.ubidict.backend.reviewrequest.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import java.time.OffsetDateTime;

/**
 * 리뷰 요청이 만들어졌다. 초안 도메인이 이것을 받아 초안을 리뷰 요청 상태로 표시한다.
 * 그래서 대상 초안 식별자를 담는다 — 수신 측이 리뷰 도메인을 되짚어 조회하지 않게 한다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 RR-4d가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record ReviewRequestCreatedEvent(
        Long reviewRequestId, Long workspaceId, ReviewRequestType type, Long targetDraftId, OffsetDateTime occurredAt)
        implements DomainEvent {}
