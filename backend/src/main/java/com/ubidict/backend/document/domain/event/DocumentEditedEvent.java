package com.ubidict.backend.document.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

/**
 * 문서 본문이 직접 편집돼 새 리비전으로 발행됐다(G-9).
 *
 * <p>선행 PR이 계약으로 먼저 만든다. 발행부는 DOC-10가 넣으며, 필요한 필드가 더 있으면 그 태스크가 더한다.
 */
public record DocumentEditedEvent(Long documentId, Long workspaceId, int versionNo, OffsetDateTime occurredAt)
        implements DomainEvent {}
