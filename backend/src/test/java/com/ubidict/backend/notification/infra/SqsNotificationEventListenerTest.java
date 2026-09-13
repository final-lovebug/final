package com.ubidict.backend.notification.infra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import com.ubidict.backend.common.infra.event.sqs.EventEnvelopeCodec;
import com.ubidict.backend.notification.infra.event.SqsNotificationEventListener;
import com.ubidict.backend.notification.service.NotificationEventHandler;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import com.ubidict.backend.workspace.domain.event.WorkspaceDeletedEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * SQS로 들어온 메시지가 올바른 핸들러 메서드에 닿는지 확인한다.
 *
 * <p>브로커 없이 <b>발행 측이 만든 그대로의 문자열</b>을 리스너에 넣어 검증한다 — 큐는 문자열을 옮길 뿐이므로 이 경계까지가 우리 코드의 책임이다.
 */
@ExtendWith(MockitoExtension.class)
class SqsNotificationEventListenerTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.of(2026, 9, 13, 10, 0, 0, 0, ZoneOffset.UTC);

    private final EventEnvelopeCodec codec = new EventEnvelopeCodec(new ObjectMapper());

    @Mock
    private NotificationEventHandler handler;

    @DisplayName("리뷰 요청 생성 메시지는 생성 핸들러로 간다.")
    @Test
    void dispatch_created() {
        // given
        ReviewRequestCreatedEvent event =
                new ReviewRequestCreatedEvent(1L, 42L, ReviewRequestType.DOCUMENT, 10L, 7L, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("판정 제출 메시지는 판정 핸들러로 간다.")
    @Test
    void dispatch_submitted() {
        // given
        ReviewSubmittedEvent event = new ReviewSubmittedEvent(1L, 2L, 3L, ReviewVerdict.APPROVED, 1, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("변경요청 메시지는 변경요청 핸들러로 간다.")
    @Test
    void dispatch_changesRequested() {
        // given
        ReviewRequestChangesRequestedEvent event =
                new ReviewRequestChangesRequestedEvent(1L, ReviewRequestType.DOCUMENT, 10L, 7L, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("반영완료 메시지는 반영완료 핸들러로 간다.")
    @Test
    void dispatch_revised() {
        // given
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(1L, ReviewRequestType.DICTIONARY, 10L, 7, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("취소 메시지는 취소 핸들러로 간다.")
    @Test
    void dispatch_canceled() {
        // given
        ReviewRequestCanceledEvent event =
                new ReviewRequestCanceledEvent(1L, ReviewRequestType.DOCUMENT, 10L, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("구독하지 않는 이벤트는 조용히 넘긴다.")
    @Test
    void skip_unsubscribed() {
        // given — 같은 큐에 다른 도메인의 이벤트가 흐르는 것은 정상이다
        WorkspaceDeletedEvent event = new WorkspaceDeletedEvent(1L, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verifyNoInteractions(handler);
    }

    @DisplayName("망가진 메시지는 거부한다 — 조용히 삼키면 DLQ로도 못 간다.")
    @Test
    void reject_brokenMessage() {
        // when & then
        assertThatThrownBy(() -> listener().on("{not json"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(MessagingErrorCode.MESSAGING_EVENT_DESERIALIZATION_FAILED);
        verifyNoInteractions(handler);
    }

    private SqsNotificationEventListener listener() {
        return new SqsNotificationEventListener(codec, handler);
    }

    private String encode(DomainEvent event) {
        return codec.encode(event, "1", OCCURRED_AT);
    }
}
