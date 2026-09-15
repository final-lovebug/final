package com.ubidict.backend.support.ai;

import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionCallbackService;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckCallbackService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link FakeLlmWorker}를 큐에 붙인다.
 *
 * <p>{@code IntegrationTestSupport}가 기본으로 들이지 않는다 — 워커가 필요한 것은 {@code app.ai.dispatch.mode=sqs}로 도는 왕복
 * 테스트뿐이고, 다른 모든 통합 테스트는 인프로세스 대역으로 돈다. 기본으로 들이면 {@code @SqsListener}가 모든 컨텍스트에서 큐를 롱폴한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeLlmWorkerConfiguration {

    @Bean
    FakeLlmWorker fakeLlmWorker(
            ObjectMapper objectMapper,
            DraftDictionaryExtractionCallbackService extractionCallbackService,
            DraftDocumentCheckCallbackService checkCallbackService) {
        return new FakeLlmWorker(objectMapper, extractionCallbackService, checkCallbackService);
    }
}
