package com.ubidict.backend.common.infra.event.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import tools.jackson.databind.ObjectMapper;

/**
 * <b>진짜 AWS SQS로 보내고 다시 받아 본다.</b>
 *
 * <p>기본적으로 실행되지 않는다 — {@code SQS_IT_QUEUE}가 있을 때만 돈다. {@code ./gradlew check}는 이 테스트를 조용히 건너뛰므로
 * CI가 AWS를 치거나 요금이 나가는 일이 없다.
 *
 * <h2>실행 방법</h2>
 *
 * <pre>{@code
 * # 1) 큐를 만든다 (한 번만). 표준 큐다.
 * aws sqs create-queue --queue-name ubidict-domain-events --region ap-northeast-2
 *
 * # 2) 자격증명과 큐 이름을 주고 실행한다
 * SQS_IT_QUEUE=ubidict-domain-events \
 * AWS_REGION=ap-northeast-2 \
 * AWS_ACCESS_KEY_ID=... \
 * AWS_SECRET_ACCESS_KEY=... \
 * ./gradlew test --tests '*RealSqsRoundTripIT' --rerun-tasks -i
 * }</pre>
 *
 * <p>{@code AWS_ACCESS_KEY_ID}·{@code AWS_SECRET_ACCESS_KEY} 대신 {@code ~/.aws/credentials}나 SSO를 써도 된다 —
 * SDK 기본 자격증명 체인을 그대로 쓴다. 필요한 IAM 권한은 {@code sqs:SendMessage}·{@code sqs:ReceiveMessage}·
 * {@code sqs:DeleteMessage}·{@code sqs:GetQueueUrl} 넷이다.
 *
 * <h2>무엇을 증명하는가</h2>
 *
 * <p>가짜 큐를 쓰는 단위 테스트가 덮지 못하는 것 — <b>직렬화한 메시지가 AWS를 왕복해 같은 이벤트로 되돌아오는지</b>다. 역직렬화 결함은 수신 측에서만
 * 드러나므로 이 경로를 한 번은 진짜로 밟아 봐야 한다.
 *
 * <p><b>메시지를 소비한다.</b> 받은 메시지는 큐에서 지워진다. 운영 큐를 가리키지 않도록 주의한다.
 */
@Disabled("SQS 외부 왕복은 테스트 범위에서 제외하고 인메모리 이벤트 테스트로 검증한다.")
class RealSqsRoundTripITest {

    private static final String QUEUE = System.getenv("SQS_IT_QUEUE");
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(20);

    @DisplayName("이벤트를 실제 SQS로 보내고 다시 받아 같은 값으로 되돌린다.")
    @Test
    void roundTrip() {
        // given
        SqsAsyncClient client = SqsAsyncClient.builder().region(region()).build();
        SqsTemplate sqsTemplate = SqsTemplate.builder().sqsAsyncClient(client).build();
        EventEnvelopeCodec codec = new EventEnvelopeCodec(new ObjectMapper());

        SqsEventPublisher publisher = new SqsEventPublisher(sqsTemplate, codec);
        ReflectionTestUtils.setField(publisher, "queue", QUEUE);

        // 이 실행만의 값을 심어 큐에 남아 있던 옛 메시지와 구분한다
        int resultVersionNo = (int) (System.currentTimeMillis() % 100_000);
        ReviewRequestRevisedEvent sentEvent = new ReviewRequestRevisedEvent(
                1L,
                ReviewRequestType.DOCUMENT,
                10L,
                resultVersionNo,
                OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        // when — 진짜로 보낸다
        publisher.publish(sentEvent);
        System.out.println("보냄 >>> queue=" + QUEUE + ", resultVersionNo=" + resultVersionNo);

        // then — 내가 보낸 그 메시지가 돌아올 때까지 받는다
        ReviewRequestRevisedEvent received = receiveMine(sqsTemplate, codec, resultVersionNo);

        System.out.println("받음 >>> " + received);

        // 봉투는 시각을 UTC로 정규화한다 — +09:00으로 보낸 값이 Z로 돌아온다. 같은 순간이므로 정상이다.
        // OffsetDateTime.equals는 순간뿐 아니라 오프셋까지 비교하므로 순간으로 견준다.
        assertThat(received.occurredAt().toInstant())
                .isEqualTo(sentEvent.occurredAt().toInstant());
        assertThat(received)
                .usingRecursiveComparison()
                .ignoringFields("occurredAt")
                .isEqualTo(sentEvent);
    }

    /**
     * 큐에 남아 있던 다른 메시지는 흘려보내고 이번에 보낸 것을 찾는다. 표준 큐는 순서를 보장하지 않으므로 첫 메시지가 내 것이라는 보장이 없다.
     */
    private ReviewRequestRevisedEvent receiveMine(
            SqsTemplate sqsTemplate, EventEnvelopeCodec codec, int resultVersionNo) {
        long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            Optional<Message<String>> message =
                    sqsTemplate.receive(from -> from.queue(QUEUE).pollTimeout(Duration.ofSeconds(5)), String.class);
            if (message.isEmpty()) {
                continue;
            }

            EventEnvelope envelope = codec.decode(message.get().getPayload());
            if (!"ReviewRequestRevisedEvent".equals(envelope.eventType())) {
                continue;
            }

            ReviewRequestRevisedEvent event = codec.payloadAs(envelope, ReviewRequestRevisedEvent.class);
            if (event.resultVersionNo() == resultVersionNo) {
                return event;
            }
        }

        throw new AssertionError("보낸 메시지가 " + POLL_TIMEOUT.toSeconds() + "초 안에 돌아오지 않았다. queue=" + QUEUE);
    }

    /**
     * {@code AWS_REGION}이 없으면 SDK 기본 체인에 맡긴다.
     */
    private static Region region() {
        String region = System.getenv("AWS_REGION");

        return region == null || region.isBlank() ? Region.AP_NORTHEAST_2 : Region.of(region);
    }
}
