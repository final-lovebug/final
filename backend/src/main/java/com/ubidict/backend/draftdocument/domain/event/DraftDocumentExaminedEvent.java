package com.ubidict.backend.draftdocument.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 문서 초안의 교정이 끝났다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 DD-4가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record DraftDocumentExaminedEvent(Long draftDocumentId, Long documentId, OffsetDateTime occurredAt)
        implements DomainEvent {}
