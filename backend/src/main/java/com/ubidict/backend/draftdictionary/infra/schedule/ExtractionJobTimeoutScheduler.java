package com.ubidict.backend.draftdictionary.infra.schedule;

import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 고아 추출 작업 회수를 주기적으로 돌린다(D-77). 스케줄 트리거만 담당하고 판단은 서비스가 한다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.timeout.enabled", havingValue = "true", matchIfMissing = true)
public class ExtractionJobTimeoutScheduler {

    private final DraftDictionaryExtractionTimeoutService timeoutService;

    @Scheduled(fixedDelayString = "${app.ai.timeout.sweep-interval}")
    public void sweep() {
        try {
            timeoutService.expireStale();
        } catch (Exception exception) {
            log.error("[ExtractionJobTimeoutScheduler.sweep] Failed to sweep stale extraction jobs.", exception);
        }
    }
}
