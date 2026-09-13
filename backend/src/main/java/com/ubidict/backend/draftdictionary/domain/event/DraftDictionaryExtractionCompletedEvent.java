package com.ubidict.backend.draftdictionary.domain.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;

public record DraftDictionaryExtractionCompletedEvent(
        Long extractionJobId, Long workspaceId, Long draftDictionaryId, OffsetDateTime occurredAt)
        implements DomainEvent {}
