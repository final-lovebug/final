package com.ubidict.backend.notification.presentation.dto;

import com.ubidict.backend.notification.domain.NotificationTargetType;
import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.notification.service.model.NotificationResult;
import java.time.OffsetDateTime;

/**
 * 버튼 문구와 이동 경로는 담지 않는다(D-49). {@code type}·{@code targetType}·{@code targetId}에서 화면이 파생한다.
 */
public record NotificationResponse(
        Long notificationId,
        Long workspaceId,
        NotificationType type,
        NotificationTargetType targetType,
        Long targetId,
        String title,
        String message,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt) {

    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.notificationId(),
                result.workspaceId(),
                result.type(),
                result.targetType(),
                result.targetId(),
                result.title(),
                result.message(),
                result.read(),
                result.readAt(),
                result.createdAt());
    }
}
