package com.ubidict.backend.common.infra.event;

import com.ubidict.backend.common.domain.event.DomainEvent;

/**
 * 도메인 이벤트 발행 포트.
 *
 * <p>상위 레이어는 이 인터페이스만 참조한다. 어떤 메시징 기술을 쓰는지는 구현체가 감춘다.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
