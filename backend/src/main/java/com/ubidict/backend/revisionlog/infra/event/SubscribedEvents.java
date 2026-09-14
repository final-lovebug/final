package com.ubidict.backend.revisionlog.infra.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 개정 이력이 구독하는 이벤트와 그 이름의 대응표.
 *
 * <p>큐에는 7개 도메인의 이벤트가 모두 흐르지만 개정 이력이 반응하는 것은 셋뿐이다. <b>무엇을 구독하는지는 소비 도메인이 안다</b> — {@code common}의 봉투는
 * 본문을 해석하지 않은 채 싣고 이름만 알리며, 어느 record로 되돌릴지는 여기서 정한다.
 *
 * <p><b>{@code ReviewRequestRevisedEvent}는 알림도 구독한다.</b> 같은 메시지를 두 도메인이 각자 받아 각자 판단하는 것이 정상이다 — 표는 도메인마다
 * 따로 있고 서로를 모른다.
 *
 * <p>이름을 단순 클래스명으로 두는 것은 발행 측 {@code EventEnvelopeCodec}과 같은 규칙이다. <b>이벤트 record의 클래스명을 바꾸면 이 표도 함께 바뀌어야
 * 한다</b> — 큐에 이미 들어간 옛 이름의 메시지는 구독되지 않고 넘어간다.
 */
final class SubscribedEvents {

    private static final Map<String, Class<? extends DomainEvent>> BY_TYPE = Stream.of(
                    DictionaryRevisedEvent.class, DocumentEditedEvent.class, ReviewRequestRevisedEvent.class)
            .collect(Collectors.toUnmodifiableMap(Class::getSimpleName, type -> type));

    private SubscribedEvents() {}

    /**
     * 구독하지 않는 이벤트면 빈 Optional. 예외가 아니다 — 큐에 다른 도메인의 이벤트가 흐르는 것은 정상이다.
     */
    static Optional<Class<? extends DomainEvent>> find(String eventType) {
        return Optional.ofNullable(BY_TYPE.get(eventType));
    }
}
