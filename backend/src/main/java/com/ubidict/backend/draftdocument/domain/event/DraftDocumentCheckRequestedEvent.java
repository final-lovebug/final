package com.ubidict.backend.draftdocument.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

public record DraftDocumentCheckRequestedEvent(
        Long checkJobId, Long documentId, Long requestedBy, OffsetDateTime occurredAt) implements DomainEvent {}
