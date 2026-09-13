package com.ubidict.backend.common.infra.event.sqs;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import java.lang.reflect.RecordComponent;
import java.time.OffsetDateTime;

/**
 * 이벤트 record에서 봉투에 쓸 값을 꺼낸다.
 *
 * <p><b>왜 리플렉션인가</b> — {@code DomainEvent}는 접근자가 없는 마커 인터페이스이고, 6개 도메인이 만든 이벤트 record 16종은 공통 상위 타입을
 * 갖지 않는다. 봉투에 필요한 두 값({@code aggregateId}·{@code occurredAt})을 꺼내자고 16개 record에 인터페이스를 강제하면 남의 도메인
 * 파일을 전부 고쳐야 한다.
 *
 * <p>규약은 이벤트 발행 규약이 이미 보장한다 — 이벤트는 <b>불변 record</b>이고 모두 {@code occurredAt}으로 끝난다.
 */
final class DomainEventMetadata {

    private DomainEventMetadata() {}

    /**
     * 이 이벤트가 속한 집합체 식별자. 봉투에 실려 추적·집계에 쓰인다.
     *
     * <p>{@code workspaceId}가 있으면 그것을 쓰고, 없으면 첫 식별자 컴포넌트로 대체한다. 이벤트 5종 중 {@code workspaceId}를 가진 것은
     * 하나뿐이라 이 대체 경로가 실제로 쓰인다.
     *
     * <p><b>지금은 순서 보장에 쓰이지 않는다</b> — 표준 큐를 쓰기로 했고(D-50) {@code MessageGroupId}는 FIFO 전용이다. 나중에 FIFO로
     * 바꾸면 이 값이 그대로 그룹 키가 된다.
     */
    static String aggregateId(DomainEvent event) {
        Object workspaceId = component(event, "workspaceId");
        if (workspaceId != null) {
            return String.valueOf(workspaceId);
        }

        for (RecordComponent recordComponent : components(event)) {
            if (recordComponent.getType() == Long.class) {
                Object value = read(event, recordComponent);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }

        return event.getClass().getSimpleName();
    }

    static OffsetDateTime occurredAt(DomainEvent event) {
        Object value = component(event, "occurredAt");

        return value instanceof OffsetDateTime occurredAt ? occurredAt : OffsetDateTime.now();
    }

    private static Object component(DomainEvent event, String name) {
        for (RecordComponent recordComponent : components(event)) {
            if (recordComponent.getName().equals(name)) {
                return read(event, recordComponent);
            }
        }

        return null;
    }

    private static RecordComponent[] components(DomainEvent event) {
        RecordComponent[] components = event.getClass().getRecordComponents();

        return components == null ? new RecordComponent[0] : components;
    }

    private static Object read(DomainEvent event, RecordComponent recordComponent) {
        try {
            return recordComponent.getAccessor().invoke(event);
        } catch (ReflectiveOperationException exception) {
            throw new BusinessException(
                    MessagingErrorCode.MESSAGING_EVENT_SERIALIZATION_FAILED,
                    "이벤트 컴포넌트 읽기 실패. event=" + event.getClass().getName(),
                    exception);
        }
    }
}
