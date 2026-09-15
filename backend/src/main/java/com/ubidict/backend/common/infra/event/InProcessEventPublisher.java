package com.ubidict.backend.common.infra.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 같은 JVM 안에서만 전달되는 발행기. <b>{@code app.messaging.mode}와 무관하게 항상 존재한다.</b>
 *
 * <p>{@link EventPublisher}와 용도가 다르다 — 그쪽은 <i>도메인 이벤트 버스</i>이고 배포 환경에서는 SQS로 나간다. 이쪽은 같은 프로세스 안에서
 * 트랜잭션 커밋 뒤에 후속 작업을 잇기 위한 내부 신호 전용이다.
 *
 * <p><b>왜 나눴는가</b> — 추출·대조의 요청 이벤트는 {@code @TransactionalEventListener}가 받아야 하는데, 그 리스너는 스프링
 * {@code ApplicationEvent}만 받는다. 그것을 {@link EventPublisher}로 발행하면 SQS 모드에서는 이벤트가 큐로 나가 버려 리스너가 아예 호출되지
 * 않는다 — 실제로 배포 프로파일에서 추출·대조가 영구히 {@code PENDING}에 머무는 원인이었다. 내부 신호는 내부 발행기로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class InProcessEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
