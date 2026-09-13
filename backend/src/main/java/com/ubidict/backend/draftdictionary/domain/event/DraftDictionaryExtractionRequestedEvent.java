package com.ubidict.backend.draftdictionary.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

public record DraftDictionaryExtractionRequestedEvent(Long extractionJobId, OffsetDateTime occurredAt)
        implements DomainEvent {}
