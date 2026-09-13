package com.ubidict.backend.notification.fixture;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.domain.NotificationTargetType;
import com.ubidict.backend.notification.domain.NotificationType;
import org.springframework.test.util.ReflectionTestUtils;

public class NotificationFixture {

    public static NotificationBuilder notification() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {

        private Long id;
        private Long recipientId = 1L;
        private Long workspaceId = 1L;
        private NotificationType type = NotificationType.REVISED;
        private NotificationTargetType targetType = NotificationTargetType.REVIEW_REQUEST;
        private Long targetId = 100L;
        private String title = "결제 문서 개정안이 반영되었습니다";
        private String message = "r5 → r6";
        private String dedupeKey = "RR_REVISED:100:6";
        private Long createdBy = 2L;

        public NotificationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public NotificationBuilder recipientId(Long recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public NotificationBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public NotificationBuilder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public NotificationBuilder targetType(NotificationTargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        public NotificationBuilder targetId(Long targetId) {
            this.targetId = targetId;
            return this;
        }

        public NotificationBuilder title(String title) {
            this.title = title;
            return this;
        }

        public NotificationBuilder message(String message) {
            this.message = message;
            return this;
        }

        public NotificationBuilder dedupeKey(String dedupeKey) {
            this.dedupeKey = dedupeKey;
            return this;
        }

        public NotificationBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Notification build() {
            Notification notification = Notification.create(
                    recipientId, workspaceId, type, targetType, targetId, title, message, dedupeKey, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(notification, "id", id);
            }

            return notification;
        }
    }
}
