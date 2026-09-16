package com.ubidict.backend.common.infra.ai;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LlmJobOutboxRepository extends JpaRepository<LlmJobOutbox, Long> {

    Optional<LlmJobOutbox> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox from LlmJobOutbox outbox
            where (outbox.status = com.ubidict.backend.common.infra.ai.LlmJobOutboxStatus.PENDING
                       and outbox.nextAttemptAt <= :now)
               or (outbox.status = com.ubidict.backend.common.infra.ai.LlmJobOutboxStatus.PROCESSING
                       and outbox.leaseExpiresAt < :now)
            order by outbox.id
            """)
    List<LlmJobOutbox> findDispatchableForUpdate(@Param("now") OffsetDateTime now, Pageable pageable);
}
