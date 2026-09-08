package com.ubidict.backend.common.infra.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 같은 JVM 안에서 이벤트를 전달하는 기본 어댑터.
 *
 * <p>로컬 실행과 테스트에서 별도 인프라 없이 이벤트 흐름을 확인하기 위해 사용한다. 배포 환경에서 외부 메시지 큐를 쓰게 되면 같은 포트를 구현하는 어댑터를 추가하고
 * {@code app.messaging.mode}로 전환한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "in-memory", matchIfMissing = true)
class InMemoryEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
