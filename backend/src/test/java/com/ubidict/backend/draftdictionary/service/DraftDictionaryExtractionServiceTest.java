package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobEventPublisher;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobReader;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobWriter;
import com.ubidict.backend.draftdictionary.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdictionary.service.model.CreateExtractionJobCommand;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDictionaryExtractionServiceTest {

    private final ExtractionJobReader reader = mock(ExtractionJobReader.class);
    private final ExtractionJobWriter writer = mock(ExtractionJobWriter.class);
    private final ExtractionJobCreationPolicyValidator jobPolicy = mock(ExtractionJobCreationPolicyValidator.class);
    private final DraftDictionaryCreationPolicyValidator draftPolicy =
            mock(DraftDictionaryCreationPolicyValidator.class);
    private final DocumentQueryPort documentQueryPort = mock(DocumentQueryPort.class);
    private final ExtractionJobEventPublisher eventPublisher = mock(ExtractionJobEventPublisher.class);
    private final WorkspaceAccessValidator accessValidator = mock(WorkspaceAccessValidator.class);
    private final DraftDictionaryExtractionService service = new DraftDictionaryExtractionService(
            reader, writer, jobPolicy, draftPolicy, documentQueryPort, eventPublisher, accessValidator);

    @DisplayName("용어 추출을 요청하면 대상 문서만 남긴 대기 작업과 요청 이벤트를 만든다.")
    @Test
    void request() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(job, "id", 30L);
        given(documentQueryPort.isExtractable(10L)).willReturn(true);
        given(documentQueryPort.isExtractable(20L)).willReturn(false);
        given(writer.append(1L, null, List.of(10L), 2L)).willReturn(job);

        ExtractionJobResult result = service.request(new CreateExtractionJobCommand(1L, null, List.of(10L, 20L), 2L));

        assertThat(result.extractionJobId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(ExtractionJobStatus.PENDING);
        assertThat(result.sourceDocumentIds()).containsExactly(10L);
        verify(draftPolicy).validate(1L);
        verify(jobPolicy).validate(1L);
        verify(eventPublisher).publishRequested(job);
    }
}
