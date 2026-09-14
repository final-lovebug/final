package com.ubidict.backend.common.infra.event.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * 큐를 통과하는 봉투가 <b>왕복하는지</b> 확인한다.
 *
 * <p>이 검증이 필요한 이유 — 본문을 {@code DomainEvent} 인터페이스 그대로 실으면 직렬화는 되지만 역직렬화가 「no Creators」로 깨진다. 인메모리
 * 경로는 객체 참조를 그대로 넘기므로 이 결함이 로컬에서 절대 드러나지 않는다. {@code ARCHITECTURE.md}가 「이 규약은 로컬에서 검증되지 않으므로 리뷰에서
 * 확인한다」고 한 자리를 테스트로 대신 막는다.
 */
class EventEnvelopeCodecTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.of(2026, 9, 13, 10, 0, 0, 0, ZoneOffset.UTC);

    private final EventEnvelopeCodec codec = new EventEnvelopeCodec(new ObjectMapper());

    @DisplayName("봉투를 문자열로 만들었다가 같은 이벤트로 되돌린다.")
    @Test
    void roundTrip() {
        // given
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(1L, ReviewRequestType.DOCUMENT, 10L, 6, OCCURRED_AT);

        // when
        EventEnvelope envelope = codec.decode(codec.encode(event, "42", OCCURRED_AT));

        // then
        assertThat(codec.payloadAs(envelope, ReviewRequestRevisedEvent.class)).isEqualTo(event);
    }

    @DisplayName("열거형과 시각이 섞인 이벤트도 값이 그대로 보존된다.")
    @Test
    void roundTrip_withEnumAndTime() {
        // given
        ReviewSubmittedEvent event = new ReviewSubmittedEvent(1L, 2L, 3L, ReviewVerdict.APPROVED, 1, OCCURRED_AT);

        // when
        EventEnvelope envelope = codec.decode(codec.encode(event, "1", OCCURRED_AT));

        // then
        assertThat(codec.payloadAs(envelope, ReviewSubmittedEvent.class)).isEqualTo(event);
    }

    @DisplayName("봉투는 이벤트 종류·메시지 그룹·발생 시각을 함께 싣는다.")
    @Test
    void envelopeMetadata() {
        // given
        ReviewRequestCanceledEvent event =
                new ReviewRequestCanceledEvent(1L, ReviewRequestType.DICTIONARY, 10L, OCCURRED_AT);

        // when
        EventEnvelope envelope = codec.decode(codec.encode(event, "42", OCCURRED_AT));

        // then
        assertThat(envelope.eventType()).isEqualTo("ReviewRequestCanceledEvent");
        assertThat(envelope.aggregateId()).isEqualTo("42");
        assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(envelope.eventId()).isNotBlank();
    }

    @DisplayName("발행할 때마다 이벤트 식별자가 새로 만들어진다.")
    @Test
    void eventIdIsUniquePerPublish() {
        // given
        ReviewRequestCanceledEvent event =
                new ReviewRequestCanceledEvent(1L, ReviewRequestType.DOCUMENT, 10L, OCCURRED_AT);

        // when & then
        assertThat(codec.decode(codec.encode(event, "1", OCCURRED_AT)).eventId())
                .isNotEqualTo(
                        codec.decode(codec.encode(event, "1", OCCURRED_AT)).eventId());
    }

    @DisplayName("워크스페이스 식별자가 있으면 그것을 메시지 그룹으로 삼는다.")
    @Test
    void aggregateId_prefersWorkspaceId() {
        // given
        ReviewRequestCreatedEvent event =
                new ReviewRequestCreatedEvent(1L, 42L, ReviewRequestType.DOCUMENT, 10L, 7L, OCCURRED_AT);

        // when & then
        assertThat(DomainEventMetadata.aggregateId(event)).isEqualTo("42");
    }

    @DisplayName("워크스페이스 식별자가 없으면 첫 식별자로 메시지 그룹을 대신한다.")
    @Test
    void aggregateId_fallsBackToFirstIdentifier() {
        // given — 취소 이벤트에는 workspaceId가 없다
        ReviewRequestCanceledEvent event =
                new ReviewRequestCanceledEvent(7L, ReviewRequestType.DOCUMENT, 10L, OCCURRED_AT);

        // when & then
        assertThat(DomainEventMetadata.aggregateId(event)).isEqualTo("7");
    }

    @DisplayName("이벤트의 발생 시각을 봉투가 그대로 쓴다.")
    @Test
    void occurredAt_comesFromEvent() {
        // given
        ReviewRequestCanceledEvent event =
                new ReviewRequestCanceledEvent(1L, ReviewRequestType.DOCUMENT, 10L, OCCURRED_AT);

        // when & then
        assertThat(DomainEventMetadata.occurredAt(event)).isEqualTo(OCCURRED_AT);
    }

    @DisplayName("시각은 UTC로 정규화되어 돌아온다 — 순간은 보존된다.")
    @Test
    void roundTrip_normalizesOffsetToUtc() {
        // given — 프로덕션은 OffsetDateTime.now()를 쓰므로 한국에서는 +09:00이 실린다
        OffsetDateTime kst = OCCURRED_AT.withOffsetSameInstant(ZoneOffset.ofHours(9));
        ReviewRequestCanceledEvent event = new ReviewRequestCanceledEvent(1L, ReviewRequestType.DOCUMENT, 10L, kst);

        // when
        EventEnvelope envelope = codec.decode(codec.encode(event, "1", kst));
        ReviewRequestCanceledEvent restored = codec.payloadAs(envelope, ReviewRequestCanceledEvent.class);

        // then — 오프셋 표기는 Z로 바뀌지만 가리키는 순간은 같다
        assertThat(restored.occurredAt().toInstant()).isEqualTo(kst.toInstant());
        assertThat(envelope.occurredAt().toInstant()).isEqualTo(kst.toInstant());
    }

    @DisplayName("망가진 메시지는 역직렬화에서 거부된다.")
    @Test
    void decode_rejectsBrokenMessage() {
        // when & then
        assertThatThrownBy(() -> codec.decode("{not json"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(MessagingErrorCode.MESSAGING_EVENT_DESERIALIZATION_FAILED);
    }
}
