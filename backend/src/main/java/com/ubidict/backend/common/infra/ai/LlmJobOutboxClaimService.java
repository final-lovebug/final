package com.ubidict.backend.common.infra.ai;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class LlmJobOutboxClaimService {

    private static final int BATCH_SIZE = 20;
    private static final Duration LEASE_DURATION = Duration.ofSeconds(30);

    private final LlmJobOutboxRepository repository;

    @Transactional
    public List<ClaimedOutbox> claimDispatchable() {
        OffsetDateTime now = OffsetDateTime.now();
        return repository.findDispatchableForUpdate(now, PageRequest.of(0, BATCH_SIZE)).stream()
                .map(outbox -> {
                    String leaseToken = UUID.randomUUID().toString();
                    outbox.claim(leaseToken, now, LEASE_DURATION);
                    return new ClaimedOutbox(outbox.getId(), leaseToken, outbox.getPayload());
                })
                .toList();
    }

    @Transactional
    public void publish(Long outboxId, String leaseToken) {
        repository.findById(outboxId).ifPresent(outbox -> outbox.publish(leaseToken));
    }

    @Transactional
    public void reschedule(Long outboxId, String leaseToken, Exception exception) {
        repository
                .findById(outboxId)
                .ifPresent(outbox -> outbox.reschedule(
                        leaseToken, exception.getMessage(), retryDelay(outbox.getAttemptCount() + 1)));
    }

    @Transactional
    public void cancel(Long outboxId) {
        repository.findById(outboxId).ifPresent(LlmJobOutbox::cancel);
    }

    private Duration retryDelay(int attempt) {
        long seconds = Math.min(1L << Math.min(attempt - 1, 6), 60L);
        return Duration.ofSeconds(seconds);
    }

    record ClaimedOutbox(Long id, String leaseToken, String payload) {}
}
