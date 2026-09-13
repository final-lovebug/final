package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionCompletedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobCompletionEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishCompleted(ExtractionJob extractionJob) {
        eventPublisher.publish(new DraftDictionaryExtractionCompletedEvent(
                extractionJob.getId(),
                extractionJob.getWorkspaceId(),
                extractionJob.getDraftDictionaryId(),
                OffsetDateTime.now()));
    }
}
