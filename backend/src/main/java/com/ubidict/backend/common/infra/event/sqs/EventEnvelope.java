package com.ubidict.backend.common.infra.event.sqs;

import java.time.OffsetDateTime;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * 큐로 나가는 메시지의 겉봉투. {@code NFR-MSG-001}의 「이벤트 Envelope(eventId·aggregateId·occurredAt) 표준화」다.
 *
 * <p><b>{@code payload}가 {@code DomainEvent}가 아니라 {@code JsonNode}인 이유</b> — {@code DomainEvent}는 구현이
 * 16종인 마커 인터페이스라, 타입 정보 없이 직렬화하면 수신 측이 어느 record로 되돌릴지 알 수 없다. Jackson에 타입 정보를 심는 방법도 있지만
 * ({@code @JsonSubTypes}) 그러면 {@code common}이 6개 도메인의 이벤트 클래스를 전부 import해야 해서 의존 방향이 뒤집힌다.
 *
 * <p>대신 <b>본문을 해석하지 않은 채로 싣고, 무엇인지는 {@code eventType}으로만 알린다.</b> 어떤 record로 되돌릴지는 그 이벤트를 구독하는 도메인이
 * 정한다 — 소비 도메인만 자기가 구독하는 이벤트를 안다.
 *
 * <p>{@code eventId}는 발행마다 새로 만들어진다. <b>알림의 멱등은 이 값에 기대지 않는다</b> — 같은 사건이 재발행되면 다른 {@code eventId}를 달고
 * 오므로, 중복 판정은 이벤트 내용에서 파생한 {@code dedupeKey}가 맡는다(D-48).
 *
 * @param eventType 구현 클래스의 단순 이름. 수신 측이 이것으로 역직렬화 대상을 고른다
 * @param aggregateId 이 이벤트가 속한 집합체 식별자. 추적·집계에 쓰고, 나중에 FIFO 큐로 바꾸면 {@code MessageGroupId}가 될 값이다.
 *     <b>지금은 표준 큐라 순서 보장에 쓰이지 않는다</b>(D-50)
 */
public record EventEnvelope(
        String eventId, String eventType, String aggregateId, OffsetDateTime occurredAt, JsonNode payload) {

    public static String newEventId() {
        return UUID.randomUUID().toString();
    }
}
