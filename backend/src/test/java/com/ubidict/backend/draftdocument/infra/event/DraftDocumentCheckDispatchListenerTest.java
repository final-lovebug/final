package com.ubidict.backend.draftdocument.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobRequestSender;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.common.infra.ai.LlmMode;
import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCheckRequestedEvent;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckExecutionService;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 접수된 작업이 AI 워커로 나가는 경계.
 *
 * <p>전환 이전 이 자리의 테스트는 리스너가 스텁 포트를 부르고 {@code complete}까지 하는 것을 검증했다. 이제 리스너는 결과를 만들지 않는다 — 무엇을 어느
 * 계약으로 실어 보내는지가 검증 대상이다.
 */
class DraftDocumentCheckDispatchListenerTest {

    private final DraftDocumentCheckExecutionService executionService = mock(DraftDocumentCheckExecutionService.class);
    private final DocumentQueryPort documentQueryPort = mock(DocumentQueryPort.class);
    private final LlmJobRequestSender llmJobRequestSender = mock(LlmJobRequestSender.class);
    private final LlmProperties llmProperties = new LlmProperties(
            LlmMode.STUB, new LlmProperties.Timeout(true, Duration.ofMinutes(15), Duration.ofMinutes(1)));
    private final DraftDocumentCheckDispatchListener listener = new DraftDocumentCheckDispatchListener(
            executionService, documentQueryPort, llmJobRequestSender, llmProperties);

    @DisplayName("작업을 워커에게 넘기면서 읽어야 할 문서 버전을 함께 싣는다.")
    @Test
    void onRequested() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.of(running()));
        given(documentQueryPort.read(10L)).willReturn(Optional.of(new DocumentSnapshot(10L, 20L, 3, "본문")));

        listener.onRequested(new DraftDocumentCheckRequestedEvent(40L, 10L, 30L, OffsetDateTime.now()));

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(executionService).markDispatching(eq(40L), requestIdCaptor.capture());
        ArgumentCaptor<LlmJobRequest> requestCaptor = ArgumentCaptor.forClass(LlmJobRequest.class);
        verify(llmJobRequestSender).send(requestCaptor.capture());

        LlmJobRequest request = requestCaptor.getValue();
        assertThat(request.requestId()).isEqualTo(requestIdCaptor.getValue());
        assertThat(request.contractVersion()).isEqualTo(LlmJobRequest.CONTRACT_VERSION);
        assertThat(request.jobType()).isEqualTo(LlmJobType.DOCUMENT_CHECK);
        assertThat(request.jobId()).isEqualTo(40L);
        assertThat(request.workspaceId()).isEqualTo(20L);
        assertThat(request.documentId()).isEqualTo(10L);
        assertThat(request.documentVersionNo()).isEqualTo(3);
        assertThat(request.mode()).isEqualTo(LlmMode.STUB);
        assertThat(request.sourceDocumentIds()).isNull();
    }

    @DisplayName("대기 중이 아닌 작업은 워커에게 다시 보내지 않는다.")
    @Test
    void onRequested_notPending() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.empty());

        listener.onRequested(new DraftDocumentCheckRequestedEvent(40L, 10L, 30L, OffsetDateTime.now()));

        verifyNoInteractions(llmJobRequestSender);
    }

    @DisplayName("문서를 읽지 못하면 작업을 실패로 끝낸다.")
    @Test
    void onRequested_documentMissing() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.of(running()));
        given(documentQueryPort.read(10L)).willReturn(Optional.empty());

        listener.onRequested(new DraftDocumentCheckRequestedEvent(40L, 10L, 30L, OffsetDateTime.now()));

        verify(executionService).expire(40L, "대상 문서를 찾을 수 없습니다.");
        verifyNoInteractions(llmJobRequestSender);
    }

    @DisplayName("발행에 실패하면 작업을 실패로 끝낸다. 그대로 두면 그 문서의 대조가 계속 막힌다.")
    @Test
    void onRequested_publishFailed() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.of(running()));
        given(documentQueryPort.read(10L)).willReturn(Optional.of(new DocumentSnapshot(10L, 20L, 3, "본문")));
        willThrow(new IllegalStateException("queue down"))
                .given(llmJobRequestSender)
                .send(any());

        listener.onRequested(new DraftDocumentCheckRequestedEvent(40L, 10L, 30L, OffsetDateTime.now()));

        verify(executionService).expire(40L, "AI 작업 요청을 발행하지 못했습니다.");
    }

    private CheckJobResult running() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CheckJobResult(40L, 10L, CheckJobStatus.RUNNING, null, null, 30L, now, now);
    }
}
