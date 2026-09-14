package com.ubidict.backend.notification.implement;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.infra.NotificationRepository;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationUpdater {

    private final NotificationRepository notificationRepository;

    public void markRead(Notification notification) {
        notification.markRead();
    }

    /**
     * 안 읽은 알림을 한 문장으로 바꾼다. 행을 하나씩 불러 {@code markRead()}를 부르면 안 읽은 수만큼 UPDATE가 나간다.
     *
     * @return 실제로 읽음으로 바뀐 개수
     */
    public int markAllRead(Long memberId, Long workspaceId) {
        return notificationRepository.markAllRead(
                memberId, workspaceId, OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));
    }
}
