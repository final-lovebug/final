package com.ubidict.backend.dictionary.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 새 사전집 버전이 발행됐다. 용어 목록은 담지 않는다 — 수백 건이 될 수 있고 Term은 엔티티다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 DIC-8가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record DictionaryRevisedEvent(Long workspaceId, Long dictionaryId, int versionNo, OffsetDateTime occurredAt)
        implements DomainEvent {}
