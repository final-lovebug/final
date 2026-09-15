package com.ubidict.backend.workspace.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 참여자가 워크스페이스에서 빠졌다. 리뷰 도메인이 지정 리뷰어에서 제외한다(O-2).
 * participantId가 아니라 memberId를 담는다 — 수신 측이 Reviewer.memberId로 매칭한다.
 */
public record ParticipantRemovedEvent(Long workspaceId, Long memberId, OffsetDateTime occurredAt)
        implements DomainEvent {}
