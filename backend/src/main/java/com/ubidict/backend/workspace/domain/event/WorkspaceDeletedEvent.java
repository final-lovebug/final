package com.ubidict.backend.workspace.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 워크스페이스가 삭제됐다. 알림과 감사 처리가 필요하면 이 이벤트를 구독한다.
 */
public record WorkspaceDeletedEvent(Long workspaceId, OffsetDateTime occurredAt) implements DomainEvent {}
