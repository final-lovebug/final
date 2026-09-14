package com.ubidict.backend.common.infra.event.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.OffsetDateTime;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * SQS로 이벤트를 발행하면 큐에 무엇이 실려 나가는지 눈으로 확인하는 테스트.
 *
 * <p>브로커를 띄우지 않는다 — {@code SqsTemplate}을 가짜로 두고 <b>발행자가 큐에 넘기려던 값</b>을 가로채 확인한다. Docker도 AWS 자격증명도
 * 필요 없고, 실행하면 실제로 나갈 메시지 본문이 그대로 출력된다.
 *
 * <p>실행: {@code ./gradlew test --tests '*SqsEventPublishTest'}
 */
class SqsEventPublishTest {

    private static final String QUEUE = "ubidict-domain-events";

    @DisplayName("이벤트를 발행하면 봉투에 싸여 큐로 나간다.")
    @Test
    void publish() {
        // given — 가짜 큐와 발행자
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        SqsEventPublisher publisher = new SqsEventPublisher(sqsTemplate, new EventEnvelopeCodec(new ObjectMapper()));
        ReflectionTestUtils.setField(publisher, "queue", QUEUE);

        ReviewRequestRevisedEvent event = new ReviewRequestRevisedEvent(
                1L, ReviewRequestType.DOCUMENT, 10L, 6, OffsetDateTime.parse("2026-09-13T10:00:00Z"));

        // when
        publisher.publish(event);

        // then — 발행자가 큐에 넘기려던 설정을 그대로 재생해 본다
        SqsSendOptions<String> sent = capture(sqsTemplate);

        verify(sent).queue(QUEUE);
        // 표준 큐를 쓰므로 FIFO 전용 파라미터는 붙지 않는다(D-53) — 붙이면 AWS가 InvalidParameterValue로 거절한다
        verify(sent, never()).messageGroupId(any());
        verify(sent, never()).messageDeduplicationId(any());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sent).payload(body.capture());

        System.out.println("큐로 나가는 메시지 >>> " + body.getValue());

        assertThat(body.getValue())
                .contains("\"eventType\":\"ReviewRequestRevisedEvent\"")
                .contains("\"resultVersionNo\":6");
    }

    /**
     * {@code send(Consumer<SqsSendOptions>)}는 람다를 받으므로, 그 람다를 잡아 기록용 가짜에 한 번 실행시킨다.
     */
    @SuppressWarnings("unchecked")
    private SqsSendOptions<String> capture(SqsTemplate sqsTemplate) {
        ArgumentCaptor<Consumer<SqsSendOptions<String>>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(sqsTemplate).send(captor.capture());

        SqsSendOptions<String> options = mock(SqsSendOptions.class, RETURNS_SELF);
        captor.getValue().accept(options);

        return options;
    }
}
