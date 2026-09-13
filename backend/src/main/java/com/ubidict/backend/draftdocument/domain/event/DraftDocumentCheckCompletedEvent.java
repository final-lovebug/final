package com.ubidict.backend.draftdocument.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

public record DraftDocumentCheckCompletedEvent(
        Long checkJobId, Long documentId, Long draftDocumentId, OffsetDateTime occurredAt) implements DomainEvent {}
