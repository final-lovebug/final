package com.ubidict.backend.dictionary.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 새 사전집 버전이 발행됐다. 용어 목록은 담지 않는다 — 수백 건이 될 수 있고 Term은 엔티티다.
 *
 * <p>발행부는 {@code DictionaryService}가 담당하며, 이벤트에는 상태가 필요한 컨슈머가 다시 조회할 수 있는 식별자만 담는다.
 */
public record DictionaryRevisedEvent(Long workspaceId, Long dictionaryId, int versionNo, OffsetDateTime occurredAt)
        implements DomainEvent {}
