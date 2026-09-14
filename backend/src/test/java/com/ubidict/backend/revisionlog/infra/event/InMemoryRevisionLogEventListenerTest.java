package com.ubidict.backend.revisionlog.infra.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.revisionlog.service.RevisionLogEventHandler;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRevisionLogEventListenerTest {

    @DisplayName("문서 직접 편집 이벤트를 공용 핸들러에 위임한다.")
    @Test
    void on_documentEdited() {
        RevisionLogEventHandler handler = mock(RevisionLogEventHandler.class);
        InMemoryRevisionLogEventListener listener = new InMemoryRevisionLogEventListener(handler);
        DocumentEditedEvent event = new DocumentEditedEvent(20L, 10L, 2, OffsetDateTime.now());

        listener.on(event);

        verify(handler).handle(event);
    }
}
