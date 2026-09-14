package com.ubidict.backend.revisionlog.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.infra.event.sqs.EventEnvelopeCodec;
import com.ubidict.backend.revisionlog.service.RevisionLogEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * 두 수신 어댑터가 {@code app.messaging.mode} 하나로 배타 선택되는지 확인한다(D-24).
 *
 * <p><b>{@code IntegrationTestSupport}를 쓰지 않는다.</b> {@code mode=sqs}로 애플리케이션을 통째로 띄우면
 * spring-cloud-aws가 {@code SqsAsyncClient}를 만들려 들어 자격증명과 네트워크를 요구한다. 배타 선택은 {@code @ConditionalOnProperty}
 * 하나로 결정되므로 컨텍스트 러너가 그 결정을 정확히 같은 조건으로 검증한다 — 발행 측 {@code SqsEventPublisherTest}가 같은 방식이다.
 *
 * <p>어느 어댑터가 뜨든 <b>같은 {@link RevisionLogEventHandler}에 위임한다</b>는 것이 여기서 드러난다. 두 어댑터가 그 빈을 주입받는 것 외에
 * 하는 일이 없어야 로직이 한 벌로 유지된다.
 */
class RevisionLogEventWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    SqsRevisionLogEventListener.class, InMemoryRevisionLogEventListener.class, StubConfig.class);

    @DisplayName("메시징 모드가 sqs면 SQS 수신 어댑터만 등록된다.")
    @Test
    void register_sqsMode() {
        contextRunner
                .withPropertyValues("app.messaging.mode=sqs", "app.messaging.sqs.queue=test-queue")
                .run(context -> {
                    assertThat(context).hasSingleBean(SqsRevisionLogEventListener.class);
                    assertThat(context).doesNotHaveBean(InMemoryRevisionLogEventListener.class);
                });
    }

    @DisplayName("메시징 모드가 in-memory면 인메모리 수신 어댑터만 등록된다.")
    @Test
    void register_inMemoryMode() {
        contextRunner.withPropertyValues("app.messaging.mode=in-memory").run(context -> {
            assertThat(context).hasSingleBean(InMemoryRevisionLogEventListener.class);
            assertThat(context).doesNotHaveBean(SqsRevisionLogEventListener.class);
        });
    }

    @DisplayName("메시징 모드를 지정하지 않으면 인메모리 수신 어댑터만 등록된다.")
    @Test
    void register_defaultMode() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryRevisionLogEventListener.class);
            assertThat(context).doesNotHaveBean(SqsRevisionLogEventListener.class);
        });
    }

    @Configuration
    static class StubConfig {

        @Bean
        EventEnvelopeCodec eventEnvelopeCodec() {
            return new EventEnvelopeCodec(new ObjectMapper());
        }

        @Bean
        RevisionLogEventHandler revisionLogEventHandler() {
            return mock(RevisionLogEventHandler.class);
        }
    }
}
