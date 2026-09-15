package com.ubidict.backend.common.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.infra.ai.sqs.SqsLlmJobRequestSender;
import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 워커로 나가는 메시지의 계약.
 *
 * <p>브로커를 띄우지 않는다 — {@code SqsTemplate}을 가짜로 두고 <b>발행자가 큐에 넘기려던 값</b>을 가로챈다. 여기서 확인하는 JSON 모양은
 * {@code docs/AI_CONTRACT.md}의 예시와 같아야 한다. 워커를 구현하는 쪽이 읽는 것이 그 문서이므로, 어긋나면 배포하고 나서야 드러난다.
 */
class SqsLlmJobRequestSenderTest {

    private static final String QUEUE = "lovebug-llm-request";
    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";

    @DisplayName("용어 추출 요청은 식별자와 실행 모드만 싣고 전용 큐로 나간다.")
    @Test
    void send_termExtraction() {
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        SqsLlmJobRequestSender sender = sender(sqsTemplate);

        sender.send(LlmJobRequest.termExtraction(REQUEST_ID, 30L, 1L, null, List.of(10L, 20L), LlmMode.REAL));

        SqsSendOptions<String> sent = capture(sqsTemplate);
        verify(sent).queue(QUEUE);
        // 표준 큐라 FIFO 전용 파라미터는 붙지 않는다(D-53).
        verify(sent, never()).messageGroupId(any());
        verify(sent, never()).messageDeduplicationId(any());

        String body = body(sent);
        System.out.println("LLM 요청 큐로 나가는 메시지 >>> " + body);

        assertThat(body)
                .contains("\"contractVersion\":1")
                .contains("\"requestId\":\"" + REQUEST_ID + "\"")
                .contains("\"jobType\":\"TERM_EXTRACTION\"")
                .contains("\"jobId\":30")
                .contains("\"workspaceId\":1")
                .contains("\"sourceDocumentIds\":[10,20]")
                .contains("\"mode\":\"REAL\"");
        // 본문은 싣지 않는다(D-69) — 워커가 DB에서 직접 읽는다.
        assertThat(body).doesNotContain("\"body\"");
    }

    @DisplayName("문서 대조 요청은 워커가 읽어야 할 문서 버전을 함께 싣는다.")
    @Test
    void send_documentCheck() {
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        SqsLlmJobRequestSender sender = sender(sqsTemplate);

        sender.send(LlmJobRequest.documentCheck(REQUEST_ID, 40L, 20L, 10L, 3, LlmMode.MOCK));

        String body = body(capture(sqsTemplate));
        System.out.println("LLM 요청 큐로 나가는 메시지 >>> " + body);

        assertThat(body)
                .contains("\"jobType\":\"DOCUMENT_CHECK\"")
                .contains("\"jobId\":40")
                .contains("\"documentId\":10")
                .contains("\"documentVersionNo\":3")
                .contains("\"mode\":\"MOCK\"");
    }

    @DisplayName("발행 어댑터는 app.ai.dispatch.mode 로 배타 선택된다. 도메인 이벤트 축과 무관하다.")
    @Test
    void adapterSelection() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(
                        SqsLlmJobRequestSender.class, InProcessLlmJobRequestSender.class, SqsStubConfig.class)
                .withPropertyValues("app.messaging.sqs.llm-request-queue=" + QUEUE);

        runner.withPropertyValues("app.ai.dispatch.mode=sqs").run(context -> {
            assertThat(context).hasSingleBean(LlmJobRequestSender.class);
            assertThat(context).hasSingleBean(SqsLlmJobRequestSender.class);
        });
        runner.run(context -> {
            assertThat(context).hasSingleBean(LlmJobRequestSender.class);
            assertThat(context).doesNotHaveBean(SqsLlmJobRequestSender.class);
        });
        // 도메인 이벤트 버스를 SQS로 돌려도 LLM 경로는 이 축만 따른다.
        runner.withPropertyValues("app.messaging.mode=sqs")
                .run(context -> assertThat(context).doesNotHaveBean(SqsLlmJobRequestSender.class));
    }

    private SqsLlmJobRequestSender sender(SqsTemplate sqsTemplate) {
        SqsLlmJobRequestSender sender = new SqsLlmJobRequestSender(sqsTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(sender, "queue", QUEUE);
        return sender;
    }

    /** {@code send(Consumer<SqsSendOptions>)}는 람다를 받으므로, 그 람다를 잡아 기록용 가짜에 한 번 실행시킨다. */
    @SuppressWarnings("unchecked")
    private SqsSendOptions<String> capture(SqsTemplate sqsTemplate) {
        ArgumentCaptor<Consumer<SqsSendOptions<String>>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(sqsTemplate).send(captor.capture());

        SqsSendOptions<String> options = mock(SqsSendOptions.class, RETURNS_SELF);
        captor.getValue().accept(options);

        return options;
    }

    private String body(SqsSendOptions<String> sent) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sent).payload(body.capture());
        return body.getValue();
    }

    @Configuration(proxyBeanMethods = false)
    static class SqsStubConfig {

        @Bean
        SqsTemplate sqsTemplate() {
            return mock(SqsTemplate.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        org.springframework.context.ApplicationEventPublisher applicationEventPublisher() {
            return mock(org.springframework.context.ApplicationEventPublisher.class);
        }
    }
}
