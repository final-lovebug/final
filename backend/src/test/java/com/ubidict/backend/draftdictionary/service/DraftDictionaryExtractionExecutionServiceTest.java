package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.implement.CandidateTermReader;
import com.ubidict.backend.draftdictionary.implement.CandidateTermWriter;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryEventPublisher;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryWriter;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobCompletionEventPublisher;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobReader;
import com.ubidict.backend.draftdictionary.implement.ExtractionResultValidator;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDictionaryExtractionExecutionServiceTest {

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";
    private static final String OTHER_REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000002";

    private final ExtractionJobReader jobReader = mock(ExtractionJobReader.class);
    private final DraftDictionaryWriter draftWriter = mock(DraftDictionaryWriter.class);
    private final CandidateTermWriter termWriter = mock(CandidateTermWriter.class);
    private final CandidateTermReader termReader = mock(CandidateTermReader.class);
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
            termReader,
            creationPolicy,
            resultValidator,
            draftEventPublisher,
            completionEventPublisher);

    @DisplayName("이미 승계된 표기가 추출 결과에 다시 나오면 후보어로 더하지 않는다.")
    @Test
    void complete_skipsCarriedOverForm() {
        // 활성 사전집의 용어는 초안 생성 시 승계 후보어로 실린다. 문서가 같은 어휘를 계속 쓰므로
        // 추출 결과에도 다시 나오는데, 그대로 add하면 DUPLICATE_CANDIDATE_FORM으로 추출 작업
        // 전체가 실패한다 — 후보어를 전부 발행하게 된 뒤로는(D-88) 이게 정상 경로다.
        ExtractionJob job = dispatchedJob();
        DraftDictionary draft = DraftDictionary.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(draft, "id", 40L);
        given(jobReader.read(30L)).willReturn(job);
        given(draftWriter.create(1L, null, List.of(10L), 2L)).willReturn(draft);
        given(termReader.readAll(40L))
                .willReturn(List.of(CandidateTerm.createExisting(40L, 7L, "결제", "기존 정의", "Payment", 2L)));
        ExtractedTerm carriedOver = new ExtractedTerm("결제", "새 정의", "Payment", List.of(10L), 2, List.of("문맥"));
        ExtractedTerm fresh = new ExtractedTerm("주문", "정의", "Order", List.of(10L), 2, List.of("문맥"));

        service.complete(30L, REQUEST_ID, List.of(10L), List.of(carriedOver, fresh));

        ArgumentCaptor<AddCandidateTermCommand> captor = ArgumentCaptor.forClass(AddCandidateTermCommand.class);
        verify(termWriter).add(captor.capture());
        assertThat(captor.getValue().form()).isEqualTo("주문");
        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
    }

    @DisplayName("대기 중인 작업을 워커에게 넘기면 실행 중 상태가 되고 상관 식별자가 남는다.")
    @Test
    void markDispatching() {
        ExtractionJob job = job();
        given(jobReader.read(30L)).willReturn(job);

        var result = service.markDispatching(30L, REQUEST_ID);

        assertThat(result).get().extracting(item -> item.status()).isEqualTo(ExtractionJobStatus.RUNNING);
        assertThat(job.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @DisplayName("대기 중이 아닌 작업은 워커에게 넘기지 않는다.")
    @Test
    void markDispatching_notPending() {
        ExtractionJob job = job();
        job.markDispatching(REQUEST_ID);
        given(jobReader.read(30L)).willReturn(job);

        assertThat(service.markDispatching(30L, OTHER_REQUEST_ID)).isEmpty();
    }

    @DisplayName("추출 결과로 사전 초안과 후보어를 만들고 작업을 완료한다.")
    @Test
    void complete() {
        ExtractionJob job = dispatchedJob();
        DraftDictionary draft = DraftDictionary.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(draft, "id", 40L);
        ExtractedTerm term = new ExtractedTerm("결제", "정의", "Payment", List.of(10L), 2, List.of("문맥"));
        given(jobReader.read(30L)).willReturn(job);
        given(draftWriter.create(1L, null, List.of(10L), 2L)).willReturn(draft);

        service.complete(30L, REQUEST_ID, List.of(10L), List.of(term));

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
        assertThat(job.getDraftDictionaryId()).isEqualTo(40L);
        verify(creationPolicy).validate(1L);
        verify(resultValidator).validate(List.of(10L), List.of(term));
        verify(termWriter).add(any());
        verify(draftEventPublisher).publishCreated(draft);
        verify(completionEventPublisher).publishCompleted(job);
    }

    @DisplayName("상관 식별자가 다른 콜백은 결과를 반영하지 않고 거절한다.")
    @Test
    void complete_requestIdMismatch() {
        ExtractionJob job = dispatchedJob();
        given(jobReader.read(30L)).willReturn(job);

        assertThatThrownBy(() -> service.complete(30L, OTHER_REQUEST_ID, List.of(10L), List.of()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_CALLBACK_FORBIDDEN));
        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
        verifyNoInteractions(draftWriter);
    }

    @DisplayName("이미 끝난 작업에 도착한 결과 콜백은 초안을 다시 만들지 않는다.")
    @Test
    void complete_terminalJob() {
        ExtractionJob job = dispatchedJob();
        job.succeed(40L);
        given(jobReader.read(30L)).willReturn(job);

        service.complete(30L, REQUEST_ID, List.of(10L), List.of());

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
        assertThat(job.getDraftDictionaryId()).isEqualTo(40L);
        verifyNoInteractions(draftWriter);
        verifyNoInteractions(creationPolicy);
    }

    @DisplayName("워커가 알린 실패를 사유와 함께 기록한다.")
    @Test
    void fail() {
        ExtractionJob job = dispatchedJob();
        given(jobReader.read(30L)).willReturn(job);

        service.fail(30L, REQUEST_ID, "모델 응답이 스키마를 만족하지 않습니다.");

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.FAILED);
        assertThat(job.getFailureReason()).isEqualTo("모델 응답이 스키마를 만족하지 않습니다.");
    }

    @DisplayName("상관 식별자가 다르면 작업을 실패시킬 수 없다.")
    @Test
    void fail_requestIdMismatch() {
        ExtractionJob job = dispatchedJob();
        given(jobReader.read(30L)).willReturn(job);

        assertThatThrownBy(() -> service.fail(30L, OTHER_REQUEST_ID, "실패"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_CALLBACK_FORBIDDEN));
        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
    }

    @DisplayName("이미 성공한 작업에 늦게 도착한 실패 콜백은 성공을 뒤집지 않는다.")
    @Test
    void fail_afterSucceeded() {
        ExtractionJob job = dispatchedJob();
        job.succeed(40L);
        given(jobReader.read(30L)).willReturn(job);

        service.fail(30L, REQUEST_ID, "늦게 도착한 실패");

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
        assertThat(job.getFailureReason()).isNull();
    }

    @DisplayName("타임아웃 회수는 상관 식별자 없이 작업을 실패로 끝낸다.")
    @Test
    void expire() {
        ExtractionJob job = dispatchedJob();
        given(jobReader.read(30L)).willReturn(job);

        service.expire(30L, "AI 작업이 제한 시간 안에 완료되지 않았습니다.");

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.FAILED);
        assertThat(job.getFailureReason()).isEqualTo("AI 작업이 제한 시간 안에 완료되지 않았습니다.");
    }

    @DisplayName("추출 결과의 표기 변형(그룹핑 정보)을 후보어 추가 커맨드까지 그대로 넘긴다.")
    @Test
    void complete_preservesVariantForms() {
        ExtractionJob job = dispatchedJob();
        DraftDictionary draft = DraftDictionary.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(draft, "id", 40L);
        ExtractedTerm term = new ExtractedTerm(
                "고객", "정의", "Customer", List.of(10L), 2, List.of("문맥"), List.of("고객", "커스터머", "클라이언트"));
        given(jobReader.read(30L)).willReturn(job);
        given(draftWriter.create(1L, null, List.of(10L), 2L)).willReturn(draft);

        service.complete(30L, REQUEST_ID, List.of(10L), List.of(term));

        ArgumentCaptor<AddCandidateTermCommand> captor = ArgumentCaptor.forClass(AddCandidateTermCommand.class);
        verify(termWriter).add(captor.capture());
        assertThat(captor.getValue().variantForms()).containsExactly("고객", "커스터머", "클라이언트");
    }

    private ExtractionJob job() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(job, "id", 30L);
        return job;
    }

    private ExtractionJob dispatchedJob() {
        ExtractionJob job = job();
        job.markDispatching(REQUEST_ID);
        return job;
    }
}
