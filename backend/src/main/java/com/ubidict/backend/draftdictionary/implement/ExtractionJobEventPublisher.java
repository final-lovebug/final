package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionRequestedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishRequested(ExtractionJob extractionJob) {
        eventPublisher.publish(
                new DraftDictionaryExtractionRequestedEvent(extractionJob.getId(), OffsetDateTime.now()));
    }
}
