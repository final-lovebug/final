package com.ubidict.backend.draftdictionary.infra.event;

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
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionRequestedEvent;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionExecutionService;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 접수된 작업이 AI 워커로 나가는 경계.
 *
 * <p>전환 이전 이 자리의 테스트는 리스너가 스텁 포트를 부르고 {@code complete}까지 하는 것을 검증했다. 이제 리스너는 결과를 만들지 않는다 — 무엇을 어느
 * 계약으로 실어 보내는지, 그리고 발행이 실패했을 때 작업이 막히지 않는지가 검증 대상이다.
 */
class DraftDictionaryExtractionDispatchListenerTest {

    private final DraftDictionaryExtractionExecutionService executionService =
            mock(DraftDictionaryExtractionExecutionService.class);
    private final LlmJobRequestSender llmJobRequestSender = mock(LlmJobRequestSender.class);
    private final LlmProperties llmProperties = new LlmProperties(
            LlmMode.REAL, new LlmProperties.Timeout(true, Duration.ofMinutes(15), Duration.ofMinutes(1)));
    private final DraftDictionaryExtractionDispatchListener listener =
            new DraftDictionaryExtractionDispatchListener(executionService, llmJobRequestSender, llmProperties);

    @DisplayName("작업을 워커에게 넘기면서 발행한 상관 식별자를 작업에도 새긴다.")
    @Test
    void onRequested() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.of(running()));

        listener.onRequested(new DraftDictionaryExtractionRequestedEvent(30L, OffsetDateTime.now()));

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(executionService).markDispatching(eq(30L), requestIdCaptor.capture());
        ArgumentCaptor<LlmJobRequest> requestCaptor = ArgumentCaptor.forClass(LlmJobRequest.class);
        verify(llmJobRequestSender).send(requestCaptor.capture());

        LlmJobRequest request = requestCaptor.getValue();
        assertThat(request.requestId()).isEqualTo(requestIdCaptor.getValue());
        assertThat(request.contractVersion()).isEqualTo(LlmJobRequest.CONTRACT_VERSION);
        assertThat(request.jobType()).isEqualTo(LlmJobType.TERM_EXTRACTION);
        assertThat(request.jobId()).isEqualTo(30L);
        assertThat(request.workspaceId()).isEqualTo(1L);
        assertThat(request.sourceDocumentIds()).containsExactly(10L);
        assertThat(request.mode()).isEqualTo(LlmMode.REAL);
        assertThat(request.documentId()).isNull();
    }

    @DisplayName("대기 중이 아닌 작업은 워커에게 다시 보내지 않는다.")
    @Test
    void onRequested_notPending() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.empty());

        listener.onRequested(new DraftDictionaryExtractionRequestedEvent(30L, OffsetDateTime.now()));

        verifyNoInteractions(llmJobRequestSender);
    }

    @DisplayName("발행에 실패하면 작업을 실패로 끝낸다. 그대로 두면 다음 추출 요청이 계속 막힌다.")
    @Test
    void onRequested_publishFailed() {
        given(executionService.markDispatching(anyLong(), anyString())).willReturn(Optional.of(running()));
        willThrow(new IllegalStateException("queue down"))
                .given(llmJobRequestSender)
                .send(any());

        listener.onRequested(new DraftDictionaryExtractionRequestedEvent(30L, OffsetDateTime.now()));

        verify(executionService).expire(30L, "AI 작업 요청을 발행하지 못했습니다.");
    }

    private ExtractionJobResult running() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ExtractionJobResult(
                30L, 1L, null, List.of(10L), ExtractionJobStatus.RUNNING, null, null, 2L, now, now);
    }
}
