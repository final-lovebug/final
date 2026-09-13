package com.ubidict.backend.common.infra.event.sqs;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.infra.event.EventPublisher;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 배포 환경의 발행 어댑터(D-24·D-49).
 *
 * <p>{@code InMemoryEventPublisher}와 <b>정확히 배타적</b>이다 — 그쪽은 {@code havingValue = "in-memory",
 * matchIfMissing = true}라 값을 주지 않으면 인메모리가 뜨고, {@code sqs}를 주면 이쪽만 뜬다.
 *
 * <p>{@code ARCHITECTURE.md} «이벤트 발행 규약 — 배치»가 배포용 어댑터를 이 자리에 두라고 규정한다. 상위 레이어는 {@link EventPublisher}
 * 포트만 알고 어느 어댑터가 떴는지 모른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "sqs")
public class SqsEventPublisher implements EventPublisher {

    private final SqsTemplate sqsTemplate;
    private final EventEnvelopeCodec codec;

    @Value("${app.messaging.sqs.queue}")
    private String queue;

    /**
     * <b>표준 큐에 보낸다</b>(D-50). {@code MessageGroupId}·{@code MessageDeduplicationId}는 붙이지 않는다 — FIFO 전용
     * 파라미터라 표준 큐에 실어 보내면 SQS가 무시하는 것이 아니라 {@code InvalidParameterValue}로 거절한다.
     *
     * <p>그래서 <b>순서도 보장되지 않고 중복 수신도 일어난다.</b> 중복은 소비 측이 막는다 — 알림은 이벤트 내용에서 파생한
     * {@code dedupeKey}에 유니크 제약을 걸어 재수신이 행을 늘리지 않게 한다(D-48).
     */
    @Override
    public void publish(DomainEvent event) {
        String aggregateId = DomainEventMetadata.aggregateId(event);
        String body = codec.encode(event, aggregateId, DomainEventMetadata.occurredAt(event));

        sqsTemplate.send(to -> to.queue(queue).payload(body));

        log.info(
                "[SqsEventPublisher.publish] Event published. eventType={}, aggregateId={}, queue={}",
                event.getClass().getSimpleName(),
                aggregateId,
                queue);
    }
}
