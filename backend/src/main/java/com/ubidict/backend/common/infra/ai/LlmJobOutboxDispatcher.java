package com.ubidict.backend.common.infra.ai;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** DB에 커밋된 LLM 요청을 at-least-once로 전송한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmJobOutboxDispatcher {

    private final LlmJobOutboxClaimService claimService;
    private final List<LlmJobDispatchLifecycle> lifecycles;
    private final LlmJobRequestSender sender;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.ai.outbox.sweep-interval}")
    public void dispatch() {
        claimService.claimDispatchable().forEach(this::dispatchOne);
    }

    private void dispatchOne(LlmJobOutboxClaimService.ClaimedOutbox claimed) {
        try {
            LlmJobRequest request = objectMapper.readValue(claimed.payload(), LlmJobRequest.class);
            LlmJobDispatchLifecycle lifecycle = lifecycles.stream()
                    .filter(candidate -> candidate.supports(request.jobType()))
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException("LLM 작업 lifecycle이 없습니다. jobType=" + request.jobType()));
            if (!lifecycle.prepare(request)) {
                claimService.cancel(claimed.id());
                return;
            }
            sender.send(request);
            claimService.publish(claimed.id(), claimed.leaseToken());
        } catch (Exception exception) {
            log.error(
                    "[LlmJobOutboxDispatcher.dispatchOne] Failed to dispatch LLM outbox. outboxId={}",
                    claimed.id(),
                    exception);
            claimService.reschedule(claimed.id(), claimed.leaseToken(), exception);
        }
    }
}
