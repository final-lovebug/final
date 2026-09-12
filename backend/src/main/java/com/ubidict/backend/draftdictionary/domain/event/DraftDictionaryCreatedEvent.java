package com.ubidict.backend.draftdictionary.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 사전 초안이 만들어졌다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 DI-4가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record DraftDictionaryCreatedEvent(Long draftDictionaryId, Long workspaceId, OffsetDateTime occurredAt)
        implements DomainEvent {}
