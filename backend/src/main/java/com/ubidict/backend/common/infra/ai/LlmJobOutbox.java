package com.ubidict.backend.common.infra.ai;

import com.ubidict.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmJobOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private LlmJobType jobType;

    @Column(nullable = false, updatable = false)
    private Long jobId;

    @Column(nullable = false, length = 36, updatable = false)
    private String requestId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LlmJobOutboxStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(length = 36)
    private String leaseToken;

    private OffsetDateTime leaseExpiresAt;

    @Column(length = 1000)
    private String lastError;

    private OffsetDateTime publishedAt;

    private LlmJobOutbox(LlmJobRequest request, String payload) {
        this.jobType = request.jobType();
        this.jobId = request.jobId();
        this.requestId = request.requestId();
        this.payload = payload;
        this.status = LlmJobOutboxStatus.PENDING;
        this.nextAttemptAt = OffsetDateTime.now();
    }

    public static LlmJobOutbox create(LlmJobRequest request, String payload) {
        return new LlmJobOutbox(request, payload);
    }

    public boolean isDispatchable(OffsetDateTime now) {
        return (status == LlmJobOutboxStatus.PENDING && !nextAttemptAt.isAfter(now))
                || (status == LlmJobOutboxStatus.PROCESSING && leaseExpiresAt != null && leaseExpiresAt.isBefore(now));
    }

    public void claim(String token, OffsetDateTime now, Duration leaseDuration) {
        if (!isDispatchable(now)) return;
        status = LlmJobOutboxStatus.PROCESSING;
        leaseToken = token;
        leaseExpiresAt = now.plus(leaseDuration);
    }

    public boolean hasLease(String token) {
        return status == LlmJobOutboxStatus.PROCESSING && token.equals(leaseToken);
    }

    public void publish(String token) {
        if (!hasLease(token)) return;
        status = LlmJobOutboxStatus.PUBLISHED;
        publishedAt = OffsetDateTime.now();
        leaseToken = null;
        leaseExpiresAt = null;
        lastError = null;
    }

    public void reschedule(String token, String error, Duration delay) {
        if (!hasLease(token)) return;
        status = LlmJobOutboxStatus.PENDING;
        attemptCount++;
        nextAttemptAt = OffsetDateTime.now().plus(delay);
        leaseToken = null;
        leaseExpiresAt = null;
        String normalized = error == null || error.isBlank() ? "AI 작업 요청 발행 실패" : error.strip();
        lastError = normalized.substring(0, Math.min(normalized.length(), 1000));
    }

    public void cancel() {
        if (status == LlmJobOutboxStatus.PENDING || status == LlmJobOutboxStatus.PROCESSING) {
            status = LlmJobOutboxStatus.CANCELLED;
            leaseToken = null;
            leaseExpiresAt = null;
        }
    }
}
