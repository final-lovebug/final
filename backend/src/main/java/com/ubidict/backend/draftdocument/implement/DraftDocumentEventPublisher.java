package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCreatedEvent;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentExaminedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDocumentEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishCreated(DraftDocument draftDocument) {
        eventPublisher.publish(new DraftDocumentCreatedEvent(
                draftDocument.getId(), draftDocument.getDocumentId(), OffsetDateTime.now()));
    }

    public void publishExamined(DraftDocument draftDocument) {
        eventPublisher.publish(new DraftDocumentExaminedEvent(
                draftDocument.getId(), draftDocument.getDocumentId(), OffsetDateTime.now()));
    }
}
