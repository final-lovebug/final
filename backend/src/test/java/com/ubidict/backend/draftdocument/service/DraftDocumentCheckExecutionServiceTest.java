package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.implement.CheckJobCompletionEventPublisher;
import com.ubidict.backend.draftdocument.implement.CheckJobReader;
import com.ubidict.backend.draftdocument.implement.CheckSuggestionValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentCreationPolicyValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentEventPublisher;
import com.ubidict.backend.draftdocument.implement.DraftDocumentWriter;
import com.ubidict.backend.draftdocument.implement.SuggestionTermWriter;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDocumentCheckExecutionServiceTest {

    private final CheckJobReader checkJobReader = mock(CheckJobReader.class);
    private final DraftDocumentWriter draftDocumentWriter = mock(DraftDocumentWriter.class);
    private final SuggestionTermWriter suggestionTermWriter = mock(SuggestionTermWriter.class);
    private final DraftDocumentCreationPolicyValidator creationPolicyValidator =
            mock(DraftDocumentCreationPolicyValidator.class);
    private final CheckSuggestionValidator checkSuggestionValidator = mock(CheckSuggestionValidator.class);
    private final DraftDocumentEventPublisher draftDocumentEventPublisher = mock(DraftDocumentEventPublisher.class);
    private final CheckJobCompletionEventPublisher completionEventPublisher =
            mock(CheckJobCompletionEventPublisher.class);
    private final DraftDocumentCheckExecutionService service = new DraftDocumentCheckExecutionService(
            checkJobReader,
            draftDocumentWriter,
            suggestionTermWriter,
            creationPolicyValidator,
            checkSuggestionValidator,
            draftDocumentEventPublisher,
            completionEventPublisher);

    @DisplayName("대조 작업을 완료하면 최신 본문으로 초안을 만들고 성공 상태를 기록한다.")
    @Test
    void complete() {
        CheckJob checkJob = CheckJob.create(10L, 30L);
        checkJob.start();
        DraftDocument draftDocument = DraftDocument.create(10L, 2, "최신 본문", 30L, 30L);
        ReflectionTestUtils.setField(draftDocument, "id", 50L);
        DocumentSnapshot document = new DocumentSnapshot(10L, 20L, 2, "최신 본문");
        given(checkJobReader.read(40L)).willReturn(checkJob);
        given(draftDocumentWriter.append(anyLong(), anyInt(), anyString(), anyLong(), anyLong()))
                .willReturn(draftDocument);

        service.complete(40L, document, List.of());

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
        assertThat(checkJob.getDraftDocumentId()).isEqualTo(50L);
        verify(draftDocumentEventPublisher).publishCreated(draftDocument);
        verify(completionEventPublisher).publishCompleted(checkJob);
    }
}
