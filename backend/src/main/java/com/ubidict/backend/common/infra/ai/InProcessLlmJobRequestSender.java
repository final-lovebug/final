package com.ubidict.backend.common.infra.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 워커가 없는 환경(로컬·테스트)의 대역(D-74).
 *
 * <p>요청을 큐 대신 <b>같은 JVM의 스프링 이벤트로 흘린다.</b> 각 도메인의 로컬 워커가 그것을 듣고 목 결과로 작업을 끝내므로, LocalStack 없이도 접수부터
 * 완료까지의 흐름을 그대로 볼 수 있다 — 전환 전 스텁 리스너가 주던 개발 경험을 보존하기 위한 것이다.
 *
 * <p><b>여기서 콜백 서비스를 직접 부르지 않는 이유</b> — {@code common}이 초안 도메인의 서비스를 알게 되면 의존 방향이 뒤집힌다. 무엇을 목 결과로 삼을지는
 * 각 도메인이 정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.dispatch.mode", havingValue = "in-process", matchIfMissing = true)
class InProcessLlmJobRequestSender implements LlmJobRequestSender {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void send(LlmJobRequest request) {
        log.info(
                "[InProcessLlmJobRequestSender.send] Llm job request dispatched in process. jobType={}, jobId={}, requestId={}",
                request.jobType(),
                request.jobId(),
                request.requestId());
        applicationEventPublisher.publishEvent(request);
    }
}
