package com.ubidict.backend.draftdocument.infra.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCheckRequestedEvent;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.infra.port.TermCheckerPort;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckExecutionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentCheckEventListenerTest {

    private final DraftDocumentCheckExecutionService executionService = mock(DraftDocumentCheckExecutionService.class);
    private final DocumentQueryPort documentQueryPort = mock(DocumentQueryPort.class);
    private final DictionaryTermQueryPort dictionaryTermQueryPort = mock(DictionaryTermQueryPort.class);
    private final TermCheckerPort termCheckerPort = mock(TermCheckerPort.class);
    private final DraftDocumentCheckEventListener listener = new DraftDocumentCheckEventListener(
            executionService, documentQueryPort, dictionaryTermQueryPort, termCheckerPort);

    @DisplayName("대조 요청 이벤트를 받으면 최신 문서와 활성 사전집으로 대조를 실행한다.")
    @Test
    void onRequested() {
        DraftDocumentCheckRequestedEvent event = new DraftDocumentCheckRequestedEvent(1L, 2L, 3L, OffsetDateTime.now());
        DocumentSnapshot document = new DocumentSnapshot(2L, 4L, 1, "본문");
        given(executionService.start(1L)).willReturn(true);
        given(documentQueryPort.read(2L)).willReturn(Optional.of(document));
        given(dictionaryTermQueryPort.hasActiveDictionary(4L)).willReturn(true);
        given(dictionaryTermQueryPort.readActiveTerms(4L)).willReturn(List.of());
        given(termCheckerPort.check(document, List.of())).willReturn(List.of());

        listener.onRequested(event);

        verify(executionService).complete(1L, document, List.of());
    }
}
