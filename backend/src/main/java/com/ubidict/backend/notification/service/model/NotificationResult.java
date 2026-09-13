package com.ubidict.backend.notification.service.model;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.domain.NotificationTargetType;
import com.ubidict.backend.notification.domain.NotificationType;
import java.time.OffsetDateTime;

/**
 * service가 presentation에 돌려주는 모델. 도메인 모델(=엔티티)을 담지 않는다.
 */
public record NotificationResult(
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

    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getId(),
                notification.getWorkspaceId(),
                notification.getType(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
