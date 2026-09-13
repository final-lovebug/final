package com.ubidict.backend.draftdictionary.infra.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionRequestedEvent;
import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import com.ubidict.backend.draftdictionary.infra.port.TermExtractorPort;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionExecutionService;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDictionaryExtractionEventListenerTest {

    private final DraftDictionaryExtractionExecutionService executionService =
            mock(DraftDictionaryExtractionExecutionService.class);
    private final DocumentQueryPort documentQueryPort = mock(DocumentQueryPort.class);
    private final DictionaryTermQueryPort dictionaryTermQueryPort = mock(DictionaryTermQueryPort.class);
    private final TermExtractorPort termExtractorPort = mock(TermExtractorPort.class);
    private final DraftDictionaryExtractionEventListener listener = new DraftDictionaryExtractionEventListener(
            executionService, documentQueryPort, dictionaryTermQueryPort, termExtractorPort);

    @DisplayName("용어 추출 요청 이벤트를 받으면 대상 문서와 활성 용어로 추출기를 실행한다.")
    @Test
    void onRequested() {
        ExtractionJobResult job = new ExtractionJobResult(
                30L,
                1L,
                null,
                List.of(10L),
                ExtractionJobStatus.RUNNING,
                null,
                null,
                2L,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        ExtractedTerm term = new ExtractedTerm("결제", "정의", null, List.of(10L), 1, List.of("문맥"));
        given(executionService.start(30L)).willReturn(Optional.of(job));
        given(documentQueryPort.isExtractable(10L)).willReturn(true);
        given(dictionaryTermQueryPort.readActiveTerms(1L)).willReturn(List.of());
        given(termExtractorPort.extract(List.of(10L), List.of())).willReturn(List.of(term));

        listener.onRequested(new DraftDictionaryExtractionRequestedEvent(30L, OffsetDateTime.now()));

        verify(executionService).complete(30L, List.of(10L), List.of(term));
    }
}
