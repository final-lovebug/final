package com.ubidict.backend.common.infra.ai;

/**
 * AI 워커에게 작업을 맡기는 포트(D-66).
 *
 * <p>{@code EventPublisher}와 <b>일부러 분리했다</b> — 이것은 여러 구독자에게 퍼지는 <i>사건 통지</i>가 아니라 특정 외부 서비스에 보내는
 * <i>명령</i>이다. 그래서 {@code DomainEvent} 마커도, 도메인 이벤트 큐도 쓰지 않는다(D-67).
 *
 * <p>어댑터 선택은 {@code app.ai.dispatch.mode}이며 <b>{@code app.messaging.mode}와 독립</b>이다. 도메인 이벤트를 인메모리로 두든
 * SQS로 두든 추출·대조는 그대로 동작한다 — 전환 이전에는 두 축이 얽혀 있어서 배포 프로파일에서 워커 경로가 통째로 죽어 있었다.
 */
public interface LlmJobRequestSender {

    void send(LlmJobRequest request);
}
