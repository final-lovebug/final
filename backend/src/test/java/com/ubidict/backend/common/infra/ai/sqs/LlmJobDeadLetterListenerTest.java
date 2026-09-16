package com.ubidict.backend.common.infra.ai.sqs;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ubidict.backend.common.infra.ai.LlmJobFailureHandler;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.common.infra.ai.LlmMode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class LlmJobDeadLetterListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmJobFailureHandler failureHandler = mock(LlmJobFailureHandler.class);
    private final LlmJobDeadLetterListener listener =
            new LlmJobDeadLetterListener(objectMapper, List.of(failureHandler));

    @DisplayName("DLQ 요청은 같은 상관 식별자로 Job 실패 보상에 위임한다.")
    @Test
    void onMessage() throws Exception {
        LlmJobRequest request = LlmJobRequest.termExtraction(
                "0d5c6f6e-0000-4000-8000-000000000001", 30L, 10L, null, List.of(20L), LlmMode.REAL);
        when(failureHandler.supports(LlmJobType.TERM_EXTRACTION)).thenReturn(true);

        listener.onMessage(objectMapper.writeValueAsString(request));

        verify(failureHandler).fail(eq(30L), eq(request.requestId()), eq("AI 워커 재시도 소진으로 DLQ로 이동했습니다."));
    }

    @DisplayName("잘못된 DLQ 본문은 Job을 변경하지 않고 확인 처리한다.")
    @Test
    void onMessage_invalidPayload() {
        listener.onMessage("not-json");

        verifyNoInteractions(failureHandler);
    }
}
