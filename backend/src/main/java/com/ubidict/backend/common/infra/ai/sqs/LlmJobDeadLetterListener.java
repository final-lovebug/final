package com.ubidict.backend.common.infra.ai.sqs;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.infra.ai.LlmJobFailureHandler;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 워커 재시도가 소진되어 DLQ에 들어온 요청을 Job 실패 상태로 회수한다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.dispatch.mode", havingValue = "sqs")
public class LlmJobDeadLetterListener {

    private static final String FAILURE_REASON = "AI 워커 재시도 소진으로 DLQ로 이동했습니다.";

    private final ObjectMapper objectMapper;
    private final List<LlmJobFailureHandler> failureHandlers;

    @SqsListener("${app.messaging.sqs.llm-dead-letter-queue}")
    public void onMessage(String payload) {
        LlmJobRequest request;
        try {
            request = objectMapper.readValue(payload, LlmJobRequest.class);
        } catch (Exception exception) {
            log.error("[LlmJobDeadLetterListener.onMessage] Invalid LLM DLQ payload acknowledged.", exception);
            return;
        }

        try {
            LlmJobFailureHandler handler = failureHandlers.stream()
                    .filter(candidate -> candidate.supports(request.jobType()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException("LLM 작업 failure handler가 없습니다. jobType=" + request.jobType()));
            handler.fail(request.jobId(), request.requestId(), FAILURE_REASON);
        } catch (BusinessException exception) {
            // 이미 끝난 Job, 지워진 Job, 불일치 requestId는 재시도해도 회복되지 않는다.
            log.error(
                    "[LlmJobDeadLetterListener.onMessage] LLM DLQ message rejected and acknowledged. jobType={}, jobId={}, code={}",
                    request.jobType(),
                    request.jobId(),
                    exception.errorCode());
        }
    }
}
