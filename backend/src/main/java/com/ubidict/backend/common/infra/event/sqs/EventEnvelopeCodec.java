package com.ubidict.backend.common.infra.event.sqs;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 도메인 이벤트를 큐에 실을 문자열로 바꾸고 되돌린다.
 *
 * <p><b>메시지를 문자열로 주고받는 이유</b> — spring-cloud-aws의 기본 메시지 컨버터가 어떤 {@code ObjectMapper}를 쓰는지에 기대지 않기
 * 위해서다. 직렬화를 여기서 직접 하면 애플리케이션의 {@code ObjectMapper}가 확실히 쓰이고, 발행·수신 양쪽이 같은 규칙을 따른다.
 *
 * <p>{@code common}은 어떤 이벤트가 있는지 모른다. {@link #payloadAs}에 되돌릴 타입을 알려주는 것은 그 이벤트를 구독하는 도메인의 몫이다.
 *
 * <p>SQS 모드일 때만 뜬다. 인메모리 경로는 객체 참조를 그대로 넘기므로 직렬화가 필요 없다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "sqs")
public class EventEnvelopeCodec {

    private final ObjectMapper objectMapper;

    public String encode(DomainEvent event, String aggregateId, OffsetDateTime occurredAt) {
        EventEnvelope envelope = new EventEnvelope(
                EventEnvelope.newEventId(),
                event.getClass().getSimpleName(),
                aggregateId,
                occurredAt,
                objectMapper.valueToTree(event));

        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JacksonException exception) {
            throw new BusinessException(
                    MessagingErrorCode.MESSAGING_EVENT_SERIALIZATION_FAILED,
                    "이벤트 직렬화 실패. event=" + event.getClass().getName(),
                    exception);
        }
    }

    public EventEnvelope decode(String message) {
        try {
            return objectMapper.readValue(message, EventEnvelope.class);
        } catch (JacksonException exception) {
            throw new BusinessException(
                    MessagingErrorCode.MESSAGING_EVENT_DESERIALIZATION_FAILED, "메시지 역직렬화 실패.", exception);
        }
    }

    /**
     * 봉투 안의 본문을 구독 도메인이 아는 record로 되돌린다.
     */
    public <T extends DomainEvent> T payloadAs(EventEnvelope envelope, Class<T> type) {
        try {
            return objectMapper.treeToValue(envelope.payload(), type);
        } catch (JacksonException exception) {
            throw new BusinessException(
                    MessagingErrorCode.MESSAGING_EVENT_DESERIALIZATION_FAILED,
                    "이벤트 본문 역직렬화 실패. eventType=" + envelope.eventType(),
                    exception);
        }
    }
}
