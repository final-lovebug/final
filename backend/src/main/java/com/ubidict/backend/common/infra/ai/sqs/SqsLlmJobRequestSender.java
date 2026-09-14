package com.ubidict.backend.common.infra.ai.sqs;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobRequestSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 워커용 발행 어댑터(D-67). <b>도메인 이벤트 큐가 아니라 전용 LLM 요청 큐</b>로 보낸다.
 *
 * <p>{@code EventEnvelopeCodec}을 쓰지 않고 {@code ObjectMapper}를 직접 잡는다 — 그 코덱은
 * {@code app.messaging.mode=sqs}일 때만 뜨는 빈이라 재사용하면 LLM 경로가 도메인 이벤트 축에 묶인다. 게다가 봉투가 풀려던 문제(마커 인터페이스 구현이
 * 16종이라 수신 측이 타입을 모른다)가 여기에는 없다 — {@link LlmJobRequest} 하나뿐이다.
 *
 * <p>{@code SqsEventPublisher}와 마찬가지로 <b>표준 큐</b>라 {@code MessageGroupId}·{@code MessageDeduplicationId}를
 * 붙이지 않는다(D-53). 중복 수신은 콜백 쪽 멱등이 막는다(D-72).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.dispatch.mode", havingValue = "sqs")
public class SqsLlmJobRequestSender implements LlmJobRequestSender {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.messaging.sqs.llm-request-queue}")
    private String queue;

    @Override
    public void send(LlmJobRequest request) {
        String body;
        try {
            body = objectMapper.writeValueAsString(request);
        } catch (JacksonException exception) {
            throw new BusinessException(
                    MessagingErrorCode.MESSAGING_LLM_REQUEST_PUBLISH_FAILED,
                    "AI 작업 요청 직렬화 실패. jobType=" + request.jobType() + ", jobId=" + request.jobId(),
                    exception);
        }

        sqsTemplate.send(to -> to.queue(queue).payload(body));

        log.info(
                "[SqsLlmJobRequestSender.send] Llm job request published. jobType={}, jobId={}, requestId={}, mode={}, queue={}",
                request.jobType(),
                request.jobId(),
                request.requestId(),
                request.mode(),
                queue);
    }
}
