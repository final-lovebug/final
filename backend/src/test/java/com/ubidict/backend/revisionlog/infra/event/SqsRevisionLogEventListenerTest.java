package com.ubidict.backend.revisionlog.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import com.ubidict.backend.common.infra.event.sqs.EventEnvelopeCodec;
import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.revisionlog.service.RevisionLogEventHandler;
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
 * <p>브로커 없이 <b>발행 측이 만든 그대로의 문자열</b>을 리스너에 넣어 검증한다 — 큐는 문자열을 옮길 뿐이므로 이 경계까지가 우리 코드의 책임이다. LocalStack을
 * 두지 않는 판단은 Notification과 같다.
 */
@ExtendWith(MockitoExtension.class)
class SqsRevisionLogEventListenerTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.of(2026, 9, 14, 10, 0, 0, 0, ZoneOffset.UTC);

    private final EventEnvelopeCodec codec = new EventEnvelopeCodec(new ObjectMapper());

    @Mock
    private RevisionLogEventHandler handler;

    @DisplayName("사전집 발행 메시지는 사전집 핸들러로 간다.")
    @Test
    void dispatch_dictionaryRevised() {
        // given
        DictionaryRevisedEvent event = new DictionaryRevisedEvent(42L, 10L, 7, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("문서 직접 편집 메시지는 문서 편집 핸들러로 간다.")
    @Test
    void dispatch_documentEdited() {
        // given
        DocumentEditedEvent event = new DocumentEditedEvent(20L, 42L, 2, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("리뷰 반영 메시지는 반영 핸들러로 간다.")
    @Test
    void dispatch_reviewRequestRevised() {
        // given
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(1L, ReviewRequestType.DOCUMENT, 30L, 3, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("사전집 반영 메시지도 핸들러까지 보낸다 — 무시 판단은 수신 기술이 하지 않는다.")
    @Test
    void dispatch_dictionaryTypeRevised() {
        // given — type == DICTIONARY를 걸러내는 것은 핸들러의 도메인 규칙이다
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(1L, ReviewRequestType.DICTIONARY, 30L, 7, OCCURRED_AT);

        // when
        listener().on(encode(event));

        // then
        verify(handler).handle(event);
    }

    @DisplayName("구독하지 않는 이벤트는 조용히 넘긴다.")
    @Test
    void skip_unsubscribed() {
        // given — 같은 큐에 다른 도메인의 이벤트가 흐르는 것은 정상이다
        WorkspaceDeletedEvent event = new WorkspaceDeletedEvent(42L, OCCURRED_AT);

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
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(MessagingErrorCode.MESSAGING_EVENT_DESERIALIZATION_FAILED));
        verifyNoInteractions(handler);
    }

    private SqsRevisionLogEventListener listener() {
        return new SqsRevisionLogEventListener(codec, handler);
    }

    private String encode(DomainEvent event) {
        return codec.encode(event, "42", OCCURRED_AT);
    }
}
