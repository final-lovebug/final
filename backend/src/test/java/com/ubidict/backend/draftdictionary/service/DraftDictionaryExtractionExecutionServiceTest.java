package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.implement.CandidateTermWriter;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryEventPublisher;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryWriter;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobCompletionEventPublisher;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobReader;
import com.ubidict.backend.draftdictionary.implement.ExtractionResultValidator;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDictionaryExtractionExecutionServiceTest {

    private final ExtractionJobReader jobReader = mock(ExtractionJobReader.class);
    private final DraftDictionaryWriter draftWriter = mock(DraftDictionaryWriter.class);
    private final CandidateTermWriter termWriter = mock(CandidateTermWriter.class);
    private final DraftDictionaryCreationPolicyValidator creationPolicy =
            mock(DraftDictionaryCreationPolicyValidator.class);
    private final ExtractionResultValidator resultValidator = mock(ExtractionResultValidator.class);
    private final DraftDictionaryEventPublisher draftEventPublisher = mock(DraftDictionaryEventPublisher.class);
    private final ExtractionJobCompletionEventPublisher completionEventPublisher =
            mock(ExtractionJobCompletionEventPublisher.class);
    private final DraftDictionaryExtractionExecutionService service = new DraftDictionaryExtractionExecutionService(
            jobReader,
            draftWriter,
            termWriter,
            creationPolicy,
            resultValidator,
            draftEventPublisher,
            completionEventPublisher);

    @DisplayName("대기 중인 용어 추출 작업을 실행 중 상태로 바꾼다.")
    @Test
    void start() {
        ExtractionJob job = job();
        given(jobReader.read(30L)).willReturn(job);

        var result = service.start(30L);

        assertThat(result).get().extracting(item -> item.status()).isEqualTo(ExtractionJobStatus.RUNNING);
    }

    @DisplayName("추출 결과로 사전 초안과 후보어를 만들고 작업을 완료한다.")
    @Test
    void complete() {
        ExtractionJob job = job();
        job.start();
        DraftDictionary draft = DraftDictionary.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(draft, "id", 40L);
        ExtractedTerm term = new ExtractedTerm("결제", "정의", "Payment", List.of(10L), 2, List.of("문맥"));
        given(jobReader.read(30L)).willReturn(job);
        given(draftWriter.create(1L, null, List.of(10L), 2L)).willReturn(draft);

        service.complete(30L, List.of(10L), List.of(term));

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
        assertThat(job.getDraftDictionaryId()).isEqualTo(40L);
        verify(creationPolicy).validate(1L);
        verify(resultValidator).validate(List.of(10L), List.of(term));
        verify(termWriter).add(any());
        verify(draftEventPublisher).publishCreated(draft);
        verify(completionEventPublisher).publishCompleted(job);
    }

    private ExtractionJob job() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(job, "id", 30L);
        return job;
    }
}
