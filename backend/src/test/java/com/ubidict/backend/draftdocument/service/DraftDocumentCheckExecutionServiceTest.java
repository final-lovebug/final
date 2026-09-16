package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.infra.ai.LlmJobOutboxService;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.implement.CheckJobCompletionEventPublisher;
import com.ubidict.backend.draftdocument.implement.CheckJobReader;
import com.ubidict.backend.draftdocument.implement.CheckSuggestionValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentCreationPolicyValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentEventPublisher;
import com.ubidict.backend.draftdocument.implement.DraftDocumentWriter;
import com.ubidict.backend.draftdocument.implement.SuggestionTermWriter;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDocumentCheckExecutionServiceTest {

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";
    private static final String OTHER_REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000002";

    private final CheckJobReader checkJobReader = mock(CheckJobReader.class);
    private final DocumentQueryPort documentQueryPort = mock(DocumentQueryPort.class);
    private final DictionaryTermQueryPort dictionaryTermQueryPort = mock(DictionaryTermQueryPort.class);
    private final DraftDocumentWriter draftDocumentWriter = mock(DraftDocumentWriter.class);
    private final SuggestionTermWriter suggestionTermWriter = mock(SuggestionTermWriter.class);
    private final DraftDocumentCreationPolicyValidator creationPolicyValidator =
            mock(DraftDocumentCreationPolicyValidator.class);
    private final CheckSuggestionValidator checkSuggestionValidator = mock(CheckSuggestionValidator.class);
    private final DraftDocumentEventPublisher draftDocumentEventPublisher = mock(DraftDocumentEventPublisher.class);
    private final CheckJobCompletionEventPublisher completionEventPublisher =
            mock(CheckJobCompletionEventPublisher.class);
    private final LlmJobOutboxService outboxService = mock(LlmJobOutboxService.class);
    private final DraftDocumentCheckExecutionService service = new DraftDocumentCheckExecutionService(
            checkJobReader,
            documentQueryPort,
            dictionaryTermQueryPort,
            draftDocumentWriter,
            suggestionTermWriter,
            creationPolicyValidator,
            checkSuggestionValidator,
            draftDocumentEventPublisher,
            completionEventPublisher,
            outboxService);

    @DisplayName("대기 중인 작업을 워커에게 넘기면 실행 중 상태가 되고 상관 식별자가 남는다.")
    @Test
    void markDispatching() {
        CheckJob checkJob = job();
        given(checkJobReader.read(40L)).willReturn(checkJob);

        var result = service.markDispatching(40L, REQUEST_ID);

        assertThat(result).get().extracting(item -> item.status()).isEqualTo(CheckJobStatus.RUNNING);
        assertThat(checkJob.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @DisplayName("대조 작업을 완료하면 최신 본문으로 초안을 만들고 성공 상태를 기록한다.")
    @Test
    void complete() {
        CheckJob checkJob = dispatchedJob();
        DraftDocument draftDocument = DraftDocument.create(10L, 2, 3, "최신 본문", 30L, 30L);
        ReflectionTestUtils.setField(draftDocument, "id", 50L);
        given(checkJobReader.read(40L)).willReturn(checkJob);
        given(documentQueryPort.read(10L)).willReturn(Optional.of(new DocumentSnapshot(10L, 20L, 2, "최신 본문")));
        given(dictionaryTermQueryPort.activeVersionNo(20L)).willReturn(OptionalInt.of(3));
        given(draftDocumentWriter.append(anyLong(), anyInt(), anyInt(), anyString(), anyLong(), anyLong()))
                .willReturn(draftDocument);

        service.complete(40L, REQUEST_ID, 2, List.of());

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
        assertThat(checkJob.getDraftDocumentId()).isEqualTo(50L);
        verify(draftDocumentWriter).append(10L, 2, 3, "최신 본문", 30L, 30L);
        verify(draftDocumentEventPublisher).publishCreated(draftDocument);
        verify(completionEventPublisher).publishCompleted(checkJob);
    }

    @DisplayName("대조를 끝냈는데 활성 사전집이 없으면 초안을 만들지 않고 작업을 실패로 끝낸다.")
    @Test
    void complete_activeDictionaryGone() {
        CheckJob checkJob = dispatchedJob();
        given(checkJobReader.read(40L)).willReturn(checkJob);
        given(documentQueryPort.read(10L)).willReturn(Optional.of(new DocumentSnapshot(10L, 20L, 2, "최신 본문")));
        given(dictionaryTermQueryPort.activeVersionNo(20L)).willReturn(OptionalInt.empty());

        service.complete(40L, REQUEST_ID, 2, List.of());

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.FAILED);
        assertThat(checkJob.getFailureReason()).isEqualTo("기준으로 삼을 활성 사전집이 없습니다.");
        verifyNoInteractions(draftDocumentWriter);
    }

    @DisplayName("대조하는 동안 문서가 새 버전이 되면 결과를 받지 않고 작업을 실패로 끝낸다.")
    @Test
    void complete_staleVersion() {
        CheckJob checkJob = dispatchedJob();
        given(checkJobReader.read(40L)).willReturn(checkJob);
        given(documentQueryPort.read(10L)).willReturn(Optional.of(new DocumentSnapshot(10L, 20L, 3, "더 새로운 본문")));

        service.complete(40L, REQUEST_ID, 2, List.of());

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.FAILED);
        assertThat(checkJob.getFailureReason()).isEqualTo("대조하는 동안 문서가 새 버전으로 바뀌었습니다.");
        verifyNoInteractions(draftDocumentWriter);
    }

    @DisplayName("상관 식별자가 다른 콜백은 결과를 반영하지 않고 거절한다.")
    @Test
    void complete_requestIdMismatch() {
        CheckJob checkJob = dispatchedJob();
        given(checkJobReader.read(40L)).willReturn(checkJob);

        assertThatThrownBy(() -> service.complete(40L, OTHER_REQUEST_ID, 2, List.of()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN));
        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.RUNNING);
        verifyNoInteractions(draftDocumentWriter);
    }

    @DisplayName("이미 끝난 작업에 도착한 결과 콜백은 초안을 다시 만들지 않는다.")
    @Test
    void complete_terminalJob() {
        CheckJob checkJob = dispatchedJob();
        checkJob.succeed(50L);
        given(checkJobReader.read(40L)).willReturn(checkJob);

        service.complete(40L, REQUEST_ID, 2, List.of());

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
        verifyNoInteractions(draftDocumentWriter);
        verifyNoInteractions(documentQueryPort);
    }

    @DisplayName("워커가 알린 실패를 사유와 함께 기록한다.")
    @Test
    void fail() {
        CheckJob checkJob = dispatchedJob();
        given(checkJobReader.read(40L)).willReturn(checkJob);

        service.fail(40L, REQUEST_ID, "모델 응답이 스키마를 만족하지 않습니다.");

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.FAILED);
        assertThat(checkJob.getFailureReason()).isEqualTo("모델 응답이 스키마를 만족하지 않습니다.");
    }

    @DisplayName("상관 식별자가 다르면 작업을 실패시킬 수 없다.")
    @Test
    void fail_requestIdMismatch() {
        CheckJob checkJob = dispatchedJob();
        given(checkJobReader.read(40L)).willReturn(checkJob);

        assertThatThrownBy(() -> service.fail(40L, OTHER_REQUEST_ID, "실패"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN));
        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.RUNNING);
    }

    @DisplayName("타임아웃 회수는 상관 식별자 없이 작업을 실패로 끝낸다.")
    @Test
    void expire() {
        CheckJob checkJob = dispatchedJob();
        given(checkJobReader.read(40L)).willReturn(checkJob);

        service.expire(40L, "AI 작업이 제한 시간 안에 완료되지 않았습니다.");

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.FAILED);
        assertThat(checkJob.getFailureReason()).isEqualTo("AI 작업이 제한 시간 안에 완료되지 않았습니다.");
    }

    private CheckJob job() {
        CheckJob checkJob = CheckJob.create(10L, 30L);
        ReflectionTestUtils.setField(checkJob, "id", 40L);
        return checkJob;
    }

    private CheckJob dispatchedJob() {
        CheckJob checkJob = job();
        checkJob.markDispatching(REQUEST_ID);
        return checkJob;
    }
}
