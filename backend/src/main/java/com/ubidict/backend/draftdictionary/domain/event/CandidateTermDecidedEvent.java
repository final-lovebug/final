package com.ubidict.backend.draftdictionary.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 후보어 하나의 판정이 끝났다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 DI-4가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record CandidateTermDecidedEvent(Long draftDictionaryId, Long candidateTermId, OffsetDateTime occurredAt)
        implements DomainEvent {}
