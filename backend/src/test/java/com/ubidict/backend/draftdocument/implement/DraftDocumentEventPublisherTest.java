package com.ubidict.backend.draftdocument.implement;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCreatedEvent;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentExaminedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentEventPublisherTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final DraftDocumentEventPublisher publisher = new DraftDocumentEventPublisher(eventPublisher);

    @DisplayName("문서 초안을 생성하면 생성 이벤트를 발행한다.")
    @Test
    void publishCreated() {
        DraftDocument draftDocument = DraftDocument.create(10L, 1, 1, "본문", 1L, 1L);

        publisher.publishCreated(draftDocument);

        verify(eventPublisher).publish(isA(DraftDocumentCreatedEvent.class));
    }

    @DisplayName("문서 초안 교정을 완료하면 교정완료 이벤트를 발행한다.")
    @Test
    void publishExamined() {
        DraftDocument draftDocument = DraftDocument.create(10L, 1, 1, "본문", 1L, 1L);

        publisher.publishExamined(draftDocument);

        verify(eventPublisher).publish(isA(DraftDocumentExaminedEvent.class));
    }
}
