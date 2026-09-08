package com.ubidict.backend.common.infra.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

class InMemoryEventPublisherTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(InMemoryEventPublisher.class);

    @DisplayName("메시징 모드를 지정하지 않으면 인메모리 발행자가 등록된다.")
    @Test
    void register() {
        // when & then
        contextRunner.run(context -> assertThat(context).hasSingleBean(EventPublisher.class));
    }

    @DisplayName("메시징 모드가 인메모리가 아니면 인메모리 발행자가 등록되지 않는다.")
    @Test
    void register_otherMode() {
        // when & then
        contextRunner.withPropertyValues("app.messaging.mode=sqs").run(context -> assertThat(context)
                .doesNotHaveBean(EventPublisher.class));
    }

    @DisplayName("이벤트를 발행하면 리스너가 수신한다.")
    @Test
    void publish() {
        contextRunner.withUserConfiguration(TestListenerConfig.class).run(context -> {
            // given
            EventPublisher eventPublisher = context.getBean(EventPublisher.class);
            TestListener listener = context.getBean(TestListener.class);
            TestEvent event = new TestEvent("document-1");

            // when
            eventPublisher.publish(event);

            // then
            assertThat(listener.received()).containsExactly(event);
        });
    }

    record TestEvent(String documentId) implements DomainEvent {}

    static class TestListener {

        private final List<DomainEvent> received = new ArrayList<>();

        @EventListener
        void on(TestEvent event) {
            received.add(event);
        }

        List<DomainEvent> received() {
            return received;
        }
    }

    @Configuration
    static class TestListenerConfig {

        @Bean
        TestListener testListener() {
            return new TestListener();
        }
    }
}
