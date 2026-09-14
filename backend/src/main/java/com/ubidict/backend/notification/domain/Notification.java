package com.ubidict.backend.notification.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.notification.exception.NotificationErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 도메인 사건 하나를 회원 한 명에게 알린 기록.
 *
 * <p><b>이 엔티티는 HTTP로 만들어지지 않는다.</b> 리뷰 요청·판정·반영 이벤트를 받은 핸들러만 만든다. 그래서 생성 경로에 요청자 검증이 없다.
 *
 * <p>수신자 한 명당 한 행이다. 한 사건이 다섯 명에게 가면 다섯 행이 생긴다 — 읽음 여부가 수신자마다 다르므로 행을 공유할 수 없다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    public static final int TITLE_MAX_LENGTH = 255;
    public static final int MESSAGE_MAX_LENGTH = 500;
    public static final int DEDUPE_KEY_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long recipientId;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private NotificationTargetType targetType;

    @Column(nullable = false, updatable = false)
    private Long targetId;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    /**
     * 같은 사건으로 알림이 두 번 생기지 않게 하는 키. {@code (recipientId, dedupeKey)}에 유니크 제약이 걸려 있다(D-43).
     *
     * <p>이벤트 내용에서 결정론적으로 파생하므로 같은 이벤트가 재수신되면 같은 값이 나오고 두 번째 insert가 DB에서 튕긴다. SQS는 at-least-once라
     * 재수신이 정상 동작이며, 이 제약이 {@code NFR-NTF-002}의 「재수신 시 재발송 금지」를 지킨다.
     */
    @Column(nullable = false, updatable = false, length = DEDUPE_KEY_MAX_LENGTH)
    private String dedupeKey;

    /**
     * 읽었는지 여부. 컬럼명이 {@code is_read}인 것은 {@code read}가 MySQL 예약어이기 때문이다.
     */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    private OffsetDateTime readAt;

    /**
     * 알림을 유발한 행위자. 시스템이 발행했으면 null이다.
     */
    private Long createdBy;

    private Notification(
            Long recipientId,
            Long workspaceId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String title,
            String message,
            String dedupeKey,
            Long createdBy) {
        this.recipientId = requireRecipient(recipientId);
        this.workspaceId = workspaceId;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = normalize(title, TITLE_MAX_LENGTH);
        this.message = normalize(message, MESSAGE_MAX_LENGTH);
        this.dedupeKey = requireDedupeKey(dedupeKey);
        this.read = false;
        this.readAt = null;
        this.createdBy = createdBy;
    }

    public static Notification create(
            Long recipientId,
            Long workspaceId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String title,
            String message,
            String dedupeKey,
            Long createdBy) {
        return new Notification(
                recipientId, workspaceId, type, targetType, targetId, title, message, dedupeKey, createdBy);
    }

    /**
     * 읽음으로 표시한다. 이미 읽었으면 아무것도 하지 않는다 — 읽은 시각은 <b>처음</b> 읽은 순간이어야 한다.
     *
     * <p>{@code read}와 {@code readAt}을 여기서만 함께 바꾼다. 둘을 따로 세팅하는 경로를 두지 않는 이유는, 어긋나는 순간 목록의 읽음 표시와 읽은 시각이
     * 서로 다른 말을 하게 되기 때문이다.
     */
    public void markRead() {
        if (read) {
            return;
        }

        read = true;
        readAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * 이 알림을 이 회원이 볼 수 있는지. 수신자 본인만 볼 수 있다.
     */
    public boolean isRecipient(Long memberId) {
        return recipientId.equals(memberId);
    }

    private static Long requireRecipient(Long recipientId) {
        if (recipientId == null) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_RECIPIENT_REQUIRED);
        }

        return recipientId;
    }

    private static String normalize(String value, int maxLength) {
        if (value == null) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_INVALID_CONTENT);
        }

        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_INVALID_CONTENT);
        }

        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private static String requireDedupeKey(String dedupeKey) {
        if (dedupeKey == null || dedupeKey.isBlank()) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_INVALID_DEDUPE_KEY);
        }
        if (dedupeKey.length() > DEDUPE_KEY_MAX_LENGTH) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_INVALID_DEDUPE_KEY);
        }

        return dedupeKey;
    }
}
