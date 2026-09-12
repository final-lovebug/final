package com.ubidict.backend.document.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 문서가 삭제됐다. 초안 도메인이 딸린 초안을 정리한다.
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 DOC-10가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record DocumentDeletedEvent(Long documentId, Long workspaceId, OffsetDateTime occurredAt)
        implements DomainEvent {}
