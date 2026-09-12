package com.ubidict.backend.workspace.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 워크스페이스가 삭제됐다. 파생 데이터 정리는 각 도메인이 구독해 처리한다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 WS-5가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record WorkspaceDeletedEvent(Long workspaceId, OffsetDateTime occurredAt) implements DomainEvent {}
