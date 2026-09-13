package com.ubidict.backend.common.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.infra.event.sqs.EventEnvelopeCodec;
import com.ubidict.backend.common.infra.event.sqs.SqsEventPublisher;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * 두 발행 어댑터가 {@code app.messaging.mode} 하나로 배타 선택되는지 확인한다(D-24·D-49).
 *
 * <p>{@code InMemoryEventPublisher}가 package-private이라 이 테스트는 그 패키지에 있어야 한다. 봉투 왕복 검증은
 * {@code sqs} 패키지의 {@code EventEnvelopeCodecTest}가 맡는다.
 */
class SqsEventPublisherTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SqsEventPublisher.class, InMemoryEventPublisher.class, SqsStubConfig.class);

    @DisplayName("메시징 모드가 sqs면 SQS 발행자만 등록된다.")
    @Test
    void register_sqsMode() {
        contextRunner
                .withPropertyValues("app.messaging.mode=sqs", "app.messaging.sqs.queue=test-queue")
                .run(context -> {
                    assertThat(context).hasSingleBean(EventPublisher.class);
                    assertThat(context).hasSingleBean(SqsEventPublisher.class);
                    assertThat(context).doesNotHaveBean(InMemoryEventPublisher.class);
                });
    }

    @DisplayName("메시징 모드를 지정하지 않으면 인메모리 발행자만 등록된다.")
    @Test
    void register_defaultMode() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EventPublisher.class);
            assertThat(context).doesNotHaveBean(SqsEventPublisher.class);
        });
    }

    @Configuration
    static class SqsStubConfig {

        @Bean
        SqsTemplate sqsTemplate() {
            return mock(SqsTemplate.class);
        }

        @Bean
        EventEnvelopeCodec eventEnvelopeCodec() {
            return new EventEnvelopeCodec(new ObjectMapper());
        }
    }
}
